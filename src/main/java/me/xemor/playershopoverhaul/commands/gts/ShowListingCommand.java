package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ShowListingCommand implements SubCommand {

    public ShowListingCommand() {

    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && sender.hasPermission("playershopoverhaul.gts.show")) {
            Player player = (Player) sender;
            PlayerShopOverhaul.getInstance().getGlobalTradeSystem().showTradeSystemView(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
