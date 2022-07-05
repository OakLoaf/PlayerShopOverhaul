package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.commands.GTSCommand;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerShopOverhaul extends JavaPlugin {

    private static PlayerShopOverhaul playerShopOverhaul;
    private ConfigHandler configHandler;
    private GlobalTradeSystem globalTradeSystem;
    private Economy econ;

    @Override
    public void onEnable() {
        // Plugin startup logic
        playerShopOverhaul = this;
        configHandler = new ConfigHandler();
        globalTradeSystem = new GlobalTradeSystem();
        PluginCommand gts = this.getServer().getPluginCommand("gts");
        GTSCommand gtsCommand = new GTSCommand();
        gts.setExecutor(gtsCommand);
        gts.setTabCompleter(gtsCommand);
        if (!setupEconomy()) this.getLogger().severe("Failed to setup economy plugin");
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
