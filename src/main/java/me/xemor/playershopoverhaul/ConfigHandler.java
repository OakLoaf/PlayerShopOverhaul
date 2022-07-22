package me.xemor.playershopoverhaul;

import me.xemor.configurationdata.ItemStackData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigHandler {

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().useUnusualXRepeatedCharacterHexFormat().hexColors().build();
    private int serverID;
    private String databaseType;
    private String databaseHost;
    private String databaseName;
    private int databasePort;
    private String databaseUsername;
    private String databasePassword;
    private Component helpMessage;
    private String claimedMessage;
    private String listingName;
    private List<String> listingLore;
    private ItemStack forwardArrow;
    private ItemStack backArrow;
    private ItemStack refresh;
    private ItemStack listings;
    private ItemStack menuBackButton;
    private ItemStack search;


    public ConfigHandler() {
        reloadConfig();
    }

    public void reloadConfig() {
        PlayerShopOverhaul.getInstance().saveDefaultConfig();
        FileConfiguration config = PlayerShopOverhaul.getInstance().getConfig();
        PlayerShopOverhaul.getInstance().saveResource("language.yml", false);
        File languageFile = new File(PlayerShopOverhaul.getInstance().getDataFolder(), "language.yml");
        YamlConfiguration language = YamlConfiguration.loadConfiguration(languageFile);
        this.serverID = config.getInt("serverID");
        this.databaseType = config.getString("database.type", "SQLite");
        this.databaseHost = config.getString("database.host", "");
        this.databaseName = config.getString("database.name", "");
        this.databasePort = config.getInt("database.port", 3306);
        this.databaseUsername = config.getString("database.username", "");
        this.databasePassword = config.getString("database.password", "");
        this.helpMessage = MiniMessage.miniMessage().deserialize(language.getStringList("help").stream().reduce("", (str1, str2) -> str1 + "\n" + str2));
        this.listingName = language.getString("listing.name", "<r><b><name>");
        this.listingLore = language.getStringList("listing.lore");
        this.claimedMessage = language.getString("claim.claimed", "<gray>You claimed <money> dollars!");
        this.forwardArrow = new ItemStackData(language.getConfigurationSection("GUI.forwardArrow")).getItem();
        this.backArrow = new ItemStackData(language.getConfigurationSection("GUI.backArrow")).getItem();
        this.refresh = new ItemStackData(language.getConfigurationSection("GUI.refresh")).getItem();
        this.listings = new ItemStackData(language.getConfigurationSection("GUI.listings")).getItem();
        this.menuBackButton = new ItemStackData(language.getConfigurationSection("GUI.menuBackButton")).getItem();
        this.search = new ItemStackData(language.getConfigurationSection("GUI.search")).getItem();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public int getServerID() { return serverID; }

    public Component getHelpMessage() { return helpMessage; }

    public Component getClaimedMessage(double money) { return MiniMessage.miniMessage().deserialize(claimedMessage, Placeholder.unparsed("money", String.valueOf(money))); }

    public String getListingName(String name) { return legacySerializer.serialize(MiniMessage.miniMessage().deserialize(listingName, Placeholder.unparsed("name", String.valueOf(name)))); }

    public List<String> getListingLore(double price, int stock) { return listingLore.stream()
            .map((str) -> legacySerializer.serialize(MiniMessage.miniMessage().deserialize(str, Placeholder.unparsed("price", String.valueOf(price)), Placeholder.unparsed("stock", String.valueOf(stock)))))
            .collect(Collectors.toList());
    }

    public ItemStack getForwardArrow() {
        return forwardArrow;
    }

    public ItemStack getBackArrow() {
        return backArrow;
    }

    public ItemStack getRefresh() {
        return refresh;
    }

    public ItemStack getListings() {
        return listings;
    }

    public ItemStack getMenuBackButton() {
        return menuBackButton;
    }

    public ItemStack getSearch() {
        return search;
    }
}
