package me.xemor.playershopoverhaul.commands.pso;

import me.xemor.playershopoverhaul.commands.SubCommand;
import me.xemor.playershopoverhaul.commands.gts.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class PSOCommand implements CommandExecutor, TabCompleter {

    private final EnumMap<PSOCommandType, SubCommand> map = new EnumMap<>(PSOCommandType.class);

    public PSOCommand() {
        map.put(PSOCommandType.HELP, new HelpCommand());
        map.put(PSOCommandType.RELOAD, new ReloadCommand());
        map.put(PSOCommandType.TOGGLEPLUGIN, new TogglePluginCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PSOCommandType psoCommandType = null;
        if (args.length == 0) {
            psoCommandType = PSOCommandType.HELP;
        }
        if (args.length >= 1) {
            try {
                psoCommandType = PSOCommandType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                psoCommandType = PSOCommandType.HELP;
            }
        }
        map.get(psoCommandType).onCommand(sender, args);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (PSOCommandType type : PSOCommandType.values()) {
                if (type.name().contains(args[0].toUpperCase())) {
                    list.add(type.name().toLowerCase());
                }
            }
            return list;
        } else {
            try {
                PSOCommandType type = PSOCommandType.valueOf(args[0].toUpperCase());
                return map.get(type).onTabComplete(sender, args);
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }
    }
}
