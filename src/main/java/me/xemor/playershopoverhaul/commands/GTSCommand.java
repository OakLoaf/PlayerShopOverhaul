package me.xemor.playershopoverhaul.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GTSCommand implements CommandExecutor, TabExecutor {

    EnumMap<SubCommandType, SubCommand> map = new EnumMap<>(SubCommandType.class);

    public GTSCommand() {
        map.put(SubCommandType.SELL, new SellItemCommand());
        map.put(SubCommandType.SHOW, new ShowListingCommand());
        map.put(SubCommandType.HELP, new HelpCommand());
        map.put(SubCommandType.CLAIM, new ClaimCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        SubCommandType subCommandType = null;
        if (args.length == 0) {
            subCommandType = SubCommandType.SHOW;
        }
        if (args.length >= 1) {
            try {
                subCommandType = SubCommandType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                subCommandType = SubCommandType.HELP;
            }
        }
        map.get(subCommandType).onCommand(sender, args);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (SubCommandType type : SubCommandType.values()) {
                if (type.name().contains(args[0].toUpperCase())) {
                    list.add(type.name().toLowerCase());
                }
            }
            return list;
        } else {
            try {
                SubCommandType type = SubCommandType.valueOf(args[0].toUpperCase());
                return map.get(type).onTabComplete(sender, args);
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }
    }
}
