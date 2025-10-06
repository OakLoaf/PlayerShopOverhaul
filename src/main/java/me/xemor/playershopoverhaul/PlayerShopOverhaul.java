package me.xemor.playershopoverhaul;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.xemor.playershopoverhaul.commands.GTSCommand;
import me.xemor.playershopoverhaul.commands.PSOCommand;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.userinterface.UserInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.orphan.Orphans;

public final class PlayerShopOverhaul extends JavaPlugin implements Listener {

    private boolean hasPlaceholderAPI = false;
    private boolean isGtsEnabled = true;
    private Injector injector;

    @Override
    public void onEnable() {
        UserInterface.enable(this);
        injector = Guice.createInjector(
                new PlayerShopOverhaulModule(this)
        );
        ConfigHandler configHandler = injector.getInstance(ConfigHandler.class);

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this).build();
        lamp.register(Orphans.path(configHandler.getGtsCommandAliases().toArray(new String[]{})).handler(injector.getInstance(GTSCommand.class)));
        lamp.register(injector.getInstance(PSOCommand.class));

        hasPlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        injector.getInstance(Storage.class).setUsername(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    public void enableGts() {
        isGtsEnabled = true;
    }

    public void disableGts() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }
        isGtsEnabled = false;
    }

    public void reload() {
        ConfigHandler configHandler = injector.getInstance(ConfigHandler.class);
        configHandler.reloadConfig();
    }

    public boolean hasPlaceholderAPI() {
        return hasPlaceholderAPI;
    }

    public boolean isGtsEnabled() {
        return isGtsEnabled;
    }
}
