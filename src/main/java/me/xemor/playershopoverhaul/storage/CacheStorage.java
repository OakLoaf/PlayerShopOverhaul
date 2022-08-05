package me.xemor.playershopoverhaul.storage;

import com.mysql.cj.util.LRUCache;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.PricedMarket;
import me.xemor.playershopoverhaul.Market;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CacheStorage implements Storage {

    private final Storage storage;
    private long getMarketsLastChecked = Long.MIN_VALUE;
    private final LRUCache<MarketArgs, CompletableFuture<List<PricedMarket>>> marketsCache = new LRUCache<>(25);
    private final LRUCache<UUID, CompletableFuture<String>> usernames = new LRUCache<>(3);
    private final LRUCache<String, CompletableFuture<UUID>> uuids = new LRUCache<>(10);


    public CacheStorage(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void setup() {
        storage.setup();
    }

    @Override
    public void registerListing(UUID uuid, ItemStack item, int stock, double pricePer) {
        storage.registerListing(uuid, item, stock, pricePer);
    }

    @Override
    public CompletableFuture<Object> removeListing(int listingID) {
        return storage.removeListing(listingID);
    }

    @Override
    public CompletableFuture<Listing> getListing(int listingID) {
        return storage.getListing(listingID);
    }

    @Override
    public CompletableFuture<Market> getMarket(int marketID) {
        return storage.getMarket(marketID);
    }

    @Override
    public CompletableFuture<PricedMarket> getPricedMarket(int marketID) {
        return storage.getPricedMarket(marketID);
    }

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset) {
        return getMarkets(offset, "");
    }

    @Override
    public CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int serverID, int offset) {
        return storage.getPlayerListings(uuid, serverID, offset);
    }

    @Override
    public CompletableFuture<List<Market>> getMarkets(List<Listing> listings) {
        return storage.getMarkets(listings);
    }

    @Override
    public CompletableFuture<EconomyResponse> purchaseFromMarket(UUID uuid, Market market, int amount) {
        return storage.purchaseFromMarket(uuid, market, amount);
    }

    @Override
    public CompletableFuture<List<PricedMarket>> getMarkets(int offset, String search) {
        long currentTime = System.currentTimeMillis();
        if (getMarketsLastChecked + 30000 < currentTime) {
            getMarketsLastChecked = System.currentTimeMillis();
            List<MarketArgs> outdated = new ArrayList<>();
            for (MarketArgs key : marketsCache.keySet()) {
                if (key.timestamp() + 60000 < currentTime) {
                    outdated.add(key);
                }
            }
            for (MarketArgs outdatedArgs : outdated) {
                marketsCache.remove(outdatedArgs);
            }
        }
        MarketArgs args = new MarketArgs(offset, search, currentTime);
        return marketsCache.computeIfAbsent(args, (marketArgs) -> storage.getMarkets(marketArgs.offset(), marketArgs.search()));
    }

    @Override
    public CompletableFuture<Double> claimPayment(UUID uuid) {
        return storage.claimPayment(uuid);
    }

    @Override
    public void setUsername(UUID uuid, String name) {
        storage.setUsername(uuid, name);
    }

    @Override
    public CompletableFuture<String> getUsername(UUID uuid) {
        return usernames.computeIfAbsent(uuid, storage::getUsername);
    }

    @Override
    public CompletableFuture<UUID> getUUID(String username) {
        return uuids.computeIfAbsent(username, storage::getUUID);
    }

    private record MarketArgs(int offset, String search, long timestamp) {
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MarketArgs other) {
                return this.offset == other.offset && this.search.equals(other.search);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + Integer.hashCode(offset);
            return hash * 31 + search.hashCode();
        }
    }

}
