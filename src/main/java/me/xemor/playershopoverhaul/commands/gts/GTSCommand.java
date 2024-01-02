package me.xemor.playershopoverhaul.commands.gts;

import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import me.xemor.playershopoverhaul.commands.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GTSCommand extends Command {

    private final EnumMap<GTSCommandType, SubCommand> map = new EnumMap<>(GTSCommandType.class);

    public GTSCommand(String name) {
        super(name);
        map.put(GTSCommandType.SELL, new SellItemCommand());
        map.put(GTSCommandType.SHOW, new ShowListingCommand());
        map.put(GTSCommandType.HELP, new HelpCommand());
        map.put(GTSCommandType.CLAIM, new ClaimCommand());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!PlayerShopOverhaul.getInstance().isGtsEnabled()) {
            sender.sendMessage(PlayerShopOverhaul.getInstance().getConfigHandler().getGtsDisabledMessage());
            return true;
        }

        GTSCommandType gtsCommandType = null;
        if (args.length == 0) {
            gtsCommandType = GTSCommandType.SHOW;
        }
        if (args.length >= 1) {
            try {
                gtsCommandType = GTSCommandType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                gtsCommandType = GTSCommandType.HELP;
            }
        }
        map.get(gtsCommandType).onCommand(sender, args);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (GTSCommandType type : GTSCommandType.values()) {
                if (type.name().contains(args[0].toUpperCase())) {
                    list.add(type.name().toLowerCase());
                }
            }
            return list;
        } else {
            try {
                GTSCommandType type = GTSCommandType.valueOf(args[0].toUpperCase());
                return map.get(type).onTabComplete(sender, args);
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }
    }
}
