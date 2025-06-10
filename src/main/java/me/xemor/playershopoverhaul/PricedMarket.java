package me.xemor.playershopoverhaul;

import me.clip.placeholderapi.PlaceholderAPI;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class PricedMarket extends Market implements Comparable<PricedMarket>  {

    private final double goingPrice;
    private final UUID goingPriceSeller;
    private int stock = 0;

    public PricedMarket(int marketID, ItemStack item, double goingPrice, UUID goingPriceSeller, int stock) {
        super(marketID, item);
        this.goingPrice = goingPrice;
        this.goingPriceSeller = goingPriceSeller;
        this.stock = stock;
    }

    public int getStock() {
        return stock;
    }

    public double getGoingPrice() {
        if (goingPrice == Double.NEGATIVE_INFINITY) throw new IllegalStateException("Going Price has not been initialised!");
        return goingPrice;
    }

    public UUID getGoingPriceSeller() {
        return goingPriceSeller;
    }

    @Override
    public int compareTo(PricedMarket o) {
        return getItem().getType().compareTo(o.getItem().getType());
    }
}
