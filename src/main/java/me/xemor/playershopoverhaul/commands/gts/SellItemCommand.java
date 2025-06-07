package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.commands.SubCommand;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.storage.ItemTooLargeException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SellItemCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player && sender.hasPermission("playershopoverhaul.gts.sell")) {
            if (!PlayerShopOverhaul.getInstance().isGtsEnabled()) {
                sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getGtsDisabledMessage());
                return;
            }

            if (args.length == 2) {
                ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                if (itemInMainHand.getType() != Material.AIR) {
                    double pricePer = Double.parseDouble(args[1]) / itemInMainHand.getAmount();
                    sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getSoldMessage(pricePer));
                    try {
                        PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage().registerListing(
                                player.getUniqueId(),
                                itemInMainHand.clone(),
                                itemInMainHand.getAmount(),
                                pricePer
                        );
                    } catch (ItemTooLargeException e) {
                        sender.sendMessage(ChatColor.RED + "That item is too large to be stored in the global trade system!");
                        return;
                    }
                    itemInMainHand.setAmount(0);
                }
            }
        } else {
            sender.sendMessage("You cannot run this command!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> tab = new ArrayList<>();
        if (args.length == 2) {
            for (int i = 0; i < 10; i++) {
                tab.add(args[1] + i);
            }
        }
        return tab;
    }
}
