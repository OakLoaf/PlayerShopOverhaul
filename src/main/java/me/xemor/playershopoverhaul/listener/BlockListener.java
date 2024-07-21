package me.xemor.playershopoverhaul.listener;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockListener implements Listener {

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();

        Block displayBlock;
        if (Tag.WALL_SIGNS.isTagged(material) && event.getSide().equals(Side.FRONT)) {
            WallSign sign = (WallSign) block.getBlockData();
            displayBlock = block.getRelative(sign.getFacing().getOppositeFace()).getRelative(BlockFace.UP);
        } else if (Tag.ALL_HANGING_SIGNS.isTagged(material)) {
            displayBlock = block.getRelative(BlockFace.DOWN);
        } else {
            return;
        }

        if (displayBlock.getType().isSolid()) {
            return;
        }

        block.getWorld().spawn(
            displayBlock.getLocation().add(0.5, 0.3, 0.5),
            Item.class,
            CreatureSpawnEvent.SpawnReason.CUSTOM,
            (item) -> {
                // TODO: Stop from being moved by blocks
                item.setItemStack(new ItemStack(Material.STICK));
//                item.setCanPlayerPickup(false);
                item.setCanMobPickup(false);
                item.setGravity(false);
                item.setVelocity(new Vector());
                item.setUnlimitedLifetime(true);
                item.setInvulnerable(true);
            });
    }
}
