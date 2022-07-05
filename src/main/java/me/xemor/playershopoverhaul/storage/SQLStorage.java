package me.xemor.playershopoverhaul.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.itemserialization.ItemSerialization;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.error.Mark;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SQLStorage implements Storage {

    private final ConfigHandler configHandler;
    DatabaseSource source;
    ExecutorService threads;
    private final ReentrantLock lock = new ReentrantLock();

    public SQLStorage(ConfigHandler configHandler) {
        this.configHandler = configHandler;
        String type = configHandler.getDatabaseType();
        String name = configHandler.getDatabaseName();
        String host = configHandler.getDatabaseHost();
        int port = configHandler.getDatabasePort();
        String user = configHandler.getDatabaseUsername();
        String password = configHandler.getDatabasePassword();
        if (type.equalsIgnoreCase("MySQL")) {
            initMySQLDataSource(name, host, port, user, password);
            // Creates a pool that always has one thread ready,
            // can have up to 4 threads,
            // and times out extra threads after 60 seconds of inactivity
            threads = new ThreadPoolExecutor(1, 4,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>());
        } else if (type.equalsIgnoreCase("SQLite")) {
            initSQLiteDataSource(name, host, port, user, password);
            threads = Executors.newFixedThreadPool(1);
        }
        setupTable();
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
        hikariConfig.setMaximumPoolSize(4);
        source = new DatabaseSource(new HikariDataSource(hikariConfig));
        testDataSource(source);
    }

    private void initSQLiteDataSource(String dbName, String host, int port, String user, String password) {
        source = new DatabaseSource();
        testDataSource(source);
    }

    private void testDataSource(DatabaseSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupTable() {
        String setup;
        try (InputStream in = SQLStorage.class.getClassLoader().getResourceAsStream("dbsetup.sql")) {
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
    public void registerListing(UUID uuid, ItemStack item, int stock, double pricePer) {
        ItemStack representativeItem = item.clone();
        representativeItem.setAmount(1);
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO listings(sellerID, stock, pricePer, marketID) VALUES(?, ?, ?, ?);"
            )) {
                int id = getMarketID(conn, representativeItem);
                if (id == -1) { createMarket(conn, representativeItem); id = getMarketID(conn, representativeItem); }
                byte[] uuidBytes = getUUIDBinary(uuid);
                stmt.setBytes(1, uuidBytes);
                stmt.setInt(2, stock);
                stmt.setDouble(3, pricePer);
                stmt.setInt(4, id);
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
            completableFuture.complete(getMarketBlocking(marketID));
        });
        return completableFuture;
    }

    private Market getMarketBlocking(int marketID) {
        try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM markets WHERE id = ?"
        )) {
            stmt.setInt(1, marketID);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            byte[] itemBytes = resultSet.getBytes("item");
            return new Market(marketID, ItemSerialization.binaryToItemStack(itemBytes));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getMarketID(Connection conn, ItemStack item) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM markets WHERE item = ?;"
        )) {
            byte[] binaryItem = ItemSerialization.itemStackToBinary(item);
            stmt.setBytes(1, binaryItem);
            return stmt.executeQuery().getInt(1);
        } catch (SQLException e) {
            return -1;
        }
    }

    private void createMarket(Connection conn, ItemStack item) {
        try (PreparedStatement stmt = conn.prepareStatement(
                """
                INSERT INTO markets(item) VALUES(?);
                """
        )) {
            stmt.setBytes(1, ItemSerialization.itemStackToBinary(item));
            stmt.execute(); //should be the id of the market that was just created
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<List<Market>> getMarkets(int offset, int limit, String search) {
        CompletableFuture<List<Market>> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    """
                    SELECT id, item, prices.price AS price
                    FROM markets
                    JOIN (SELECT min(pricePer) AS price, sum(stock) AS stock, marketID FROM listings GROUP BY marketID) AS prices
                    ON prices.marketID = markets.id
                    ORDER BY prices.stock DESC
                    LIMIT ? OFFSET ?
                    """
            )) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                ResultSet resultSet = stmt.executeQuery();
                List<Market> markets = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    byte[] itemBytes = resultSet.getBytes("item");
                    double pricePer = resultSet.getDouble("price");
                    ItemStack itemStack = ItemSerialization.binaryToItemStack(itemBytes);
                    markets.add(new Market(id, itemStack, pricePer));
                }
                completableFuture.complete(markets);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    @Override
    public CompletableFuture<List<Market>> getMarkets(int offset, int limit) {
        return getMarkets(offset, limit, "");
    }

    @Override
    public CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int offset, int limit) {
        CompletableFuture<List<Listing>> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM listings WHERE sellerID = ?"
            )) {
                stmt.setBytes(1, getUUIDBinary(uuid));
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
        threads.submit(() -> {
            String rawStatement = """
                SELECT markets.id, item, listings.id
                FROM markets
                JOIN listings ON markets.id = listings.marketID
                WHERE listings.id IN (%s)
                """;
            StringJoiner joiner = new StringJoiner(",");
            for (Listing listing : listings) {
                joiner.add(String.valueOf(listing.getID()));
            }
            rawStatement = String.format(rawStatement, joiner);
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
    }

    private void depositPayment(UUID uuid, double toPay) {
        try (Connection conn = source.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                """
                INSERT INTO payment(sellerID, serverID, toPay) VALUES(?, ?, ?);
                """
        )) {
            stmt.setBytes(1, getUUIDBinary(uuid));
            stmt.setInt(2, PlayerShopOverhaul.getInstance().getConfigHandler().getServerID());
            stmt.setDouble(3, toPay);
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
                        START TRANSACTION;
                        SELECT listings.id AS lid, sellerID, stock, pricePer FROM listings
                        JOIN markets ON listings.id = markets.id
                        WHERE markets.id = ?
                        ORDER BY listings.pricePer ASC
                        LIMIT ?
                        FOR UPDATE;
                        """
                );
                stmt.setInt(1, market.getID());
                stmt.setInt(2, amount);
                ResultSet resultSet = stmt.executeQuery();
                int stockSum = 0;
                double cost = 0;
                List<Integer> forRemoval = new ArrayList<>();
                HashMap<UUID, Double> sellerToPayment = new HashMap<>();
                Listing listingForReinsertion = null;
                while (resultSet.next()) {
                    if (stockSum > amount) break;
                    int id = resultSet.getInt("lid");
                    UUID sellerID = fromUUIDBinary(resultSet.getBytes("sellerID"));
                    int stock = resultSet.getInt("stock");
                    double pricePer = resultSet.getDouble("pricePer");
                    stockSum += stock;
                    double price = stock * pricePer;
                    forRemoval.add(id);
                    if (stockSum > amount) {
                        price -= (stockSum - amount) * pricePer;
                        listingForReinsertion = new Listing(id, sellerID, market.getID(), stockSum - amount, pricePer);
                    }
                    sellerToPayment.put(sellerID, price);
                    cost += price;
                }
                if (stockSum < amount) {
                    conn.prepareStatement("COMMIT;").execute();
                    completableFuture.completeExceptionally(new IllegalStateException("There were not enough items on the market!"));
                    return;
                }
                Economy economy = PlayerShopOverhaul.getInstance().getEconomy();
                EconomyResponse transaction = economy.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), cost);
                if (!transaction.transactionSuccess()) {
                    conn.prepareStatement("COMMIT;").execute();
                    completableFuture.complete(transaction);
                    return;
                }
                for (Map.Entry<UUID, Double> entry : sellerToPayment.entrySet()) { //pay the people who put the listings up
                    economy.depositPlayer(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue());
                }
                StringJoiner joiner = new StringJoiner(",");
                for (Integer integer : forRemoval) {
                    joiner.add(String.valueOf(integer));
                }
                String rawStmt = String.format("""
                        DELETE FROM listings WHERE id IN (%s);
                        %s
                        COMMIT;
                        """, joiner,
                        listingForReinsertion != null ? "INSERT INTO listings (uuid, stock, pricePer, marketID) VALUES(?, ?, ?, ?);" : "");
                stmt = conn.prepareStatement(
                        rawStmt
                );
                if (listingForReinsertion != null) {
                    stmt.setBytes(1, getUUIDBinary(listingForReinsertion.getUUID()));
                    stmt.setInt(2, listingForReinsertion.getStock());
                    stmt.setDouble(3, listingForReinsertion.getPricePer());
                    stmt.setInt(4, market.getID());
                }
                stmt.execute();
                completableFuture.complete(transaction);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        });
        return completableFuture;
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
        long high = bb.getLong();
        long low = bb.getLong();
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
}
