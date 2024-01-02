package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.commands.gts.GTSCommand;
import me.xemor.playershopoverhaul.commands.pso.PSOCommand;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.userinterface.UserInterface;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class PlayerShopOverhaul extends JavaPlugin implements Listener {

    private static PlayerShopOverhaul playerShopOverhaul;
    private ConfigHandler configHandler;
    private GlobalTradeSystem globalTradeSystem;
    private BukkitAudiences bukkitAudiences;
    private Economy econ;
    private boolean hasPlaceholderAPI = false;
    private boolean isCommandRegistered = true;
    private OfflinePlayerCache offlinePlayerCache = new OfflinePlayerCache();

    @Override
    public void onEnable() {
        // Plugin startup logic
        playerShopOverhaul = this;
        UserInterface.enable(this);
        configHandler = new ConfigHandler();
        globalTradeSystem = new GlobalTradeSystem();
        bukkitAudiences = BukkitAudiences.create(this);
        registerCommand();
        PluginCommand pso = this.getCommand("pso");
        PSOCommand psoCommand = new PSOCommand();
        pso.setTabCompleter(psoCommand);
        pso.setExecutor(psoCommand);
        if (!setupEconomy()) this.getLogger().severe("Failed to setup economy plugin");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) hasPlaceholderAPI = true;
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        globalTradeSystem.getStorage().setUsername(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    public void registerCommand() {
        GTSCommand gtsCommand = new GTSCommand("gts");
        gtsCommand.setAliases(configHandler.getGtsCommandAliases());

        try {
            getCommandMap().register("gts", gtsCommand);
            isCommandRegistered = true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void unregisterCommand() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }
//
//        PluginCommand gts = this.getServer().getPluginCommand("gts");
//        gts.setExecutor((sender, command, label, args) -> true);
//        gts.setTabCompleter((sender, command, label, args) -> List.of());
//        isCommandRegistered = false;

        try {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) {
                return;
            }

            commandMap.getKnownCommands().values().stream()
                .filter(command -> command.getLabel().equalsIgnoreCase("gts") || command.getAliases().contains("gts"))
                .findFirst().ifPresent(match -> match.unregister(commandMap));

            isCommandRegistered = false;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private CommandMap getCommandMap() throws NoSuchFieldException, IllegalAccessException {
        final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);
        return (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
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

    public boolean isCommandRegistered() {
        return isCommandRegistered;
    }

    public BukkitAudiences getBukkitAudiences() {
        return bukkitAudiences;
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
}
