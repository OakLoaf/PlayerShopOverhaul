package me.xemor.playershopoverhaul.storage;

import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.PricedMarket;
import me.xemor.playershopoverhaul.Market;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    void registerListing(UUID uuid, ItemStack item, int stock, double pricePer) throws ItemTooLargeException;
    CompletableFuture<Object> removeListing(int listingID);
    CompletableFuture<Listing> getListing(int listingID);
    CompletableFuture<Market> getMarket(int marketID);
    CompletableFuture<PricedMarket> getPricedMarket(int marketID);
    CompletableFuture<List<PricedMarket>> getMarkets(int offset);
    CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int serverID, int offset);
    CompletableFuture<List<Market>> getMarkets(List<Listing> listings);
    CompletableFuture<Double> costToPurchaseAmountFromMarket(int marketID, int amount);
    CompletableFuture<EconomyResponse> purchaseFromMarket(UUID uuid, int marketID, int amount, double maxPrice);
    CompletableFuture<List<PricedMarket>> getMarkets(int offset, String search);
    CompletableFuture<Double> claimPayment(UUID uuid);
    void setUsername(UUID uuid, String name);
    CompletableFuture<String> getUsername(UUID uuid);
    CompletableFuture<UUID> getUUID(String username);


}
