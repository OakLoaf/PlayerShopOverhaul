package me.xemor.playershopoverhaul.itemserialization;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemSerialization {

    public static byte[] itemStackToBinary(ItemStack item) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(byteArrayOutputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ItemStack binaryToItemStack(byte[] itemData) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(itemData);
        try {
            BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(inputStream);
            return (ItemStack) bukkitObjectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}