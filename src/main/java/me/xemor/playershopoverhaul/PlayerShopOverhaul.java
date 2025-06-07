package me.xemor.playershopoverhaul;

import me.xemor.foliahacks.FoliaHacks;
import me.xemor.playershopoverhaul.commands.gts.GTSCommand;
import me.xemor.playershopoverhaul.commands.pso.PSOCommand;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.fastofflineplayer.OfflinePlayerCache;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.userinterface.UserInterface;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

public final class PlayerShopOverhaul extends JavaPlugin implements Listener {

    private static PlayerShopOverhaul playerShopOverhaul;
    private ConfigHandler configHandler;
    private GlobalTradeSystem globalTradeSystem;
    private Economy econ;
    private boolean hasPlaceholderAPI = false;
    private boolean isGtsEnabled = true;
    private final OfflinePlayerCache offlinePlayerCache = new OfflinePlayerCache();
    private FoliaHacks foliaHacks;

    @Override
    public void onEnable() {
        // Plugin startup logic
        foliaHacks = new FoliaHacks(this);
        playerShopOverhaul = this;
        UserInterface.enable(this);
        configHandler = new ConfigHandler();
        globalTradeSystem = new GlobalTradeSystem();
        enableGts();
        PluginCommand pso = this.getCommand("pso");
        PSOCommand psoCommand = new PSOCommand();
        pso.setTabCompleter(psoCommand);
        pso.setExecutor(psoCommand);
        PluginCommand gts = this.getCommand("gts");
        GTSCommand gtsCommand = new GTSCommand();
        gts.setAliases(configHandler.getGtsCommandAliases());
        gts.setTabCompleter(gtsCommand);
        gts.setExecutor(gtsCommand);
        if (!setupEconomy()) this.getLogger().severe("Failed to setup economy plugin");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) hasPlaceholderAPI = true;
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        globalTradeSystem.getStorage().setUsername(e.getPlayer().getUniqueId(), e.getPlayer().getName());
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
        configHandler.reloadConfig();
        globalTradeSystem = new GlobalTradeSystem();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public boolean hasPlaceholderAPI() {
        return hasPlaceholderAPI;
    }

    public boolean isGtsEnabled() {
        return isGtsEnabled;
    }

    public Economy getEconomy() {
        return econ;
    }

    public GlobalTradeSystem getGlobalTradeSystem() {
        return globalTradeSystem;
    }

    public ConfigHandler getConfigHandler() { return configHandler; }

    public static PlayerShopOverhaul getInstance() {
        return playerShopOverhaul;
    }

    public OfflinePlayerCache getOfflinePlayerCache() {
        return offlinePlayerCache;
    }

    public FoliaHacks getFoliaHacks() {
        return foliaHacks;
    }

    public GracefulScheduling getScheduling() {
        return foliaHacks.getScheduling();
    }
}
