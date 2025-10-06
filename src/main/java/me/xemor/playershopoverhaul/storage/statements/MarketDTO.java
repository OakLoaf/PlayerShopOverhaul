package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.Market;
import me.xemor.playershopoverhaul.itemserialization.ItemSerialization;

public record MarketDTO(int id, byte[] item, String name) {
    public Market asMarket() {
        return new Market(id, ItemSerialization.binaryToItemStack(item));
    }
}
