package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("playershopoverhaul.gts.help")) {
            PlayerShopOverhaul instance = PlayerShopOverhaul.getInstance();
            if (!instance.isGtsEnabled()) {
                sender.sendMessage(instance.getConfigHandler().getGtsDisabledMessage());
                return;
            }

            sender.sendMessage(instance.getConfigHandler().getHelpMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }

}
