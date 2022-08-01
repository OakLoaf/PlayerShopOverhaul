package me.xemor.playershopoverhaul.storage;

import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class SQLiteStorage extends SQLStorage {
    public SQLiteStorage(ConfigHandler configHandler) {
        super(configHandler);
    }

    @Override
    public void setup() {
        initSQLiteDataSource();
        threads = Executors.newFixedThreadPool(1);
        setupTable("sqlitesetup.sql");
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
                List<PurchasedListing> purchasedListings = new ArrayList<>();
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
                    purchasedListings.add(new PurchasedListing(sellerID, serverID, price));
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
                for (PurchasedListing purchasedListing : purchasedListings) { //pay the people who put the listings up
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

    private void initSQLiteDataSource() {
        source = new DatabaseSource();
        testDataSource(source);
    }


}
