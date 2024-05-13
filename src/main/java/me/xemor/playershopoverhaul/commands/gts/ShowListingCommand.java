package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShowListingCommand implements SubCommand {

    public ShowListingCommand() {

    }
    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player && sender.hasPermission("playershopoverhaul.gts.show")) {
            if (!PlayerShopOverhaul.getInstance().isGtsEnabled()) {
                sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getGtsDisabledMessage());
                return;
            }

            if (args.length == 3 && sender.hasPermission("playershopoverhaul.gts.show.others")) {
                Player otherPlayer = Bukkit.getPlayer(args[1]);
                CompletableFuture<UUID> uuidFuture;
                if (otherPlayer == null) {
                    uuidFuture = PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage().getUUID(args[1]);
                }
                else {
                    uuidFuture = CompletableFuture.completedFuture(player.getUniqueId());
                }
                int serverID = Integer.parseInt(args[2]);
                uuidFuture.whenComplete((uuid, ignored) -> {
                    PlayerShopOverhaul.getInstance().getGlobalTradeSystem().showListings(player, uuid, serverID);
                });
            }
            else {
                PlayerShopOverhaul.getInstance().getGlobalTradeSystem().showTradeSystemView(player);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

}
