package me.xemor.playershopoverhaul.commands.pso;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TogglePluginCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("playershopoverhaul.pso.toggleplugin")) {
            if (PlayerShopOverhaul.getInstance().isGtsEnabled()) {
                sender.sendMessage(ChatColor.RED + "Turning off /gts...");
                sender.sendMessage(ChatColor.RED + "This does NOT persist between restarts");
                PlayerShopOverhaul.getInstance().disableGts();
                sender.sendMessage(ChatColor.RED + "Turned /gts off");
            }
            else {
                sender.sendMessage(ChatColor.GREEN + "Turning on /gts...");
                PlayerShopOverhaul.getInstance().enableGts();
                sender.sendMessage(ChatColor.GREEN + "Turned /gts on");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
