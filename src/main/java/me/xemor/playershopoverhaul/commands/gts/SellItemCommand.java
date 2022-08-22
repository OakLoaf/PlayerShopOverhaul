package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.ConfigHandler;
import me.xemor.playershopoverhaul.commands.SubCommand;
import me.xemor.playershopoverhaul.userinterface.GlobalTradeSystem;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SellItemCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("playershopoverhaul.gts.sell")) {
            Player player = (Player) sender;
            if (args.length == 2) {
                ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
                if (itemInMainHand.getType() != Material.AIR) {
                    double pricePer = Double.parseDouble(args[1]) / itemInMainHand.getAmount();
                    PlayerShopOverhaul.getInstance().getBukkitAudiences().sender(sender)
                            .sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getSoldMessage(pricePer));
                    PlayerShopOverhaul.getInstance().getGlobalTradeSystem().getStorage().registerListing(player.getUniqueId(), itemInMainHand, itemInMainHand.getAmount(), pricePer);
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
