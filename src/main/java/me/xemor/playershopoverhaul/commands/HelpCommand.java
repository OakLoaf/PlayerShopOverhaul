package me.xemor.playershopoverhaul.commands;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getHelpMessage());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }

}
