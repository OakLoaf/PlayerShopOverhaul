package me.xemor.playershopoverhaul.commands.pso;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("playershopoverhaul.pso.reload")) {
            sender.sendMessage(ChatColor.GREEN + "Reloading...");
            PlayerShopOverhaul.getInstance().reload();
            sender.sendMessage(ChatColor.GREEN + "Reloaded");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
