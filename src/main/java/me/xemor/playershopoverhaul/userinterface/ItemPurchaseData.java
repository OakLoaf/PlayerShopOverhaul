package me.xemor.playershopoverhaul.userinterface;

import me.xemor.playershopoverhaul.PricedMarket;

public class ItemPurchaseData {
    private int numberToPurchase = 1;
    private final PricedMarket pricedMarket;

    public ItemPurchaseData(PricedMarket pricedMarket) {
        this.pricedMarket = pricedMarket;
    }

    public int getNumberToPurchase() {
        return numberToPurchase;
    }

    public ItemPurchaseData setNumberToPurchase(int numberToPurchase) {
        this.numberToPurchase = numberToPurchase;
        return this;
    }

    public void addNumberToPurchase(int numberToAdd) {
        numberToPurchase = Math.min(Math.max(numberToPurchase + numberToAdd, 0), pricedMarket.getStock());
    }

    public PricedMarket getPricedMarket() {
        return pricedMarket;
    }
}
