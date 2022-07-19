package me.xemor.playershopoverhaul.commands;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        PlayerShopOverhaul instance = PlayerShopOverhaul.getInstance();
        instance.getBukkitAudiences().sender(sender).sendMessage(instance.getConfigHandler().getHelpMessage());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }

}
