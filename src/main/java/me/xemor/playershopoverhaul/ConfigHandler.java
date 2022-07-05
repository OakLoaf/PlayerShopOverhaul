package me.xemor.playershopoverhaul;

import me.xemor.playershopoverhaul.storage.SQLStorage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigHandler {

    private final FileConfiguration config;
    private final YamlConfiguration language;
    private int serverID;
    private String helpMessage;
    private String listingName;
    private List<String> listingLore;


    public ConfigHandler() {
        PlayerShopOverhaul.getInstance().saveDefaultConfig();
        config = PlayerShopOverhaul.getInstance().getConfig();
        PlayerShopOverhaul.getInstance().saveResource("language.yml", false);
        File languageFile = new File(PlayerShopOverhaul.getInstance().getDataFolder(), "language.yml");
        language = YamlConfiguration.loadConfiguration(languageFile);
        this.serverID = config.getInt("serverID");
        this.helpMessage = language.getStringList("help").stream().map((str) -> ChatColor.translateAlternateColorCodes('&', str)).reduce("", (str1, str2) -> str1 + "\n" + str2);
        this.listingName = ChatColor.translateAlternateColorCodes('&', language.getString("listing.name", "&r&l%type%"));
        this.listingLore = language.getStringList("listing.lore").stream().map((str) -> ChatColor.translateAlternateColorCodes('&', str)).collect(Collectors.toList());
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

    public String getHelpMessage() { return helpMessage; }

    public String getListingName() { return listingName; }

    public List<String> getListingLore() { return new ArrayList<>(listingLore); }

}
