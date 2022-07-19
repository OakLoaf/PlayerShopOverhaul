package me.xemor.playershopoverhaul.commands;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClaimCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            CompletableFuture<Double> moneyFuture = PlayerShopOverhaul.getInstance().getGlobalTradeSystem().claimPayment(player);
            moneyFuture.thenAccept((money) -> {
                PlayerShopOverhaul playerShopOverhaul = PlayerShopOverhaul.getInstance();
                playerShopOverhaul.getBukkitAudiences().sender(sender).sendMessage(playerShopOverhaul.getConfigHandler().getClaimedMessage(money));
            });
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }

}
