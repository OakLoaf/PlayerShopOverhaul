package me.xemor.playershopoverhaul;

import org.bukkit.inventory.ItemStack;

public class Market {

    final int marketID;
    final ItemStack item;

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

    public String getName() {
        return PricedMarket.getName(item);
    }

    public static String getName(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : PlayerShopOverhaul.getInstance().getConfigHandler().getListingName(item.getType().name());
    }
}
