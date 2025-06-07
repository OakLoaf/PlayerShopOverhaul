package me.xemor.playershopoverhaul;

import java.util.UUID;

public class Listing implements Comparable<Listing> {

    private final int marketID;
    private final int serverId;
    private final UUID sellerID;
    private final int stock;
    private final int id;
    private final double pricePer;

    public Listing(int id, int serverId, UUID sellerID, int marketID, int stock, double pricePer) {
        this.sellerID = sellerID;
        this.serverId = serverId;
        this.id = id;
        this.marketID = marketID;
        this.stock = stock;
        this.pricePer = pricePer;
    }

    public UUID getSellerID() {
        return sellerID;
    }

    public int getStock() {
        return stock;
    }

    public double getPricePer() {
        return pricePer;
    }

    public int getServerId() {
        return serverId;
    }

    public double getTotalPrice() {
        return pricePer * stock;
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
