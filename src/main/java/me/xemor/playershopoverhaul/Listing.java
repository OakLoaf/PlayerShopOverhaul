package me.xemor.playershopoverhaul;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class Listing implements Comparable<Listing> {

    private final int marketID;
    private final UUID uuid;
    private final int stock;
    private final int id;
    private final double pricePer;

    public Listing(int id, UUID uuid, int marketID, int stock, double pricePer) {
        this.uuid = uuid;
        this.id = id;
        this.marketID = marketID;
        this.stock = stock;
        this.pricePer = pricePer;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getStock() {
        return stock;
    }

    public double getPricePer() {
        return pricePer;
    }

    public int getID() {
        return id;
    }

    @Override
    public int compareTo(Listing o) {
        return Double.compare(pricePer, o.pricePer);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Listing otherListing) {
            return this.id == otherListing.id;
        }
        return false;
    }
}
