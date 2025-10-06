package me.xemor.playershopoverhaul.storage.fastofflineplayer;

import com.mysql.cj.util.LRUCache;
import jakarta.inject.Inject;
import me.xemor.playershopoverhaul.storage.Storage;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OfflinePlayerCache {

    private final Map<UUID, OfflinePlayer> offlinePlayerCache = Collections.synchronizedMap(new LRUCache<>(42));

    @Inject
    private Storage storage;

    public CompletableFuture<OfflinePlayer> getOfflinePlayer(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer);
        }
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
        storage.getUsername(uuid).thenAccept((name) -> {
            InterfaceMock.MethodOverride nameOverride = new InterfaceMock.MethodOverride() {
                @Override
                public boolean overrides(Method method) {
                    try {
                        return OfflinePlayer.class.getMethod("getName").equals(method);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Object invoke() {
                    return name;
                }
            };
            InterfaceMock.MethodOverride uuidOverride = new InterfaceMock.MethodOverride() {
                @Override
                public boolean overrides(Method method) {
                    try {
                        return OfflinePlayer.class.getMethod("getUniqueId").equals(method);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Object invoke() {
                    return uuid;
                }
            };
            completableFuture.complete(
                    InterfaceMock.mockInterface(OfflinePlayer.class, nameOverride, uuidOverride)
            );
        });
        return completableFuture;
    }

}
