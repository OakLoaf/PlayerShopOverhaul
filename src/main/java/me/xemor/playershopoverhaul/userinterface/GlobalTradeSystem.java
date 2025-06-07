package me.xemor.playershopoverhaul.userinterface;

import me.xemor.playershopoverhaul.*;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.SQLStorage;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.userinterface.ChestInterface;
import me.xemor.userinterface.textinterface.TextInterface;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GlobalTradeSystem implements Listener {

    private static NamespacedKey marketIDKey;
    private static NamespacedKey listingsIDKey;
    private static NamespacedKey priceKey;
    private final TextInterface textInterface = new TextInterface();
    private final Storage storage;
    private final static ItemStack air = new ItemStack(Material.AIR);
    private final ItemPurchaseView itemPurchaseView;

    public GlobalTradeSystem() {
        marketIDKey = new NamespacedKey(PlayerShopOverhaul.getInstance(), "marketID");
        listingsIDKey = new NamespacedKey(PlayerShopOverhaul.getInstance(), "listingsID");
        priceKey =new NamespacedKey(PlayerShopOverhaul.getInstance(), "price");
        textInterface.title("Search");
        ConfigHandler configHandler = PlayerShopOverhaul.getInstance().getConfigHandler();
        // TODO: turn cache back on
        storage = new SQLStorage(configHandler);
        itemPurchaseView = new ItemPurchaseView(configHandler);
    }

    public void showTradeSystemView(Player player) {
        ChestInterface<GTSData> chestInterface = new ChestInterface<>(PlayerShopOverhaul.getInstance().getConfigHandler().getGuiTitle(), 5, new GTSData("", 0));
        GTSData data = chestInterface.getInteractions().getData();
        ItemStack search = PlayerShopOverhaul.getInstance().getConfigHandler().getSearch();
        ItemStack myListings = PlayerShopOverhaul.getInstance().getConfigHandler().getListings();
        ItemStack forwardArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getForwardArrow();
        ItemStack backArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getBackArrow();
        ItemStack refresh = PlayerShopOverhaul.getInstance().getConfigHandler().getRefresh();
        chestInterface.calculateInventoryContents(
                new String[] {
                        "    R    ",
                        "         ",
                        "B       F",
                        "         ",
                        "L       S"
                },
                Map.of('B', backArrow, 'F', forwardArrow, 'L', myListings, 'S', search, 'R', refresh)
        );
        chestInterface.getInteractions().addSimpleInteraction(forwardArrow, (otherPlayer) -> {
            if (chestInterface.getInventory().getItem(10) != null) data.setPageNumber(data.getPageNumber() + 1);
            updateTradeSystemView(player, chestInterface);
        });
        chestInterface.getInteractions().addSimpleInteraction(backArrow, (otherPlayer) -> {
            if (data.getPageNumber() > 0) data.setPageNumber(data.getPageNumber() - 1);
            updateTradeSystemView(player, chestInterface);
        });
        chestInterface.getInteractions().addSimpleInteraction(myListings, (otherPlayer) -> showListings(otherPlayer, otherPlayer.getUniqueId(), PlayerShopOverhaul.getInstance().getConfigHandler().getServerID()));
        chestInterface.getInteractions().addSimpleInteraction(refresh, this::showTradeSystemView);
        chestInterface.getInteractions().addSimpleInteraction(search, (otherPlayer) -> {
            player.closeInventory();
            PlayerShopOverhaul.getInstance().getScheduling().entitySpecificScheduler(otherPlayer)
                    .runDelayed(() -> {
                        textInterface.getInput(otherPlayer, (result) -> {
                            data.setCurrentSearch(result);
                            PlayerShopOverhaul.getInstance().getFoliaHacks()
                                    .runASAP(otherPlayer, () -> updateTradeSystemView(otherPlayer, chestInterface));
                        });
                    }, () -> {}, 2L);
        });
        chestInterface.getInteractions().addInteraction(
                (item -> item != null && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(marketIDKey, PersistentDataType.INTEGER)
                ),
                (clickPlayer, item, clickType) -> {
                    int marketID = item.getItemMeta().getPersistentDataContainer().get(marketIDKey, PersistentDataType.INTEGER);
                    storage.getPricedMarket(marketID).thenAccept((market) -> {
                        PlayerShopOverhaul.getInstance().getFoliaHacks().runASAP(clickPlayer, () -> {
                            itemPurchaseView.displayItemPurchaseView(clickPlayer, market);
                        });
                    });
                }
        );
        updateTradeSystemView(player, chestInterface);
    }

    private void updateTradeSystemView(Player player, ChestInterface<GTSData> chestInterface) {
        displayItems(player, chestInterface);
    }

    private void displayItems(Player player, ChestInterface<GTSData> chestInterface) {
        Inventory inventory = chestInterface.getInventory();
        GTSData data = chestInterface.getInteractions().getData();
        String search = data.getCurrentSearch();
        int page = data.getPageNumber();
        int offset = 21 * page;
        CompletableFuture<List<PricedMarket>> marketsFuture = storage.getMarkets(offset, search);
        marketsFuture.thenAccept((markets) -> {
            PlayerShopOverhaul.getInstance().getFoliaHacks().runASAP(player, () -> {
                assert markets.size() <= 21;
                for (int i = 0; i < 21; i++) { //markets.size() should be 21 in all circumstances as a page has 21 items on it
                    int index = i + 10 + (i / 7) * 2;
                    if (i < markets.size()) {
                        PricedMarket pricedMarket = markets.get(i);
                        PricedMarket.MarketRepresentation representation = pricedMarket.getMarketRepresentation();
                        inventory.setItem(index, representation.instant());
                        representation.finished().thenAccept((finished) -> {
                            Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> {
                                inventory.setItem(index, finished);
                            });
                        });
                    }
                    else {
                        inventory.setItem(index, air);
                    }
                }
            });
        });
        player.openInventory(inventory);
    }

    public void showListings(Player player, UUID userToShow, int serverID) {
        ChestInterface<GTSData> chestInterface = new ChestInterface<>("Your Listings", 5, new GTSData("", 0));
        updateListingsView(player, userToShow, serverID, chestInterface);
        player.openInventory(chestInterface.getInventory());
    }

    private void updateListingsView(Player player, UUID dataUUID, int serverID, ChestInterface<GTSData> chestInterface) {
        ItemStack forwardArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getForwardArrow();
        ItemStack backArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getBackArrow();
        ItemStack backButton = PlayerShopOverhaul.getInstance().getConfigHandler().getMenuBackButton();
        GTSData data = chestInterface.getInteractions().getData();
        chestInterface.getInteractions().addSimpleInteraction(forwardArrow, (otherPlayer) -> {
            if (chestInterface.getInventory().getItem(10) != null) data.setPageNumber(data.getPageNumber() + 1);
            displayListingsViewItems(otherPlayer, dataUUID, serverID, chestInterface);
        });
        chestInterface.getInteractions().addSimpleInteraction(backArrow, (otherPlayer) -> {
            if (data.getPageNumber() > 0) data.setPageNumber(data.getPageNumber() - 1);
            displayListingsViewItems(otherPlayer, dataUUID, serverID, chestInterface);
        });
        chestInterface.calculateInventoryContents(new String[] {
                        "    R    ",
                        "         ",
                        "B       F",
                        "         ",
                        "    b    "
                },
                Map.of('B', backArrow, 'F', forwardArrow, 'b', backButton));
        chestInterface.getInteractions().addSimpleInteraction(backButton, this::showTradeSystemView);
        chestInterface.getInteractions().addInteraction((item) -> item != null && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(listingsIDKey, PersistentDataType.INTEGER),
                (clickPlayer, item, clickType) -> {
                    Inventory inventory = clickPlayer.getOpenInventory().getTopInventory();
                    ItemMeta itemMeta = item.getItemMeta();
                    int id = itemMeta.getPersistentDataContainer().get(listingsIDKey, PersistentDataType.INTEGER);
                    itemMeta.getPersistentDataContainer().remove(listingsIDKey);
                    item.setItemMeta(itemMeta);
                    CompletableFuture<Object> future = storage.removeListing(id);
                    inventory.remove(item);
                    future.thenAccept((ignored) -> {
                       HashMap<Integer, ItemStack> items = clickPlayer.getInventory().addItem(item);
                       for (ItemStack leftover : items.values()) {
                           clickPlayer.getLocation().getWorld().dropItem(clickPlayer.getLocation(), leftover);
                       }
                    });
                    future.exceptionally((error) -> {
                        clickPlayer.sendMessage(ChatColor.RED + "This item has already been purchased!");
                        return null;
                    });
                });
        //chestInterface.getInteractions().addCloseInteraction(this::showTradeSystemView);
        displayListingsViewItems(player, dataUUID, serverID, chestInterface);
    }

    private void displayListingsViewItems(Player player, UUID dataUUID, int serverID, ChestInterface<GTSData> chestInterface) {
        GTSData data = chestInterface.getInteractions().getData();
        CompletableFuture<List<Listing>> listingsFuture = storage.getPlayerListings(dataUUID, serverID, 21 * data.getPageNumber());
        listingsFuture.thenAccept((listings) -> {
            storage.getMarkets(listings).thenAccept((markets) -> {
                PlayerShopOverhaul.getInstance().getFoliaHacks().runASAP(player,
                        () -> {
                            Inventory inventory = chestInterface.getInventory();
                            for (int i = 0; i < 21; i++) {
                                ItemStack item;
                                int index = i + 10 + (i / 7) * 2;
                                if (i < listings.size()) {
                                    item = markets.get(i).getItem();
                                    ItemMeta meta = item.getItemMeta();
                                    meta.getPersistentDataContainer().set(listingsIDKey, PersistentDataType.INTEGER, listings.get(i).getID());
                                    item.setItemMeta(meta);
                                    item.setAmount(listings.get(i).getStock());
                                    inventory.setItem(index, item);
                                }
                                else {
                                    inventory.setItem(index, air);
                                }
                            }
                        }
                );
            });
        });
    }

    public CompletableFuture<Double> claimPayment(Player player) {
        Storage storage = PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage();
        return storage.claimPayment(player.getUniqueId());
    }

    public static NamespacedKey getMarketIDKey() {
        return marketIDKey;
    }

    public static NamespacedKey getListingsIDKey() {
        return listingsIDKey;
    }

    public static NamespacedKey getPriceKey() {
        return priceKey;
    }

    public Storage getStorage() {
        return storage;
    }
}
