package me.xemor.playershopoverhaul.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.xemor.playershopoverhaul.*;
import me.xemor.playershopoverhaul.itemserialization.ItemSerialization;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SQLStorage implements Storage {

    private final ConfigHandler configHandler;
    DatabaseSource source;
    ExecutorService threads;

    public SQLStorage(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    private void initMySQLDataSource(String dbName, String host, int port, String user, String password) {
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", "com.mysql.cj.jdbc.MysqlDataSource");
        props.setProperty("dataSource.serverName", host);
        props.setProperty("dataSource.portNumber", String.valueOf(port));
        props.setProperty("dataSource.user", user);
        props.setProperty("dataSource.password", password);
        props.setProperty("dataSource.databaseName", dbName);
        HikariConfig hikariConfig = new HikariConfig(props);
        hikariConfig.setMaximumPoolSize(8);
        source = new DatabaseSource(new HikariDataSource(hikariConfig));
        testDataSource(source);
    }

    void testDataSource(DatabaseSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void setupTable(String fileName) {
        String setup;
        try (InputStream in = SQLStorage.class.getClassLoader().getResourceAsStream(fileName)) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining(""));
        } catch (IOException e) {
            PlayerShopOverhaul.getInstance().getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            e.printStackTrace();
            return;
        }
        String[] split = setup.split("\\|");
        for (String str : split) {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(str)) {
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        PlayerShopOverhaul.getInstance().getLogger().info("Database setup complete.");
    }

    @Override
    public void setup() {
        String name = configHandler.getDatabaseName();
        String host = configHandler.getDatabaseHost();
        int port = configHandler.getDatabasePort();
        String user = configHandler.getDatabaseUsername();
        String password = configHandler.getDatabasePassword();
        initMySQLDataSource(name, host, port, user, password);
        // Creates a pool that always has one thread ready,
        // can have up to 4 threads,
        // and times out extra threads after 60 seconds of inactivity
        threads = new ThreadPoolExecutor(1, 8,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        setupTable("mysqlsetup.sql");
    }

    @Override
    public void registerListing(UUID uuid, ItemStack item, int stock, double pricePer) {
        ItemStack representativeItem = item.clone();
        representativeItem.setAmount(1);
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO listings(sellerID, serverID, stock, pricePer, marketID) VALUES(?, ?, ?, ?, ?);"
            )) {
                int id = getMarketID(conn, representativeItem);
                if (id == -1) { createMarket(conn, representativeItem); id = getMarketID(conn, representativeItem); }
                PlayerShopOverhaul.getInstance().getLogger().fine(String.valueOf(id));
                byte[] uuidBytes = getUUIDBinary(uuid);
                stmt.setBytes(1, uuidBytes);
                stmt.setInt(2, PlayerShopOverhaul.getInstance().getConfigHandler().getServerID());
                stmt.setInt(3, stock);
                stmt.setDouble(4, pricePer);
                stmt.setInt(5, id);
                stmt.execute();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Object> removeListing(int listingID) {
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM listings WHERE id = ?"
            )) {
                stmt.setInt(1, listingID);
                stmt.execute();
                completableFuture.complete(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Listing> getListing(int listingID) {
        CompletableFuture<Listing> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM listings WHERE id = ?"
            )) {
                stmt.setInt(1, listingID);
                ResultSet resultSet = stmt.executeQuery();
                resultSet.next();
                UUID uuid = fromUUIDBinary(resultSet.getBytes("sellerID"));
                int marketID = resultSet.getInt("marketID");
                int stock = resultSet.getInt("stock");
                double pricePer = resultSet.getDouble("pricePer");
                completableFuture.complete(new Listing(listingID, uuid, marketID, stock, pricePer));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Market> getMarket(int marketID) {
        CompletableFuture<Market> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            Market market = getMarketBlocking(marketID);
            if (market == null) {
                completableFuture.completeExceptionally(new NullPointerException("Market is null!"));
                return;
            }
            completableFuture.complete(market);
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<PricedMarket> getPricedMarket(int marketID) {
        CompletableFuture<PricedMarket> future = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT markets.item AS item, prices.price AS price, prices.stock AS stock, users.sellerID AS uuid
                    FROM markets
                    JOIN (SELECT min(pricePer) AS price, sum(stock) AS stock, marketID FROM listings GROUP BY marketID) AS prices
                    ON prices.marketID = markets.id
                    JOIN (SELECT sellerID, pricePer, marketID FROM listings) AS users
                    ON users.pricePer = prices.price AND users.marketID = prices.marketID
                    WHERE markets.id = ?
                    """
            )) {
                stmt.setInt(1, marketID);
                ResultSet resultSet = stmt.executeQuery();
                resultSet.next();
                byte[] itemBytes = resultSet.getBytes("item");
                double goingPrice = resultSet.getDouble("price");
                int stock = resultSet.getInt("stock");
                UUID uuid = fromUUIDBinary(resultSet.getBytes("uuid"));
                future.complete(new PricedMarket(marketID, ItemSerialization.binaryToItemStack(itemBytes), goingPrice, uuid, stock));
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private Market getMarketBlocking(int marketID) {
        try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                """
                SELECT markets.item AS item
                FROM markets
                WHERE markets.id = ?
                """
        )) {
            stmt.setInt(1, marketID);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            byte[] itemBytes = resultSet.getBytes("item");
            return new Market(marketID, ItemSerialization.binaryToItemStack(itemBytes));
        } catch (SQLException e) {}
        return null;
    }

    private int getMarketID(Connection conn, ItemStack item) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM markets WHERE item = ?;"
        )) {
            byte[] binaryItem = ItemSerialization.itemStackToBinary(item);
            stmt.setBytes(1, binaryItem);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            return -1;
        }
    }

    private void createMarket(Connection conn, ItemStack item) {
        try (PreparedStatement stmt = conn.prepareStatement(
                """
                INSERT INTO markets(item, name) VALUES(?, ?);
                """
        )) {
            stmt.setBytes(1, ItemSerialization.itemStackToBinary(item));
            stmt.setString(2, Market.getName(item));
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset, String search) {
        CompletableFuture<List<PricedMarket>> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT id, item, prices.price AS price, prices.stock AS stock, prices.sellerID AS uuid
                    FROM markets
                    JOIN (SELECT sellerID, min(pricePer) AS price, sum(stock) AS stock, marketID FROM listings GROUP BY marketID) AS prices
                    ON prices.marketID = markets.id
                    WHERE name LIKE ?
                    ORDER BY stock DESC
                    LIMIT 21 OFFSET ?
                    """
            )) {
                String lowerSearch = search.toLowerCase();
                stmt.setString(1, "%" + lowerSearch + "%");
                stmt.setInt(2, offset);
                ResultSet resultSet = stmt.executeQuery();
                List<PricedMarket> pricedMarkets = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    byte[] itemBytes = resultSet.getBytes("item");
                    double pricePer = resultSet.getDouble("price");
                    int stock = resultSet.getInt("stock");
                    UUID uuid = fromUUIDBinary(resultSet.getBytes("uuid"));
                    ItemStack itemStack = ItemSerialization.binaryToItemStack(itemBytes);
                    pricedMarkets.add(new PricedMarket(id, itemStack, pricePer, uuid, stock));
                }
                completableFuture.complete(pricedMarkets);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Double> claimPayment(UUID uuid) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT id
                    FROM payment
                    WHERE sellerID = ?
                    AND serverID = ?
                    """
            )) {
                stmt.setBytes(1, getUUIDBinary(uuid));
                stmt.setInt(2, PlayerShopOverhaul.getInstance().getConfigHandler().getServerID());
                ResultSet resultSet = stmt.executeQuery();
                List<Integer> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("id"));
                }
                String idsSQLSet = createSQLSet(ids);
                if ("()".equals(idsSQLSet)) { future.complete(0D); return; }
                PreparedStatement sum = conn.prepareStatement(String.format("""
                    SELECT SUM(toPay) AS pay
                    FROM payment
                    WHERE id IN %s
                """, idsSQLSet));
                resultSet = sum.executeQuery();
                resultSet.next();
                double toPay = resultSet.getDouble("pay");
                PreparedStatement delete = conn.prepareStatement(String.format("""
                        DELETE FROM payment
                        WHERE id IN %s
                        """, idsSQLSet));
                delete.execute();
                future.complete(toPay);
            } catch (Exception e) {
                future.completeExceptionally(e);
                e.printStackTrace();
            }
        });
        return future;
    }

    @Override
    public void setUsername(UUID uuid, String name) {
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    REPLACE INTO nameToUUID(sellerID, username) VALUES(?, ?);
                    """
            )) {
                stmt.setBytes(1, getUUIDBinary(uuid));
                stmt.setString(2, name);
                stmt.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<String> getUsername(UUID uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT username FROM nameToUUID WHERE sellerID = ?;
                    """
            )) {
                stmt.setBytes(1, getUUIDBinary(uuid));
                ResultSet resultSet = stmt.executeQuery();
                boolean isFull = resultSet.next();
                if (!isFull) { future.complete(uuid.toString()); return; } //if their username isn't in the database, return their uuid as a string
                future.complete(resultSet.getString("username"));
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT sellerID FROM nameToUUID WHERE username = ?;
                    """
            )) {
                stmt.setString(1, username);
                ResultSet resultSet = stmt.executeQuery();
                resultSet.next();
                future.complete(UUID.fromString(resultSet.getString("sellerID")));
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset) {
        return getMarkets(offset, "");
    }

    @Override
    public CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int serverID, int offset) {
        CompletableFuture<List<Listing>> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT * 
                    FROM listings 
                    WHERE sellerID = ? 
                    AND serverID = ?
                    LIMIT 21 OFFSET ?
                    """
            )) {
                stmt.setBytes(1, getUUIDBinary(uuid));
                stmt.setInt(2, serverID);
                stmt.setInt(3, offset);
                ResultSet resultSet = stmt.executeQuery();
                List<Listing> listings = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int stock = resultSet.getInt("stock");
                    double pricePer = resultSet.getDouble("pricePer");
                    int marketID = resultSet.getInt("marketID");
                    listings.add(new Listing(id, uuid, marketID, stock, pricePer));
                }
                completableFuture.complete(listings);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<List<Market>> getMarkets(List<Listing> listings) {
        CompletableFuture<List<Market>> completableFuture = new CompletableFuture<>();
        if (listings.size() == 0) { completableFuture.complete(Collections.emptyList()); return completableFuture; }
        threads.submit(() -> {
            String rawStatement = """
                SELECT markets.id, item, listings.id
                FROM markets
                JOIN listings ON markets.id = listings.marketID
                WHERE listings.id IN %s;
                """;
            rawStatement = String.format(rawStatement, createSQLSet(listings.stream().map(Listing::getID).collect(Collectors.toList())));
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    rawStatement
            )) {
                ResultSet resultSet = stmt.executeQuery();
                Object[] objects = new Object[listings.size()];
                for (int i = 0; i < objects.length; i++) {
                    objects[i] = listings.get(i).getID();
                }
                while (resultSet.next()) {
                    int marketID = resultSet.getInt(1);
                    byte[] itemBytes = resultSet.getBytes(2);
                    ItemStack item = ItemSerialization.binaryToItemStack(itemBytes);
                    int listingID = resultSet.getInt(3);
                    Market market = new Market(marketID, item);
                    for (int i = 0; i < objects.length; i++) {
                        if (objects[i].equals(listingID)) {
                            objects[i] = market;
                        }
                    }
                }
                List<Market> markets = Arrays.stream(objects).map((object) -> (Market) object).toList();
                completableFuture.complete(markets);
            } catch (Exception e) {
                e.printStackTrace();
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    protected void depositPayment(UUID uuid, int serverID, double toPay) {
        try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                """
                INSERT INTO payment(sellerID, serverID, toPay) VALUES(?, ?, ?);
                """
        )) {
            stmt.setBytes(1, getUUIDBinary(uuid));
            stmt.setInt(2, serverID);
            stmt.setDouble(3, toPay);
            stmt.execute();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<EconomyResponse> purchaseFromMarket(UUID uuid, Market market, int amount) {
        CompletableFuture<EconomyResponse> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        """
                        SELECT listings.id AS lid, sellerID, serverID, stock, pricePer FROM listings
                        JOIN markets ON listings.marketID = markets.id
                        WHERE markets.id = ?
                        ORDER BY listings.pricePer ASC
                        LIMIT ?
                        """
                );
                stmt.setInt(1, market.getID());
                stmt.setInt(2, amount);
                ResultSet resultSet = stmt.executeQuery();
                int stockSum = 0;
                double cost = 0;
                List<Integer> forRemoval = new ArrayList<>();
                List<SQLiteStorage.PurchasedListing> purchasedListings = new ArrayList<>();
                Listing listingForReinsertion = null;
                int serverIDForReinsertion = -1;
                while (resultSet.next()) {
                    if (stockSum > amount) break;
                    int id = resultSet.getInt("lid");
                    UUID sellerID = fromUUIDBinary(resultSet.getBytes("sellerID"));
                    int stock = resultSet.getInt("stock");
                    double pricePer = resultSet.getDouble("pricePer");
                    int serverID = resultSet.getInt("serverID");
                    stockSum += stock;
                    double price = stock * pricePer;
                    forRemoval.add(id);
                    if (stockSum > amount) {
                        price -= (stockSum - amount) * pricePer;
                        listingForReinsertion = new Listing(id, sellerID, market.getID(), stockSum - amount, pricePer);
                        serverIDForReinsertion = serverID;
                    }
                    purchasedListings.add(new SQLiteStorage.PurchasedListing(sellerID, serverID, price));
                    cost += price;
                }
                if (stockSum < amount) {
                    completableFuture.completeExceptionally(new IllegalStateException("There were not enough items on the market!"));
                    return;
                }
                Economy economy = PlayerShopOverhaul.getInstance().getEconomy();
                EconomyResponse transaction = economy.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), cost);
                if (!transaction.transactionSuccess()) {
                    completableFuture.complete(transaction);
                    return;
                }
                for (SQLiteStorage.PurchasedListing purchasedListing : purchasedListings) { //pay the people who put the listings up
                    depositPayment(purchasedListing.sellerID(), purchasedListing.serverID(), purchasedListing.toPay());
                }
                StringJoiner joiner = new StringJoiner(",");
                for (Integer integer : forRemoval) {
                    joiner.add(String.valueOf(integer));
                }
                String rawStmt = String.format("DELETE FROM listings WHERE id IN (%s);", joiner);
                stmt = conn.prepareStatement(
                        rawStmt
                );
                stmt.execute();
                if (listingForReinsertion != null) {
                    PreparedStatement insert = conn.prepareStatement("INSERT INTO listings (sellerID, serverID, stock, pricePer, marketID) VALUES(?, ?, ?, ?, ?);");
                    insert.setBytes(1, getUUIDBinary(listingForReinsertion.getSellerID()));
                    insert.setInt(2, serverIDForReinsertion);
                    insert.setInt(3, listingForReinsertion.getStock());
                    insert.setDouble(4, listingForReinsertion.getPricePer());
                    insert.setInt(5, market.getID());
                    insert.execute();
                }
                completableFuture.complete(transaction);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    public String createSQLSet(List<Integer> ints) {
        StringJoiner joiner = new StringJoiner(",");
        for (Integer integer : ints) {
            joiner.add(String.valueOf(integer));
        }
        return "(" + joiner.toString() + ")";
    }

    byte[] getUUIDBinary(UUID uuid) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeLong(uuid.getLeastSignificantBits());
        dataOutputStream.writeLong(uuid.getMostSignificantBits());
        byte[] uuidBytes = byteArrayOutputStream.toByteArray();
        dataOutputStream.close();
        return uuidBytes;
    }

    UUID fromUUIDBinary(byte[] uuidBinary) {
        ByteBuffer bb = ByteBuffer.wrap(uuidBinary);
        long low = bb.getLong();
        long high = bb.getLong();
        return new UUID(high, low);
    }


    public final class DatabaseSource {

        private Connection connection = null;
        private HikariDataSource source = null;

        public DatabaseSource(HikariDataSource source) { //MySQL
            this.source = source;
        }

        public DatabaseSource() {} //SQLite

        public Connection getConnection() throws SQLException {
            if (source != null) return source.getConnection();
            else return DriverManager.getConnection("jdbc:sqlite:" + new File(PlayerShopOverhaul.getInstance().getDataFolder(), "gts.db").getAbsolutePath());
        }
    }

    protected static record PurchasedListing(UUID sellerID, int serverID, double toPay) {}
}
