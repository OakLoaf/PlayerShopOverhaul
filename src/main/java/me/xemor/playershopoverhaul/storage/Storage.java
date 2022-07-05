package me.xemor.playershopoverhaul.storage;

import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage {

    void registerListing(UUID uuid, ItemStack item, int stock, double pricePer);
    CompletableFuture<Object> removeListing(int listingID);
    CompletableFuture<Listing> getListing(int listingID);
    CompletableFuture<Market> getMarket(int marketID);
    CompletableFuture<List<Market>> getMarkets(int offset, int limit);
    CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int offset, int limit);
    CompletableFuture<List<Market>> getMarkets(List<Listing> listings);
    CompletableFuture<EconomyResponse> purchaseFromMarket(UUID uuid, Market market, int amount);
    CompletableFuture<List<Market>> getMarkets(int offset, int limits, String search);


}
