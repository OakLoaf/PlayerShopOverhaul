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
    private static final Pattern matchPlaceholders = Pattern.compile("%(.*?)%");

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

    public MarketRepresentation getMarketRepresentation() {
        ItemStack representation = getItem().clone();
        ConfigHandler configHandler = PlayerShopOverhaul.getInstance().getConfigHandler();
        ItemMeta itemMeta = representation.getItemMeta();
        if (itemMeta == null) itemMeta = Bukkit.getItemFactory().getItemMeta(representation.getType());
        String name = getName();
        List<String> lore = new ArrayList<>(configHandler.getListingLore(goingPrice, stock));
        itemMeta.getPersistentDataContainer().set(
                GlobalTradeSystem.getMarketIDKey(),
                PersistentDataType.INTEGER,
                getMarketID());
        itemMeta.getPersistentDataContainer().set(
                GlobalTradeSystem.getPriceKey(),
                PersistentDataType.DOUBLE,
                goingPrice);
        CompletableFuture<ItemStack> finishedItemStack = new CompletableFuture<>();
        if (PlayerShopOverhaul.getInstance().hasPlaceholderAPI()) {
            String loadingDisplayName = matchPlaceholders.matcher(name).replaceAll("Loading...");
            List<String> loadingLore = lore.stream().map((line) -> matchPlaceholders.matcher(line).replaceAll("Loading...")).toList();
            ItemMeta finalItemMeta = itemMeta;
            PlayerShopOverhaul.getInstance().getOfflinePlayerCache().getOfflinePlayer(goingPriceSeller).thenAccept((offlinePlayer -> {
                Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> {
                    String placeholderedDisplayName = PlaceholderAPI.setPlaceholders(offlinePlayer, name);
                    List<String> placeholderedLore = PlaceholderAPI.setPlaceholders(offlinePlayer, lore);
                    finalItemMeta.setDisplayName(placeholderedDisplayName);
                    finalItemMeta.setLore(placeholderedLore);
                    representation.setItemMeta(finalItemMeta);
                    finishedItemStack.complete(representation);
                });
            }));
            itemMeta.setDisplayName(loadingDisplayName);
            itemMeta.setLore(loadingLore);
            representation.setItemMeta(itemMeta);
        }
        else {
            itemMeta.setDisplayName(name);
            itemMeta.setLore(lore);
            representation.setItemMeta(itemMeta);
            finishedItemStack.complete(representation);
        }
        return new MarketRepresentation(representation, finishedItemStack);
    }

    public record MarketRepresentation(ItemStack instant, CompletableFuture<ItemStack> finished) {}

    @Override
    public int compareTo(PricedMarket o) {
        return getItem().getType().compareTo(o.getItem().getType());
    }
}
