package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import org.bukkit.inventory.ItemStack;

public class Market {

    private final int marketID;
    private final ItemStack item;

    public Market(int marketID, ItemStack item) {
        this.marketID = marketID;
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getID() {
        return marketID;
    }

    public String getName(ConfigHandler configHandler) {
        return PricedMarket.getName(configHandler, item);
    }

    public static String getName(ConfigHandler configHandler, ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : configHandler.getListingName(item.getType().name());
    }

    public int getMarketID() {
        return marketID;
    }
}
