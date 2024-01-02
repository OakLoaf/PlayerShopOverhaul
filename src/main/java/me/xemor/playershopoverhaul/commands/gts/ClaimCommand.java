package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClaimCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player && sender.hasPermission("playershopoverhaul.gts.claim")) {
            if (!PlayerShopOverhaul.getInstance().isGtsEnabled()) {
                sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getGtsDisabledMessage());
                return;
            }

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
