package me.xemor.playershopoverhaul.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.xemor.configurationdata.ConfigurationData;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ConfigHandler {

    private PlayerShopOverhaul playerShopOverhaul;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().useUnusualXRepeatedCharacterHexFormat().hexColors().build();
    private int serverID;
    private double upperPriceMultiplier;
    private String databaseType;
    private String databaseHost;
    private String databaseName;
    private int databasePort;
    private String databaseUsername;
    private String databasePassword;
    private List<String> gtsCommandAliases;
    private LanguageConfig languageConfig;

    @Inject
    public ConfigHandler(PlayerShopOverhaul playerShopOverhaul) {
        this.playerShopOverhaul = playerShopOverhaul;
        reloadConfig();
    }

    public void reloadConfig() {
        playerShopOverhaul.saveDefaultConfig();
        FileConfiguration config = playerShopOverhaul.getConfig();
        playerShopOverhaul.saveResource("language.yml", false);
        File languageFile = new File(playerShopOverhaul.getDataFolder(), "language.yml");
        this.serverID = config.getInt("serverID");
        this.upperPriceMultiplier = config.getDouble("upperPriceMultiplier", 1.1);
        this.databaseType = config.getString("database.type", "SQLite");
        this.databaseHost = config.getString("database.host", "");
        this.databaseName = config.getString("database.name", "");
        this.databasePort = config.getInt("database.port", 3306);
        this.databaseUsername = config.getString("database.username", "");
        this.databasePassword = config.getString("database.password", "");
        this.gtsCommandAliases = config.getStringList("gts-command-aliases");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper = ConfigurationData.setupObjectMapperForConfigurationData(objectMapper);
        try {
            languageConfig = objectMapper.readValue(languageFile, LanguageConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public List<String> getGtsCommandAliases() {
        return gtsCommandAliases;
    }

    public double getUpperPriceMultiplier() {
        return upperPriceMultiplier;
    }

    public int getServerID() { return serverID; }

    public LanguageConfig getLanguageConfig() {
        return languageConfig;
    }

    public Component getHelpMessage() { return MiniMessage.miniMessage().deserialize(String.join("\n", languageConfig.getHelp())); }

    public Component getGtsDisabledMessage() {
        return MiniMessage.miniMessage().deserialize(languageConfig.getGtsDisabled());
    }

    public Component getClaimedMessage(double money) { return MiniMessage.miniMessage().deserialize(languageConfig.getClaim().getClaimed(), Placeholder.unparsed("money", String.valueOf(money))); }

    public String getListingName(String name) {
        return legacySerializer.serialize(MiniMessage.miniMessage().deserialize(languageConfig.getListing().getName(), Placeholder.unparsed("name", String.valueOf(name))));
    }

    public List<String> getListingLore(double price, int stock) {
        return languageConfig.getListing().getLore().stream()
            .map((str) -> legacySerializer.serialize(MiniMessage.miniMessage().deserialize(
                    str,
                    Placeholder.unparsed("price", "%.2f".formatted(price)),
                    Placeholder.unparsed("stock", String.valueOf(stock)))))
            .collect(Collectors.toList());
    }

    public String getGuiTitle() {
        return languageConfig.getGTSView().getTitle();
    }

    public Component getSoldMessage(double pricePer) {
        return MiniMessage.miniMessage().deserialize(languageConfig.getSell().getSoldMessage(), Placeholder.unparsed("priceper", String.valueOf(pricePer)));
    }

    public ItemStack getForwardArrow() {
        return languageConfig.getGTSView().getForwardArrow();
    }

    public ItemStack getBackArrow() {
        return languageConfig.getGTSView().getBackArrow();
    }

    public ItemStack getRefresh() {
        return languageConfig.getGTSView().getRefresh();
    }

    public ItemStack getListings() {
        return languageConfig.getGTSView().getListings();
    }

    public ItemStack getMenuBackButton() {
        return languageConfig.getGTSView().getMenuBackButton();
    }

    public ItemStack getSearch() {
        return languageConfig.getGTSView().getSearch();
    }
}
