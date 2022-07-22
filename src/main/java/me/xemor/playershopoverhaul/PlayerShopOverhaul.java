package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.commands.gts.GTSCommand;
import me.xemor.playershopoverhaul.commands.pso.PSOCommand;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PlayerShopOverhaul extends JavaPlugin {

    private static PlayerShopOverhaul playerShopOverhaul;
    private ConfigHandler configHandler;
    private GlobalTradeSystem globalTradeSystem;
    private BukkitAudiences bukkitAudiences;
    private Economy econ;
    private boolean isCommandRegistered = true;

    @Override
    public void onEnable() {
        // Plugin startup logic
        playerShopOverhaul = this;
        configHandler = new ConfigHandler();
        globalTradeSystem = new GlobalTradeSystem();
        bukkitAudiences = BukkitAudiences.create(this);
        registerCommand();
        PluginCommand pso = this.getCommand("pso");
        PSOCommand psoCommand = new PSOCommand();
        pso.setTabCompleter(psoCommand);
        pso.setExecutor(psoCommand);
        if (!setupEconomy()) this.getLogger().severe("Failed to setup economy plugin");
    }

    public void registerCommand() {
        PluginCommand gts = this.getServer().getPluginCommand("gts");
        GTSCommand gtsCommand = new GTSCommand();
        gts.setExecutor(gtsCommand);
        gts.setTabCompleter(gtsCommand);
        isCommandRegistered = true;
    }

    public void unregisterCommand() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.closeInventory();
        }
        PluginCommand gts = this.getServer().getPluginCommand("gts");
        gts.setExecutor((sender, command, label, args) -> true);
        gts.setTabCompleter((sender, command, label, args) -> List.of());
        isCommandRegistered = false;
    }

    public void reload() {
        configHandler = new ConfigHandler();
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


}
