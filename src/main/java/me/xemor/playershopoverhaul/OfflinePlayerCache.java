package me.xemor.playershopoverhaul;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mysql.cj.util.LRUCache;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
        private final String name;

        public FastOfflinePlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public boolean isOnline() {
            return Bukkit.getPlayer(uuid) != null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Override
        public @NotNull PlayerProfile getPlayerProfile() {
            return null;
        }

        @Override
        public boolean isBanned() {
            return false; //probs not needed in a shop plugin
        }

        @Override
        public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String s, @Nullable Date date, @Nullable String s1) {
            return null;
        }

        @Override
        public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String s, @Nullable Instant instant, @Nullable String s1) {
            return null;
        }

        @Override
        public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String s, @Nullable Duration duration, @Nullable String s1) {
            return null;
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
        public long getLastLogin() {
            return 0;
        }

        @Override
        public long getLastSeen() {
            return 0;
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
