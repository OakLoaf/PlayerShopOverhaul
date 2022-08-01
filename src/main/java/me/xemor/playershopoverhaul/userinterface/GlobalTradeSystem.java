package me.xemor.playershopoverhaul.userinterface;

import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.Market;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.storage.CacheStorage;
import me.xemor.playershopoverhaul.storage.SQLStorage;
import me.xemor.playershopoverhaul.storage.SQLiteStorage;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.userinterface.ChestInterface;
import me.xemor.userinterface.TextInterface;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.xml.stream.events.Namespace;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GlobalTradeSystem implements Listener {

    private static NamespacedKey marketIDKey;
    private static NamespacedKey listingsIDKey;
    private static NamespacedKey priceKey;
    private final TextInterface textInterface = new TextInterface();
    private final Storage storage;

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
        chestInterface.getInteractions().addSimpleInteraction(myListings, this::showListings);
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
                    CompletableFuture<Market> marketFuture = storage.getMarket(marketID);
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
                                        clickPlayer.getInventory().addItem(market.getItem().clone());
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
                                storage.purchaseFromMarket(clickPlayer.getUniqueId(), market, 64).thenAccept((response) -> {
                                    ItemStack toGive = market.getItem().clone();
                                    toGive.setAmount(64);
                                    if (response.transactionSuccess()) {
                                        clickPlayer.getInventory().addItem(toGive);
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
        int limit = 21 * (page + 1);
        CompletableFuture<List<Market>> marketsFuture = storage.getMarkets(offset, limit, search);
        marketsFuture.thenAccept((markets) -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 21; i++) {
                        ItemStack item;
                        if (offset + i < markets.size()) {
                            Market market = markets.get(i);
                            item = market.getMarketRepresentation();
                        }
                        else {
                            item = new ItemStack(Material.AIR);
                        }
                        inventory.setItem(i + 10 + (i / 7) * 2, item);
                    }
                }
            }.runTask(PlayerShopOverhaul.getInstance());
        });
    }

    public void showListings(Player player) {
        ChestInterface<GTSData> chestInterface = new ChestInterface<>("Your Listings", 5, new GTSData("", 0));
        updateListingsView(player, chestInterface);
        player.openInventory(chestInterface.getInventory());
    }

    private void updateListingsView(Player player, ChestInterface<GTSData> chestInterface) {
        ItemStack forwardArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getForwardArrow();
        ItemStack backArrow = PlayerShopOverhaul.getInstance().getConfigHandler().getBackArrow();
        ItemStack backButton = PlayerShopOverhaul.getInstance().getConfigHandler().getMenuBackButton();
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
                       clickPlayer.getInventory().addItem(item);
                       inventory.remove(item);
                    });
                });
        //chestInterface.getInteractions().addCloseInteraction(this::showTradeSystemView);
        displayListingsViewItems(player, chestInterface);
    }

    private void displayListingsViewItems(Player player, ChestInterface<GTSData> chestInterface) {
        GTSData data = chestInterface.getInteractions().getData();
        CompletableFuture<List<Listing>> listingsFuture = storage.getPlayerListings(player.getUniqueId(), 21 * data.getPageNumber(), 21 * (data.getPageNumber() + 1));
        listingsFuture.thenAccept((listings) -> {
            storage.getMarkets(listings).thenAccept((markets) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Inventory inventory = chestInterface.getInventory();
                        for (int i = 0; i < 21 && i < markets.size(); i++) {
                            ItemStack item;
                            if (i < listings.size()) {
                                item = markets.get(i).getItem();
                                ItemMeta meta = item.getItemMeta();
                                meta.getPersistentDataContainer().set(listingsIDKey, PersistentDataType.INTEGER, listings.get(i).getID());
                                item.setItemMeta(meta);
                            }
                            else {
                                item = new ItemStack(Material.AIR);
                            }
                            item.setAmount(listings.get(i).getStock());
                            inventory.setItem(i + 10 + (i / 7) * 2, item);
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
