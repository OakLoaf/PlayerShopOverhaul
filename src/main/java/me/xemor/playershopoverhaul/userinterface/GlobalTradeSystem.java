package me.xemor.playershopoverhaul.userinterface;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.clip.placeholderapi.PlaceholderAPI;
import me.xemor.foliahacks.FoliaHacks;
import me.xemor.playershopoverhaul.*;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.playershopoverhaul.storage.fastofflineplayer.OfflinePlayerCache;
import me.xemor.userinterface.chestinterface.ChestInterface;
import me.xemor.userinterface.textinterface.TextInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Singleton
public class GlobalTradeSystem implements Listener {

    private final PlayerShopOverhaul playerShopOverhaul;
    private final ConfigHandler configHandler;
    private final FoliaHacks foliaHacks;
    private final OfflinePlayerCache offlinePlayerCache;
    private static NamespacedKey marketIDKey;
    private static NamespacedKey listingsIDKey;
    private static NamespacedKey priceKey;
    private final TextInterface textInterface = new TextInterface();
    private final Storage storage;
    private final static ItemStack air = new ItemStack(Material.AIR);
    private final ItemPurchaseView itemPurchaseView;
    private static final Pattern matchPlaceholders = Pattern.compile("%(.*?)%");

    @Inject
    public GlobalTradeSystem(
            PlayerShopOverhaul playerShopOverhaul,
            ConfigHandler configHandler,
            FoliaHacks foliaHacks,
            OfflinePlayerCache offlinePlayerCache,
            Storage storage,
            ItemPurchaseView itemPurchaseView
    ) {
        this.playerShopOverhaul = playerShopOverhaul;
        this.configHandler = configHandler;
        this.foliaHacks = foliaHacks;
        this.offlinePlayerCache = offlinePlayerCache;
        this.storage = storage;
        this.itemPurchaseView = itemPurchaseView;
        marketIDKey = new NamespacedKey(playerShopOverhaul, "marketID");
        listingsIDKey = new NamespacedKey(playerShopOverhaul, "listingsID");
        priceKey = new NamespacedKey(playerShopOverhaul, "price");
        textInterface.title("Search");
    }

