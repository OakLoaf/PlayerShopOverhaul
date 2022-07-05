package me.xemor.playershopoverhaul.commands;

import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.playershopoverhaul.Listing;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AddListingCommand implements SubCommand {

    GlobalTradeSystem tradeSystem = PlayerShopOverhaul.getInstance().getGlobalTradeSystem();

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                if (itemInMainHand.getType() != Material.AIR) {
                    double pricePer = Double.parseDouble(args[1]) / itemInMainHand.getAmount();
                    tradeSystem.getStorage().registerListing(player.getUniqueId(), itemInMainHand, itemInMainHand.getAmount(), pricePer);
                    itemInMainHand.setAmount(0);
                }
            }
        } else {
            sender.sendMessage("You cannot run this command!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
