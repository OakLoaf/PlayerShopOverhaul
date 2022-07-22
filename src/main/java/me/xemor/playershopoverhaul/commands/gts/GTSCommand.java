package me.xemor.playershopoverhaul.commands.gts;


import me.xemor.playershopoverhaul.commands.SubCommand;
import me.xemor.playershopoverhaul.commands.gts.GTSCommandType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GTSCommand implements CommandExecutor, TabExecutor {

    private final EnumMap<GTSCommandType, SubCommand> map = new EnumMap<>(GTSCommandType.class);

    public GTSCommand() {
        map.put(GTSCommandType.SELL, new SellItemCommand());
        map.put(GTSCommandType.SHOW, new ShowListingCommand());
        map.put(GTSCommandType.HELP, new HelpCommand());
        map.put(GTSCommandType.CLAIM, new ClaimCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
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
