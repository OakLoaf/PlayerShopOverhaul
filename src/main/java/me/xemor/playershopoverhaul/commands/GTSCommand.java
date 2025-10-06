package me.xemor.playershopoverhaul.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.configuration.ConfigHandler;
import me.xemor.playershopoverhaul.storage.ItemTooLargeException;
import me.xemor.playershopoverhaul.storage.Storage;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.Suggest;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.concurrent.CompletableFuture;

@Singleton
public class GTSCommand implements OrphanCommand {

    @Inject
    private PlayerShopOverhaul playerShopOverhaul;
    @Inject
    private ConfigHandler configHandler;
    @Inject
    private GlobalTradeSystem globalTradeSystem;
    @Inject
    private Storage storage;

    @CommandPlaceholder
    public void onCommand(BukkitCommandActor actor) {
        if (!playerShopOverhaul.isGtsEnabled()) {
            actor.reply(configHandler.getGtsDisabledMessage());
        }
        if (actor.isPlayer()) {
            Player player = actor.asPlayer();
            show(player);
        }
    }

    @Subcommand("help")
    @CommandPermission("playershopoverhaul.gts.help")
    public void help(BukkitCommandActor actor) {
        if (!playerShopOverhaul.isGtsEnabled()) {
            actor.reply(configHandler.getGtsDisabledMessage());
            return;
        }

        actor.reply(configHandler.getHelpMessage());
    }

    @Subcommand("sell")
    @CommandPermission("playershopoverhaul.gts.sell")
    public void sell(Player player, @Suggest({"1", "5", "10", "50", "100", "500", "1000"}) double price) {
        if (!playerShopOverhaul.isGtsEnabled()) {
            player.sendMessage(configHandler.getGtsDisabledMessage());
            return;
        }

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        if (itemInMainHand.getType() != Material.AIR) {
            double pricePer = price / itemInMainHand.getAmount();
            player.sendMessage(configHandler.getSoldMessage(pricePer));
            try {
                storage.registerListing(
                        player.getUniqueId(),
                        itemInMainHand.clone(),
                        itemInMainHand.getAmount(),
                        pricePer
                );
            } catch (ItemTooLargeException e) {
                player.sendMessage(ChatColor.RED + "That item is too large to be stored in the global trade system!");
                return;
            }
            itemInMainHand.setAmount(0);
        }
    }

    @Subcommand("show")
    @CommandPermission("playershopoverhaul.gts.show")
    public void show(Player player) {
        if (!playerShopOverhaul.isGtsEnabled()) {
            player.sendMessage(configHandler.getGtsDisabledMessage());
            return;
        }

        globalTradeSystem.showTradeSystemView(player);
    }

    @Subcommand("show")
    @CommandPermission("playershopoverhaul.gts.show.others")
    public void show(Player player, OfflinePlayer other) {
        int serverID = configHandler.getServerID();
        globalTradeSystem.showListings(player, other.getUniqueId(), serverID);
    }

    @Subcommand("claim")
    @CommandPermission("playershopoverhaul.gts.claim")
    public void claim(Player player) {
        if (!playerShopOverhaul.isGtsEnabled()) {
            player.sendMessage(configHandler.getGtsDisabledMessage());
            return;
        }
        CompletableFuture<Double> moneyFuture = globalTradeSystem.claimPayment(player);
        moneyFuture.thenAccept((money) -> {
            player.sendMessage(configHandler.getClaimedMessage(money));
        });
    }


}
