package me.xemor.playershopoverhaul.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.xemor.playershopoverhaul.PlayerShopOverhaul;
import org.bukkit.ChatColor;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Singleton
@Command({"playershopoverhaul", "pso"})
public class PSOCommand {

    @Inject
    private PlayerShopOverhaul playerShopOverhaul;

    @Subcommand("reload")
    @CommandPermission( "playershopoverhaul.pso.reload")
    public void reload(BukkitCommandActor actor) {
        actor.reply(ChatColor.GREEN + "Reloading...");
        playerShopOverhaul.reload();
        actor.reply(ChatColor.GREEN + "Reloaded");
    }

    @Subcommand("toggleplugin")
    @CommandPermission( "playershopoverhaul.pso.toggleplugin")
    public void togglePlugin(BukkitCommandActor actor) {
        if (playerShopOverhaul.isGtsEnabled()) {
            actor.reply(ChatColor.RED + "Turning off /gts...");
            actor.reply(ChatColor.RED + "This does NOT persist between restarts");
            playerShopOverhaul.disableGts();
            actor.reply(ChatColor.RED + "Turned /gts off");
        }
        else {
            actor.reply(ChatColor.GREEN + "Turning on /gts...");
            playerShopOverhaul.enableGts();
            actor.reply(ChatColor.GREEN + "Turned /gts on");
        }
    }
}