    public MarketRepresentation getMarketRepresentation(PricedMarket pricedMarket) {
        ItemStack representation = pricedMarket.getItem().clone();
        ItemMeta itemMeta = representation.getItemMeta();
        if (itemMeta == null) itemMeta = Bukkit.getItemFactory().getItemMeta(representation.getType());
        String name = PricedMarket.getName(configHandler, pricedMarket.getItem());
        List<String> lore = new ArrayList<>(configHandler.getListingLore(pricedMarket.getGoingPrice(), pricedMarket.getStock()));
        itemMeta.getPersistentDataContainer().set(
                GlobalTradeSystem.getMarketIDKey(),
                PersistentDataType.INTEGER,
                pricedMarket.getMarketID()
        );
        itemMeta.getPersistentDataContainer().set(
                GlobalTradeSystem.getPriceKey(),
                PersistentDataType.DOUBLE,
                pricedMarket.getGoingPrice()
        );
        CompletableFuture<ItemStack> finishedItemStack = new CompletableFuture<>();
        if (playerShopOverhaul.hasPlaceholderAPI()) {
            String loadingDisplayName = matchPlaceholders.matcher(name).replaceAll("Loading...");
            List<String> loadingLore = lore.stream().map((line) -> matchPlaceholders.matcher(line).replaceAll("Loading...")).toList();
            ItemMeta finalItemMeta = itemMeta;
            offlinePlayerCache.getOfflinePlayer(pricedMarket.getGoingPriceSeller()).thenAccept((offlinePlayer -> {
                Bukkit.getScheduler().runTask(playerShopOverhaul, () -> {
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

    public void showTradeSystemView(Player player) {
        String title = LegacyComponentSerializer.legacyAmpersand().serialize(MiniMessage.miniMessage().deserialize(configHandler.getGuiTitle()));
        ChestInterface<GTSData> chestInterface = new ChestInterface<>(title, 5, new GTSData("", 0));
        GTSData data = chestInterface.getInteractions().getData();
        ItemStack search = configHandler.getSearch();
        ItemStack myListings = configHandler.getListings();
        ItemStack forwardArrow = configHandler.getForwardArrow();
        ItemStack backArrow = configHandler.getBackArrow();
        ItemStack refresh = configHandler.getRefresh();

        ItemStack border = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);

        chestInterface.calculateInventoryContents(
                new String[] {
                    "         ",
                    "         ",
                    "         ",
                    "         ",
                    "         ",
                    "L##BRF##S"
                },
                Map.of(
                    'B', backArrow,
                    'F', forwardArrow,
                    'L', myListings,
                    'S', search,
                    'R', refresh,
                    '#', border
                )
        );
        chestInterface.getInteractions().addItemSimpleInteraction(forwardArrow, (otherPlayer) -> {
            if (chestInterface.getInventory().getItem(10) != null) data.setPageNumber(data.getPageNumber() + 1);
            updateTradeSystemView(player, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(backArrow, (otherPlayer) -> {
            if (data.getPageNumber() > 0) data.setPageNumber(data.getPageNumber() - 1);
            updateTradeSystemView(player, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(myListings, (otherPlayer) -> showListings(otherPlayer, otherPlayer.getUniqueId(), configHandler.getServerID()));
        chestInterface.getInteractions().addItemSimpleInteraction(refresh, this::showTradeSystemView);
        chestInterface.getInteractions().addItemSimpleInteraction(search, (otherPlayer) -> {
            player.closeInventory();
            foliaHacks.getScheduling().entitySpecificScheduler(otherPlayer)
                    .runDelayed(() -> {
                        textInterface.getInput(otherPlayer, (result) -> {
                            data.setCurrentSearch(result);
                            foliaHacks
                                    .runASAP(otherPlayer, () -> updateTradeSystemView(otherPlayer, chestInterface));
                        });
                    }, () -> {}, 2L);
        });
        chestInterface.getInteractions().addItemInteraction(
                (item -> item != null && item.hasItemMeta() &&
                        item.getItemMeta().getPersistentDataContainer().has(marketIDKey, PersistentDataType.INTEGER)
                ),
                (clickPlayer, item, clickType) -> {
                    int marketID = item.getItemMeta().getPersistentDataContainer().get(marketIDKey, PersistentDataType.INTEGER);
                    storage.getPricedMarket(marketID).thenAccept((market) -> {
                        foliaHacks.runASAP(clickPlayer, () -> {
                            itemPurchaseView.displayItemPurchaseView(clickPlayer, market, this::showTradeSystemView);
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
            foliaHacks.runASAP(player, () -> {
                assert markets.size() <= 21;
                for (int i = 0; i < 21; i++) { //markets.size() should be 21 in all circumstances as a page has 21 items on it
                    int index = i + 10 + (i / 7) * 2;
                    if (i < markets.size()) {
                        PricedMarket pricedMarket = markets.get(i);
                        MarketRepresentation representation = getMarketRepresentation(pricedMarket);
                        inventory.setItem(index, representation.instant());
                        representation.finished().thenAccept((finished) -> {
                            Bukkit.getScheduler().runTask(playerShopOverhaul, () -> {
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
        ItemStack forwardArrow = configHandler.getForwardArrow();
        ItemStack backArrow = configHandler.getBackArrow();
        ItemStack backButton = configHandler.getMenuBackButton();
        GTSData data = chestInterface.getInteractions().getData();
        chestInterface.getInteractions().addItemSimpleInteraction(forwardArrow, (otherPlayer) -> {
            if (chestInterface.getInventory().getItem(10) != null) data.setPageNumber(data.getPageNumber() + 1);
            displayListingsViewItems(otherPlayer, dataUUID, serverID, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(backArrow, (otherPlayer) -> {
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
        chestInterface.getInteractions().addItemSimpleInteraction(backButton, this::showTradeSystemView);
        chestInterface.getInteractions().addItemInteraction((item) -> item != null && item.hasItemMeta() &&
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
                foliaHacks.runASAP(player,
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

    public record MarketRepresentation(ItemStack instant, CompletableFuture<ItemStack> finished) {}
}
