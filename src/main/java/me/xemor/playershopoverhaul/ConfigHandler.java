package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.storage.SQLStorage;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigHandler {

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().useUnusualXRepeatedCharacterHexFormat().hexColors().build();
    private final FileConfiguration config;
    private final YamlConfiguration language;
    private int serverID;
    private Component helpMessage;
    private Component claimedMessage;
    private String listingName;
    private List<String> listingLore;


    public ConfigHandler() {
        PlayerShopOverhaul.getInstance().saveDefaultConfig();
        config = PlayerShopOverhaul.getInstance().getConfig();
        PlayerShopOverhaul.getInstance().saveResource("language.yml", false);
        File languageFile = new File(PlayerShopOverhaul.getInstance().getDataFolder(), "language.yml");
        language = YamlConfiguration.loadConfiguration(languageFile);
        this.serverID = config.getInt("serverID");
        this.helpMessage = MiniMessage.miniMessage().deserialize(language.getStringList("help").stream().reduce("", (str1, str2) -> str1 + "\n" + str2));
        this.listingName = language.getString("listing.name", "<r><b><name>");
        this.listingLore = language.getStringList("listing.lore");
        this.claimedMessage = MiniMessage.miniMessage().deserialize(language.getString("claim.claimed", "<gray>You claimed <money> dollars!"));
    }

    public String getDatabaseType() {
        return config.getString("database.type", "SQLite");
    }

    public String getDatabaseHost() {
        return config.getString("database.host", "");
    }

    public String getDatabaseName() {
        return config.getString("database.name", "");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseUsername() {
        return config.getString("database.username", "");
    }

    public String getDatabasePassword() {
        return config.getString("database.password", "");
    }

    public int getServerID() { return serverID; }

    public Component getHelpMessage() { return helpMessage; }

    public Component getClaimedMessage(double money) { return claimedMessage; }

    public String getListingName(String name) { return legacySerializer.serialize(MiniMessage.miniMessage().deserialize(listingName, Placeholder.unparsed("name", String.valueOf(name)))); }

    public List<String> getListingLore(double price, int stock) { return listingLore.stream()
            .map((str) -> legacySerializer.serialize(MiniMessage.miniMessage().deserialize(str, Placeholder.unparsed("price", String.valueOf(price)), Placeholder.unparsed("stock", String.valueOf(stock)))))
            .collect(Collectors.toList()); }

}
