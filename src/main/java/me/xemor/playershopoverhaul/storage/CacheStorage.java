package me.xemor.playershopoverhaul.storage;

import com.mysql.cj.util.LRUCache;
import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CacheStorage implements Storage {

    private final Storage storage;
    private long lastCheckedAge = Long.MIN_VALUE;
    private final LRUCache<MarketArgs, CompletableFuture<List<Market>>> lruCache = new LRUCache<>(25);

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
    public CompletableFuture<List<Market>> getMarkets(int offset, int limit) {
        return getMarkets(offset, limit, "");
    }

    @Override
    public CompletableFuture<List<Listing>> getPlayerListings(UUID uuid, int offset, int limit) {
        return storage.getPlayerListings(uuid, offset, limit);
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
    public CompletableFuture<List<Market>> getMarkets(int offset, int limit, String search) {
        long currentTime = System.currentTimeMillis();
        if (lastCheckedAge + 30000 < currentTime) {
            lastCheckedAge = System.currentTimeMillis();
            List<MarketArgs> outdated = new ArrayList<>();
            for (MarketArgs key : lruCache.keySet()) {
                if (key.timestamp() + 60000 < currentTime) {
                    outdated.add(key);
                }
            }
            for (MarketArgs outdatedArgs : outdated) {
                lruCache.remove(outdatedArgs);
            }
        }
        MarketArgs args = new MarketArgs(offset, limit, search, currentTime);
        return lruCache.computeIfAbsent(args, (marketArgs) -> storage.getMarkets(marketArgs.offset(), marketArgs.limit(), marketArgs.search()));
    }

    @Override
    public CompletableFuture<Double> claimPayment(UUID uuid) {
        return storage.claimPayment(uuid);
    }

    private static record MarketArgs(int offset, int limit, String search, long timestamp) {
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MarketArgs other) {
                return this.limit == other.limit && this.offset == other.offset && this.search.equals(other.search);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + Integer.hashCode(offset);
            hash = hash * 31 + Integer.hashCode(limit);
            return hash * 31 + search.hashCode();
        }
    }

}
