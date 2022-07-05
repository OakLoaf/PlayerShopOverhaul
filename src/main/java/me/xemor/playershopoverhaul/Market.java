package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class Market implements Comparable<Market> {

    private final int marketID;
    private final ItemStack item;
    private double goingPrice = Double.NEGATIVE_INFINITY;

    public Market(int marketID, ItemStack item, double goingPrice) {
        this.marketID = marketID;
        this.item = item;
        this.goingPrice = goingPrice;
    }

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

    public double getGoingPrice() {
        if (goingPrice == Double.NEGATIVE_INFINITY) throw new IllegalStateException("Going Price has not been initialised!");
        return goingPrice;
    }

    /*
        public void addListing(Listing listing) {
            if (listing.getItem().isSimilar(item)) {
                listings.add(listing);
            }
            else {
                PlayerShopOverhaul.getInstance().getLogger().severe("A listing that doesn't match the market has been added to it!");
            }
        }

        public Listing getCheapestListing() {
            return listings.peek();
        }

        private int calculatePrice(int amount) {
            int price = 0;
            int amountUnaccountedFor = amount;
            for (Listing listing : listings) {
                int stock = listing.getStock();
                if (amountUnaccountedFor >= stock) {
                    amountUnaccountedFor -= stock;
                    price += listing.getPricePer() * stock;
                }
                else {
                    price += listing.getPricePer() * amountUnaccountedFor;
                    amountUnaccountedFor = 0;
                }
            }
            return price;
        }

        private void updateStock(int amount) {
            int amountUnaccountedFor = amount;
            while (amountUnaccountedFor > 0) {
                Listing listing = listings.peek();
                int stock = listing.getStock();
                if (amountUnaccountedFor >= stock) {
                    amountUnaccountedFor -= stock;
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(listing.getUUID());
                    economy.depositPlayer(offlinePlayer, stock * listing.getPricePer());
                    listings.poll();
                    tradeSystem.getPlayerToListings().remove(listing.getUUID(), listing);
                }
                else {
                    stock -= amountUnaccountedFor;
                    amountUnaccountedFor = 0;
                    listing.setStock(stock);
                }
            }
        }



        public void removeListing(Listing listing) {
            listings.remove(listing);
        }

        public boolean purchase(Player player, int amount) {
            if (amount > totalStock) return false;
            EconomyResponse economyResponse = economy.withdrawPlayer(player, calculatePrice(amount));
            if (economyResponse.transactionSuccess()) {
                updateStock(amount);
                ItemStack newItem = item.clone();
                newItem.setAmount(amount);
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newItem);
                World world = player.getWorld();
                for (ItemStack item : leftovers.values()) {
                    world.dropItemNaturally(player.getLocation(), item);
                }
            }
            return economyResponse.transactionSuccess();
        }


    */
    public String getName() {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : PlayerShopOverhaul.getInstance().getConfigHandler().getListingName().replace("%type%", item.getType().name());
    }

    public ItemStack getMarketRepresentation() {
        ItemStack representation = item.clone();
        ConfigHandler configHandler = PlayerShopOverhaul.getInstance().getConfigHandler();
        ItemMeta itemMeta = representation.getItemMeta();
        if (itemMeta == null) itemMeta = Bukkit.getItemFactory().getItemMeta(representation.getType());
        itemMeta.setDisplayName(getName());
        List<String> lore = configHandler.getListingLore();
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i,
                    lore.get(i).replaceAll("%price%", String.valueOf(goingPrice))
            );
        }
        itemMeta.setLore(lore);
        itemMeta.getPersistentDataContainer().set(
                new NamespacedKey(PlayerShopOverhaul.getInstance(), "marketID"),
                PersistentDataType.INTEGER,
                marketID);
        representation.setItemMeta(itemMeta);
        return representation;
    }

    @Override
    public int compareTo(Market o) {
        return item.getType().compareTo(o.item.getType());
    }
}
