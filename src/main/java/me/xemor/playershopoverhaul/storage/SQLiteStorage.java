package me.xemor.playershopoverhaul.storage;

import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SQLiteStorage extends SQLStorage {
    public SQLiteStorage(ConfigHandler configHandler) {
        super(configHandler);
    }

    @Override
    public CompletableFuture<EconomyResponse> purchaseFromMarket(UUID uuid, Market market, int amount) {
        CompletableFuture<EconomyResponse> completableFuture = new CompletableFuture<>();
        threads.submit(() -> {
            try (Connection conn = source.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        """
                        SELECT listings.id AS lid, sellerID, stock, pricePer FROM listings
                        JOIN markets ON listings.id = markets.id
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

}
