package me.xemor.playershopoverhaul.userinterface;

import me.xemor.playershopoverhaul.*;
import me.xemor.playershopoverhaul.storage.CacheStorage;
import me.xemor.playershopoverhaul.storage.SQLStorage;
import me.xemor.playershopoverhaul.storage.SQLiteStorage;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.userinterface.ChestInterface;
import me.xemor.userinterface.TextInterface;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

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

    public GlobalTradeSystem() {
        marketIDKey = new NamespacedKey(PlayerShopOverhaul.getInstance(), "marketID");
        listingsIDKey = new NamespacedKey(PlayerShopOverhaul.getInstance(), "listingsID");
        priceKey =new NamespacedKey(PlayerShopOverhaul.getInstance(), "price");
        textInterface.title("Search");
        ConfigHandler configHandler = PlayerShopOverhaul.getInstance().getConfigHandler();
        if (configHandler.getDatabaseType().equals("MySQL")) {
            storage = new CacheStorage(new SQLStorage(configHandler));
        }
        else {
            storage = new CacheStorage(new SQLiteStorage(configHandler));
        }
        storage.setup();
    }

    public void showTradeSystemView(Player player) {
        ChestInterface<GTSData> chestInterface = new ChestInterface<>("Global Trade System", 5, new GTSData("", 0));
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    textInterface.getInput(otherPlayer, (result) -> {
                        data.setCurrentSearch(result);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                updateTradeSystemView(otherPlayer, chestInterface);
                            }
                        }.runTask(PlayerShopOverhaul.getInstance());
                    });
                }
            }.runTaskLater(PlayerShopOverhaul.getInstance(), 2L);
        });
        chestInterface.getInteractions().addInteraction(
                (item -> item != null && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(marketIDKey, PersistentDataType.INTEGER)
                ),
                (clickPlayer, item, clickType) -> {
                    int marketID = item.getItemMeta().getPersistentDataContainer().get(marketIDKey, PersistentDataType.INTEGER);
                    double cachedGoingPrice = item.getItemMeta().getPersistentDataContainer().get(priceKey, PersistentDataType.DOUBLE);
                    CompletableFuture<PricedMarket> marketFuture = storage.getPricedMarket(marketID);
                    marketFuture.exceptionally((ignored) -> {
                        Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "There is insufficient stock!"));
                        return null;
                    })
                    .thenAccept((market) -> {
                        try {
                            if (market.getGoingPrice() > cachedGoingPrice * 1.1) {
                                Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "Please try again and refresh/reopen the store in one minute! The price has increased by more than 10% since it was read."));
                                return;
                            }
                            if (clickType == ClickType.LEFT) {
                                storage.purchaseFromMarket(clickPlayer.getUniqueId(), market, 1).thenAccept((response) -> {
                                    if (response.transactionSuccess()) {
                                        HashMap<Integer, ItemStack> items = clickPlayer.getInventory().addItem(market.getItem().clone());
                                        for (ItemStack leftover : items.values()) {
                                            clickPlayer.getLocation().getWorld().dropItem(clickPlayer.getLocation(), leftover);
                                        }
                                        Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.GREEN + String.format("It was bought successfully for %.2f! You now have %.2f.", response.amount, response.balance)));
                                    }
                                    else {
                                        Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "You have insufficient funds!"));
                                    }
                                }).exceptionally((throwable -> {
                                    Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "There is insufficient stock!"));
                                    return null;
                                }));
                            }
                            else if (clickType == ClickType.SHIFT_LEFT) {
                                storage.purchaseFromMarket(clickPlayer.getUniqueId(), market, item.getType().getMaxStackSize()).thenAccept((response) -> {
                                    ItemStack toGive = market.getItem().clone();
                                    toGive.setAmount(item.getType().getMaxStackSize());
                                    if (response.transactionSuccess()) {
                                        HashMap<Integer, ItemStack> items = clickPlayer.getInventory().addItem(toGive);
                                        for (ItemStack leftover : items.values()) {
                                            clickPlayer.getLocation().getWorld().dropItem(clickPlayer.getLocation(), leftover);
                                        }
                                        Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.GREEN + String.format("You have bought 64 successfully for %.2f! You now have %.2f.", response.amount, response.balance)));
                                    }
                                    else {
                                        Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "You have insufficient funds!"));
                                    }
                                }).exceptionally((throwable -> {
                                    Bukkit.getScheduler().runTask(PlayerShopOverhaul.getInstance(), () -> clickPlayer.sendMessage(ChatColor.RED + "There is insufficient stock to buy 64!"));
                                    return null;
                                }));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
        );
        updateTradeSystemView(player, chestInterface);
    }

    private void updateTradeSystemView(Player player, ChestInterface<GTSData> chestInterface) {
        Inventory inventory = chestInterface.getInventory();
        player.openInventory(inventory);
        displayItems(chestInterface);
    }

    private void displayItems(ChestInterface<GTSData> chestInterface) {
        Inventory inventory = chestInterface.getInventory();
        GTSData data = chestInterface.getInteractions().getData();
        String search = data.getCurrentSearch();
        int page = data.getPageNumber();
        int offset = 21 * page;
        CompletableFuture<List<PricedMarket>> marketsFuture = storage.getMarkets(offset, search);
        marketsFuture.thenAccept((markets) -> {
            new BukkitRunnable() {
                @Override
                public void run() {
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
                }
            }.runTask(PlayerShopOverhaul.getInstance());
        });
    }

    public void showListings(Player player, UUID userToShow, int serverID) {
        ChestInterface<GTSData> chestInterface = new ChestInterface<>("Your Listings", 5, new GTSData("", 0));
        updateListingsView(userToShow, serverID, chestInterface);
        player.openInventory(chestInterface.getInventory());
    }

    private void updateListingsView(UUID dataUUID, int serverID, ChestInterface<GTSData> chestInterface) {
        ItemStack forwardArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getForwardArrow();
        ItemStack backArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getBackArrow();
        ItemStack backButton = PlayerShopOverhaul.getInstance().getConfigHandler().getMenuBackButton();
        GTSData data = chestInterface.getInteractions().getData();
        chestInterface.getInteractions().addSimpleInteraction(forwardArrow, (otherPlayer) -> {
            if (chestInterface.getInventory().getItem(10) != null) data.setPageNumber(data.getPageNumber() + 1);
            displayListingsViewItems(dataUUID, serverID, chestInterface);
        });
        chestInterface.getInteractions().addSimpleInteraction(backArrow, (otherPlayer) -> {
            if (data.getPageNumber() > 0) data.setPageNumber(data.getPageNumber() - 1);
            displayListingsViewItems(dataUUID, serverID, chestInterface);
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
                    future.thenAccept((ignored) -> {
                       HashMap<Integer, ItemStack> items = clickPlayer.getInventory().addItem(item);
                       for (ItemStack leftover : items.values()) {
                           clickPlayer.getLocation().getWorld().dropItem(clickPlayer.getLocation(), leftover);
                       }
                       inventory.remove(item);
                    });
                });
        //chestInterface.getInteractions().addCloseInteraction(this::showTradeSystemView);
        displayListingsViewItems(dataUUID, serverID, chestInterface);
    }

    private void displayListingsViewItems(UUID dataUUID, int serverID, ChestInterface<GTSData> chestInterface) {
        GTSData data = chestInterface.getInteractions().getData();
        CompletableFuture<List<Listing>> listingsFuture = storage.getPlayerListings(dataUUID, serverID, 21 * data.getPageNumber());
        listingsFuture.thenAccept((listings) -> {
            storage.getMarkets(listings).thenAccept((markets) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
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
                }.runTask(PlayerShopOverhaul.getInstance());
            });
        });
    }

    public CompletableFuture<Double> claimPayment(Player player) {
        Storage storage = PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage();
        CompletableFuture<Double> moneyFuture = storage.claimPayment(player.getUniqueId());
        moneyFuture.thenAccept((money) -> PlayerShopOverhaul.getInstance().getEconomy().depositPlayer(player, money));
        return moneyFuture;
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
