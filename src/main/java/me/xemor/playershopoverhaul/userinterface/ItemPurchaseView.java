package me.xemor.playershopoverhaul.userinterface;

import com.google.inject.Inject;
import me.xemor.foliahacks.FoliaHacks;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.PricedMarket;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.configuration.LanguageConfig;
import me.xemor.playershopoverhaul.storage.ExceedsMaxPriceException;
import me.xemor.playershopoverhaul.storage.InsufficientStockException;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.userinterface.chestinterface.ChestInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemPurchaseView {

    private final PlayerShopOverhaul playerShopOverhaul;
    private final FoliaHacks foliaHacks;
    private final ConfigHandler configHandler;
    private final Storage storage;
    private static LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().useUnusualXRepeatedCharacterHexFormat().hexColors().build();

    @Inject
    public ItemPurchaseView(
            PlayerShopOverhaul playerShopOverhaul,
            FoliaHacks foliaHacks,
            ConfigHandler configHandler,
            Storage storage
    ) {
        this.playerShopOverhaul = playerShopOverhaul;
        this.foliaHacks = foliaHacks;
        this.configHandler = configHandler;
        this.storage = storage;
    }

    public void updateUI(Player player, ChestInterface<ItemPurchaseData> chestInterface) {
        ItemPurchaseData data = chestInterface.getInteractions().getData();
        chestInterface.getInventory().setItem(4, getListing(data, "Loading..."));
        storage.costToPurchaseAmountFromMarket(data.getPricedMarket().getMarketID(), data.getNumberToPurchase())
            .thenAccept((cost) -> {
                foliaHacks.runASAP(player, () -> {
                    ItemStack listing = getListing(data, "%.2f".formatted(cost));
                    ItemMeta itemMeta = listing.getItemMeta();
                    itemMeta.getPersistentDataContainer().set(new NamespacedKey(playerShopOverhaul, "totalprice"), PersistentDataType.DOUBLE, cost);
                    listing.setItemMeta(itemMeta);
                    chestInterface.getInventory().setItem(4, listing);
                });
            });
    }

    public void displayItemPurchaseView(Player clickPlayer, PricedMarket market, Consumer<Player> backButton) {
        ItemPurchaseData itemPurchaseData = new ItemPurchaseData(market);
        Component inventoryTitleComponent = MiniMessage.miniMessage().deserialize(
                configHandler.getLanguageConfig().getItemPurchaseView().getTitle(),
                Placeholder.unparsed("itemname", PricedMarket.getName(configHandler, market.getItem()))
        );
        String inventoryTitle = legacySerializer.serialize(inventoryTitleComponent);
        ChestInterface<ItemPurchaseData> chestInterface = new ChestInterface<>(inventoryTitle, 1, itemPurchaseData);
        LanguageConfig.ItemPurchaseViewConfig itemPurchaseView = configHandler.getLanguageConfig().getItemPurchaseView();
        Map<Character, ItemStack> guiItems = Map.of(
                '0', itemPurchaseView.getMenuBackButton(),
                '1', itemPurchaseView.getPurchase64Less(),
                '2', itemPurchaseView.getPurchase16Less(),
                '3', itemPurchaseView.getPurchase1Less(),
                '4', itemPurchaseView.getPurchase1More(),
                '5', itemPurchaseView.getPurchase16More(),
                '6', itemPurchaseView.getPurchase64More(),
                '7', itemPurchaseView.getPurchase192More()
        );
        chestInterface.calculateInventoryContents(
                new String[]{
                        "0123 4567"
                },
                guiItems
        );
        updateUI(clickPlayer, chestInterface);
        chestInterface.getInteractions().addItemInteraction((item) -> {
            Double price = item.getPersistentDataContainer().get(new NamespacedKey(playerShopOverhaul, "totalprice"), PersistentDataType.DOUBLE);
            return price != null;
        }, (player, item, clickType) -> {
            Double price = item.getPersistentDataContainer().get(new NamespacedKey(playerShopOverhaul, "totalprice"), PersistentDataType.DOUBLE);
            storage.purchaseFromMarket(clickPlayer.getUniqueId(), market.getMarketID(), itemPurchaseData.getNumberToPurchase(), price)
                    .thenAccept((response) -> {
                        if (response.transactionSuccess()) {
                            givePlayerItems(player, market.getItem(), itemPurchaseData.getNumberToPurchase());
                            foliaHacks.runASAP(clickPlayer, () -> clickPlayer.sendMessage(ChatColor.GREEN + String.format("It was bought successfully for %.2f! You now have %.2f.", response.amount, response.balance)));
                        }
                        else {
                            foliaHacks.runASAP(clickPlayer, () -> clickPlayer.sendMessage(ChatColor.RED + "You have insufficient funds!"));
                        }
                    }).exceptionally((throwable -> {
                        if (throwable instanceof InsufficientStockException e)
                            foliaHacks.runASAP(clickPlayer, () -> clickPlayer.sendMessage(ChatColor.RED + "There is insufficient stock!"));

                        else if (throwable instanceof ExceedsMaxPriceException e)
                            foliaHacks.runASAP(clickPlayer, () -> clickPlayer.sendMessage(ChatColor.RED + "This would have cost more than the advertised maximum price, try purchasing less at once!"));
                        return null;
                    }));
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('0'), backButton);
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('1'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(-64);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('2'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(-16);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('3'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(-1);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('4'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(1);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('5'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(16);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('6'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(64);
            updateUI(otherPlayer, chestInterface);
        });
        chestInterface.getInteractions().addItemSimpleInteraction(guiItems.get('7'), (otherPlayer) -> {
            chestInterface.getInteractions().getData().addNumberToPurchase(192);
            updateUI(otherPlayer, chestInterface);
        });
        clickPlayer.openInventory(chestInterface.getInventory());
    }

    public void givePlayerItems(Player player, ItemStack item, int amount) {
        int maxStackSize = item.getMaxStackSize();
        for (int amountToGive = amount; amountToGive > 0; amountToGive -= maxStackSize) {
            int stackSize = Math.min(amountToGive, maxStackSize);
            ItemStack toGive = item.clone();
            toGive.setAmount(stackSize);
            Map<Integer, ItemStack> items = player.getInventory().addItem(toGive);
            for (ItemStack leftover : items.values()) {
                player.getLocation().getWorld().dropItem(player.getLocation(), leftover);
            }
        }
    }

    public ItemStack getListing(ItemPurchaseData itemPurchaseData, String totalPrice) {
        PricedMarket pricedMarket = itemPurchaseData.getPricedMarket();
        ItemStack marketItem = pricedMarket.getItem();
        ItemStack item = new ItemStack(marketItem.getType(), 1);
        LanguageConfig.ItemPurchaseViewConfig itemPurchaseView = configHandler.getLanguageConfig().getItemPurchaseView();
        Component itemName = MiniMessage.miniMessage().deserialize(
                itemPurchaseView.getListing().getName(),
                Placeholder.unparsed("name", PricedMarket.getName(configHandler, marketItem))
        );
        List<Component> itemLore = itemPurchaseView.getListing().getLore().stream()
                .map((str) -> MiniMessage.miniMessage().deserialize(
                        str,
                        Placeholder.unparsed("totalprice", totalPrice),
                        Placeholder.unparsed("amounttopurchase", String.valueOf(itemPurchaseData.getNumberToPurchase())),
                        Placeholder.unparsed("stock", String.valueOf(itemPurchaseData.getPricedMarket().getStock()))
                ))
                .toList();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) itemMeta = Bukkit.getItemFactory().getItemMeta(item.getType());
        itemMeta.displayName(itemName);
        itemMeta.lore(itemLore);
        item.setItemMeta(itemMeta);
        return item;
    }

}
