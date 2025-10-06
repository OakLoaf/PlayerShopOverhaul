package me.xemor.playershopoverhaul;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.xemor.foliahacks.FoliaHacks;
import me.xemor.playershopoverhaul.commands.GTSCommand;
import me.xemor.playershopoverhaul.commands.PSOCommand;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.CacheStorage;
import me.xemor.playershopoverhaul.storage.SQLStorage;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.playershopoverhaul.storage.fastofflineplayer.OfflinePlayerCache;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.playershopoverhaul.userinterface.ItemPurchaseView;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

public class PlayerShopOverhaulModule extends AbstractModule {

    private final PlayerShopOverhaul plugin;
    private Economy economy;

    public PlayerShopOverhaulModule(PlayerShopOverhaul plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    @Override
    protected void configure() {
        bind(PlayerShopOverhaul.class).toInstance(plugin);
        FoliaHacks foliaHacks = new FoliaHacks(plugin);
        bind(FoliaHacks.class).toInstance(foliaHacks);
        bind(GracefulScheduling.class).toInstance(foliaHacks.getScheduling());
        bind(Economy.class).toInstance(economy);
        bind(ConfigHandler.class).in(Singleton.class);
        bind(Storage.class).toProvider(new Provider<>() {
            @Inject
            private Provider<ConfigHandler> configHandlerProvider;
            @Inject
            private Provider<PlayerShopOverhaul> playerShopOverhaulProvider;
            @Inject
            private Provider<Economy> economyProvider;

            @Override
            public Storage get() {
                return new CacheStorage(new SQLStorage(playerShopOverhaulProvider.get(), configHandlerProvider.get(), economyProvider.get()));
            }
        }).in(Singleton.class);
        bind(GlobalTradeSystem.class).in(Singleton.class);
        bind(GTSCommand.class).in(Singleton.class);
        bind(PSOCommand.class).in(Singleton.class);
        bind(OfflinePlayerCache.class).in(Singleton.class);
        bind(ItemPurchaseView.class).in(Singleton.class);
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Failed to setup economy plugin");
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("Failed to setup economy plugin");
        }
        economy = rsp.getProvider();
    }

}
