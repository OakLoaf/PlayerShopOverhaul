package me.xemor.playershopoverhaul.itemserialization;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Arrays;

/**
 * Long term this needs to be replaced with a special SerializeyItemStack in order to prevent the version from ever getting serialised
 * therefore not needing my dodgy workaround
 */
public class ItemSerialization {

    public static byte[] itemStackToBinary(ItemStack item) {
        return item.serializeAsBytes();
    }

    public static ItemStack binaryToItemStack(byte[] itemData) {
        return ItemStack.deserializeBytes(itemData);
    }

}