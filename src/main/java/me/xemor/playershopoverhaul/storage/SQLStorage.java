package me.xemor.playershopoverhaul.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.xemor.playershopoverhaul.*;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.itemserialization.ItemSerialization;
import me.xemor.playershopoverhaul.storage.statements.*;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDArgumentFactory;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDColumnMapper;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SQLStorage implements Storage {

    private final ConfigHandler configHandler;
    private final ExecutorService threads;
    private Jdbi jdbi;

    public SQLStorage(ConfigHandler configHandler) {
        this.configHandler = configHandler;
        threads = new ThreadPoolExecutor(1, 8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = defaultFactory.newThread(r);
                        thread.setUncaughtExceptionHandler((t, e) ->
                                PlayerShopOverhaul.getInstance().getLogger().log(Level.SEVERE, "Uncaught exception in thread " + t.getName(), e)
                        );
                        return thread;
                    }
                });
        initialiseDataSource();
    }

    private void initialiseDataSource() {
        boolean isMySQL = configHandler.getDatabaseType().equalsIgnoreCase("mysql");
        Properties props;
        if (isMySQL) props = getMySQLProperties();
        else props = getH2Properties();
        HikariConfig hikariConfig = new HikariConfig(props);
        hikariConfig.setMaximumPoolSize(4);
        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        jdbi = Jdbi.create(hikariDataSource)
                .registerArgument(new UUIDArgumentFactory())
                .registerColumnMapper(UUID.class, new UUIDColumnMapper())
                .installPlugin(new SqlObjectPlugin());
        setupTable("setup.sql");
    }

    private Properties getMySQLProperties() {
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", "com.mysql.cj.jdbc.MysqlDataSource");
        props.setProperty("dataSource.serverName", configHandler.getDatabaseHost());
        props.setProperty("dataSource.portNumber", String.valueOf(configHandler.getDatabasePort()));
        props.setProperty("dataSource.user", configHandler.getDatabaseUsername());
        props.setProperty("dataSource.password", configHandler.getDatabasePassword());
        props.setProperty("dataSource.databaseName", configHandler.getDatabaseName());
        return props;
    }

    private Properties getH2Properties() {
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", "org.h2.jdbcx.JdbcDataSource");
        props.setProperty("dataSource.url", "jdbc:h2:file:%s;MODE=MySQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE".formatted(
                PlayerShopOverhaul.getInstance().getDataFolder().getAbsolutePath() + "/database.db"
        ));
        props.setProperty("dataSource.user", "sa"); // default H2 user
        props.setProperty("dataSource.password", ""); // default H2 password
        return props;
    }

    private void setupTable(String fileName) {
        String setup;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining(""));
        } catch (IOException e) {
            PlayerShopOverhaul.getInstance().getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            e.printStackTrace();
            return;
        }
        String[] split = setup.split("\\|");
        for (String str : split) {
            try (Handle handle = jdbi.open()) {
                handle.execute(str);
            }
        }
        PlayerShopOverhaul.getInstance().getLogger().info("Database setup complete.");
    }

    @Override
    public void registerListing(UUID uuid, ItemStack item, int stock, double pricePer) throws ItemTooLargeException {
        ItemStack itemAmount1 = item.clone();
        itemAmount1.setAmount(1);
        byte[] serializedItem = ItemSerialization.itemStackToBinary(itemAmount1);
        if (serializedItem.length > 65500) {
            throw new ItemTooLargeException("The item was too large to be stored in the database!");
        }
        submitWithExceptionLogging(() -> {
            int serverID = configHandler.getServerID();
            jdbi.useTransaction((handle) -> {
                handle.attach(MarketStatements.class).insertOrReplaceMarket(serializedItem, Market.getName(item));
                handle.attach(ListingStatements.class).insertListing(uuid, serverID, stock, pricePer, serializedItem);
            });
        });
    }

    @Override
    public CompletableFuture<Object> removeListing(int listingID) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            jdbi.useTransaction((handle) -> {
                handle.attach(ListingStatements.class).deleteListing(listingID);
            });
            future.complete(true);
        });
        return future;
    }

    @Override
    public CompletableFuture<Listing> getListing(int listingID) {
        CompletableFuture<Listing> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            Listing listing = jdbi.inTransaction((handle) -> {
                return handle.attach(ListingStatements.class).getListing(listingID);
            });
            future.complete(listing);
        });
        return future;
    }

    @Override
    public CompletableFuture<Market> getMarket(int marketID) {
        CompletableFuture<Market> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            MarketDTO marketDTO = jdbi.inTransaction((handle) -> {
                return handle.attach(MarketStatements.class).getMarketById(marketID);
            });
            future.complete(marketDTO.asMarket());
        });
        return future;
    }

    @Override
    public CompletableFuture<PricedMarket> getPricedMarket(int marketID) {
        CompletableFuture<PricedMarket> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            PricedMarketStatements.PricedMarketDTO pricedMarketDTO = jdbi.inTransaction((handle) -> {
                return handle.attach(PricedMarketStatements.class).getPricedMarketById(marketID);
            });
            future.complete(pricedMarketDTO.asPricedMarket());
        });
        return future;
    }

    @Override
    public CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int serverID, int offset) {
        CompletableFuture<List<Listing>> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            List<Listing> listings = jdbi.inTransaction((handle) -> {
                return handle.attach(ListingStatements.class).getPlayerListings(uuid, serverID, offset);
            });
            future.complete(listings);
        });
        return future;
    }

    @Override
    public CompletableFuture<List<Market>> getMarkets(List<Listing> listings) {
        CompletableFuture<List<Market>> future = new CompletableFuture<>();
        if (listings.isEmpty()) future.complete(List.of());
        else {
            submitWithExceptionLogging(() -> {
                List<MarketStatements.ListingIdToMarketDTO> listingIdToMarketDTOS = jdbi.inTransaction((handle) -> {
                    return handle.attach(MarketStatements.class).getMarketsForListings(listings.stream().map(Listing::getID).toList());
                });
                List<Market> markets = listings.stream().flatMap(
                        (it1) -> listingIdToMarketDTOS
                                .stream()
                                .filter((it2) -> it1.getID() == it2.listingID()).findFirst().stream()
                ).map(
                        (it) -> new Market(it.marketID(), ItemSerialization.binaryToItemStack(it.item()))
                ).toList();
                future.complete(markets);
            });
        }
        return future;
    }

    @Override
    public CompletableFuture<Double> costToPurchaseAmountFromMarket(int marketID, int amount) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            jdbi.useTransaction((handle) -> {
                ListingStatements listingStatements = handle.attach(ListingStatements.class);
                List<Listing> listings = listingStatements.listingsToPurchaseForStockAmount(marketID, amount);
                int stockSum = 0;
                double cost = 0;
                for (Listing listing : listings) {
                    if (stockSum > amount) break;
                    stockSum += listing.getStock();
                    double price = listing.getTotalPrice();
                    if (stockSum > amount) {
                        int surplusStock = stockSum - amount;
                        price -= surplusStock * listing.getPricePer();
                    }
                    cost += price;
                }
                future.complete(cost);
            });
        });
        return future;
    }

    @Override
    public CompletableFuture<EconomyResponse> purchaseFromMarket(UUID buyerUUID, int marketID, int quantityToPurchase, double maxPrice) {
        CompletableFuture<EconomyResponse> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            jdbi.useTransaction((handle) -> {
                ListingStatements listingStatements = handle.attach(ListingStatements.class);
                List<Listing> listings = listingStatements.listingsToPurchaseForStockAmount(marketID, quantityToPurchase);
                int stockSum = 0;
                double cost = 0;
                List<PurchasedListing> purchasedListings = new ArrayList<>();
                List<Integer> oldListings = new ArrayList<>();
                Listing listingForReinsertion = null;
                for (Listing listing : listings) {
                    stockSum += listing.getStock();

                    if (stockSum >= quantityToPurchase) {
                        int surplusStock = stockSum - quantityToPurchase;
                        double price = listing.getTotalPrice() - surplusStock * listing.getPricePer();

                        if (surplusStock > 0) {
                            listingForReinsertion = new Listing(
                                    listing.getID(),
                                    listing.getServerId(),
                                    listing.getSellerID(),
                                    marketID,
                                    surplusStock,
                                    listing.getPricePer()
                            );
                        }

                        oldListings.add(listing.getID());
                        purchasedListings.add(new PurchasedListing(listing.getSellerID(), listing.getServerId(), price));
                        cost += price;
                        break;
                    }

                    // still below the target, take whole listing
                    oldListings.add(listing.getID());
                    purchasedListings.add(new PurchasedListing(listing.getSellerID(), listing.getServerId(), listing.getTotalPrice()));
                    cost += listing.getTotalPrice();
                }
                if (stockSum < quantityToPurchase) {
                    future.completeExceptionally(new InsufficientStockException("There were not enough items on the market!"));
                    return;
                }
                if (cost > maxPrice) {
                    future.completeExceptionally(new ExceedsMaxPriceException("The price of the items would have been %s, greater than %s!".formatted(cost, maxPrice)));
                    return;
                }
                Economy economy = PlayerShopOverhaul.getInstance().getEconomy();
                // Use offline player here as 99% of the time, the player will still be online
                // in the case, they are offline. We are in another thread, and hopefully it won't block main thread.
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(buyerUUID);
                EconomyResponse transaction = economy.withdrawPlayer(offlinePlayer, cost);
                if (!transaction.transactionSuccess()) {
                    future.complete(transaction);
                    return;
                }

                PaymentStatements paymentStatements = handle.attach(PaymentStatements.class);
                for (PurchasedListing purchasedListing : purchasedListings) {
                    paymentStatements.insertPayment(purchasedListing.sellerID(), purchasedListing.serverID(), purchasedListing.toPay());
                    LogStatements logStatements = handle.attach(LogStatements.class);
                    logStatements.logPayment(buyerUUID, purchasedListing.sellerID(), quantityToPurchase, purchasedListing.toPay(), marketID);
                }

                int[] listingsToDelete = oldListings.stream().mapToInt(Integer::intValue).toArray();
                if (listingsToDelete.length > 0) listingStatements.deleteListings(listingsToDelete);

                if (listingForReinsertion != null) {
                    MarketStatements marketStatements = handle.attach(MarketStatements.class);
                    MarketDTO market = marketStatements.getMarketById(marketID);
                    listingStatements.insertListing(
                            listingForReinsertion.getSellerID(),
                            listingForReinsertion.getServerId(),
                            listingForReinsertion.getStock(),
                            listingForReinsertion.getPricePer(),
                            market.item()
                    );
                }
                future.complete(transaction);
            });
        });
        return future;
    }

    private record PurchasedListing(UUID sellerID, int serverID, double toPay) {}

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset) {
        return getMarkets(offset, "");
    }

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset, String search) {
        CompletableFuture<List<PricedMarket>> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            List<PricedMarketStatements.PricedMarketDTO> pricedMarketDTOs = jdbi.inTransaction((handle) -> {
                return handle.attach(PricedMarketStatements.class).getPricedMarkets(offset, '%' + search.toUpperCase() + '%');
            });
            future.complete(pricedMarketDTOs.stream().map(PricedMarketStatements.PricedMarketDTO::asPricedMarket).toList());
        });
        return future;
    }

    @Override
    public CompletableFuture<Double> claimPayment(UUID uuid) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
           jdbi.useTransaction((handle) -> {
               PaymentStatements paymentStatements = handle.attach(PaymentStatements.class);
               List<PaymentStatements.PaymentDTO> payments = paymentStatements.getPayments(uuid, configHandler.getServerID());
               payments.stream().map(PaymentStatements.PaymentDTO::toPay).reduce(Double::sum).ifPresentOrElse((toPaySum) -> {
                   OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                   EconomyResponse economyResponse = PlayerShopOverhaul.getInstance().getEconomy().depositPlayer(player, toPaySum);
                   if (economyResponse.transactionSuccess()) {
                       paymentStatements.deletePayments(payments.stream().mapToInt(PaymentStatements.PaymentDTO::id).toArray());
                   }
                   future.complete(toPaySum);
               }, () -> future.complete(0.0));
           });
        });
        return future;
    }

    @Override
    public void setUsername(UUID uuid, String name) {
        submitWithExceptionLogging(() -> {
            jdbi.useTransaction((handle) -> {
                NameToUUIDStatements nameToUUIDStatements = handle.attach(NameToUUIDStatements.class);
                nameToUUIDStatements.setUsername(uuid, name);
            });
        });
    }

    @Override
    public CompletableFuture<String> getUsername(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            String username = jdbi.inTransaction((handle) -> {
                NameToUUIDStatements nameToUUIDStatements = handle.attach(NameToUUIDStatements.class);
                return nameToUUIDStatements.getUsername(uuid);
            });
            future.complete(username);
        });
        return future;
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        submitWithExceptionLogging(() -> {
            UUID uuid = jdbi.inTransaction((handle) -> {
                NameToUUIDStatements nameToUUIDStatements = handle.attach(NameToUUIDStatements.class);
                return nameToUUIDStatements.getUUID(username);
            });
            future.complete(uuid);
        });
        return future;
    }

    private void submitWithExceptionLogging(Runnable runnable) {
        threads.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                PlayerShopOverhaul.getInstance().getLogger().log(Level.SEVERE, "Exception in async task", t);
                throw t;
            }
        });
    }
}
