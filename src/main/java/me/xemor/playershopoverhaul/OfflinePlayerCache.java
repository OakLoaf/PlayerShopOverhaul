package me.xemor.playershopoverhaul;

import com.mysql.cj.util.LRUCache;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class OfflinePlayerCache {

    private final Map<UUID, OfflinePlayer> offlinePlayerCache = Collections.synchronizedMap(new LRUCache<>(42));

    public CompletableFuture<OfflinePlayer> getOfflinePlayer(UUID uuid) {
        OfflinePlayer offlinePlayer = offlinePlayerCache.get(uuid);
        CompletableFuture<OfflinePlayer> completableFuture;
        if (offlinePlayer == null) {
            completableFuture = computeOfflinePlayer(uuid);
            completableFuture.thenAccept((otherPlayer) -> offlinePlayerCache.put(uuid, otherPlayer));
        }
        else {
            completableFuture = CompletableFuture.completedFuture(offlinePlayer);
        }
        return completableFuture;
    }

    private CompletableFuture<OfflinePlayer> computeOfflinePlayer(UUID uuid) {
        CompletableFuture<OfflinePlayer> completableFuture = new CompletableFuture<>();
        PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage().getUsername(uuid).thenAccept((name) -> {
            completableFuture.complete(new FastOfflinePlayer(uuid, name));
        });
        return completableFuture;
    }

    public static class FastOfflinePlayer implements OfflinePlayer {

        private final UUID uuid;

        public FastOfflinePlayer(UUID uuid, String name) {
            this.uuid = uuid;
        }

        @Override
        public boolean isOnline() {
            return Bukkit.getPlayer(uuid) != null;
        }

        @Nullable
        @Override
        public String getName() {
            return null;
        }

        @NotNull
        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @NotNull
        @Override
        public PlayerProfile getPlayerProfile() {
            return null; //shouldn't be needed by placeholders
        }

        @Override
        public boolean isBanned() {
            return false; //probs not needed in a shop plugin
        }

        @Override
        public boolean isWhitelisted() {
            return false; //probs not needed in a shop plugin
        }

        @Override
        public void setWhitelisted(boolean value) {}

        @Nullable
        @Override
        public Player getPlayer() {
            return Bukkit.getPlayer(uuid);
        }

        @Override
        public long getFirstPlayed() {
            return 0; //not needed for most placeholders
        }

        @Override
        public long getLastPlayed() {
            return 0; //not needed for most placeholders
        }

        @Override
        public boolean hasPlayedBefore() {
            return false; //not needed for most placeholders
        }

        @Nullable
        @Override
        public Location getBedSpawnLocation() {
            return null; //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void setStatistic(@NotNull Statistic statistic, int newValue) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public int getStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
            return 0; //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
            return 0; //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
            return 0; //not needed for most placeholders
        }

        @Override
        public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) throws IllegalArgumentException {
            //not needed for most placeholders
        }

        @Override
        public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {
            //not needed for most placeholders
        }

        @Override
        public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) {
            //not needed for most placeholders
        }

        @Nullable
        @Override
        public Location getLastDeathLocation() {
            return null; //not needed for most placeholders
        }

        @NotNull
        @Override
        public Map<String, Object> serialize() {
            return null; //not needed for most placeholders
        }

        @Override
        public boolean isOp() {
            return false; //not needed for most placeholders
        }

        @Override
        public void setOp(boolean value) {
            //not needed for most placeholders
        }
    }

}
