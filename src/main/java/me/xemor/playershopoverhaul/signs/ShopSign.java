package me.xemor.playershopoverhaul.signs;

import org.bukkit.*;
import org.bukkit.block.HangingSign;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

public class ShopSign {

    public static boolean isValidShopSign(Sign sign) {
        Side[] sides = sign instanceof HangingSign ? Side.values() : new Side[]{Side.FRONT};

        boolean hasShopIntent = false;
        for (Side side : sides) {
            SignSide signSide = sign.getSide(side);
            String[] lines = signSide.getLines();

            if (lines[0].equalsIgnoreCase("[shop]")) {
                hasShopIntent = true;

                Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(lines[1]));
                if (material == null) {
                    return false;
                }
            }
        }

        return hasShopIntent;
    }

    public static boolean isValidShopSign(SignSide signSide) {
        String[] lines = signSide.getLines();

        if (!lines[0].equalsIgnoreCase("[shop]")) {
            return false;
        }

        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(lines[1]));
        return material != null;
    }

    public static void makeShopSignFriendly(Sign sign) {
        Side[] sides = sign instanceof HangingSign ? Side.values() : new Side[]{Side.FRONT};
        for (Side side : sides) {
            SignSide signSide = sign.getSide(side);
            String[] lines = signSide.getLines();

            signSide.setLine(0, "[Shop]");
            signSide.setLine(1, makeFriendly(lines[1]));

            OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(lines[2]);
            String playerName = player.getName();
            if (playerName != null) {
                signSide.setLine(2, playerName);
            }
        }
    }

    private static String makeFriendly(String string) {
        String[] words = string.split("\\s");

        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(Character.toTitleCase(word.charAt(0)))
                .append(word.substring(1))
                .append(" ");
        }

        return result.toString().trim();
    }
}
