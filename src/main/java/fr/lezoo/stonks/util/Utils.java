package fr.lezoo.stonks.util;

import fr.lezoo.stonks.Stonks;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;

import java.text.DecimalFormat;
import java.util.Objects;

public class Utils {
    public static DecimalFormat singleDigit = new DecimalFormat("0.#");
    public static DecimalFormat fourDigits = new DecimalFormat("0.####");

    /**
     * Transforms 'badly-formatted ENUM name' into 'BADLY_FORMATTED_ENUM_NAME'
     *
     * @param str String input
     * @return String formatted for enum fields
     */
    public static String enumName(String str) {
        return Objects.requireNonNull(str, "String cannot be null").toUpperCase().replace(" ", "_").replace("-", "_");
    }

    /**
     * Super useful to display enum names like DIAMOND_SWORD in chat
     *
     * @param input String with lower cases and spaces only
     * @return Same string with capital letters at the beginning of each word.
     */
    public static String caseOnWords(String input) {
        StringBuilder builder = new StringBuilder(input);
        boolean isLastSpace = true;
        for (int i = 0; i < builder.length(); i++) {
            char ch = builder.charAt(i);
            if (isLastSpace && ch >= 'a' && ch <= 'z') {
                builder.setCharAt(i, (char) (ch + ('A' - 'a')));
                isLastSpace = false;
            } else isLastSpace = ch == ' ';
        }
        return builder.toString();
    }

    public static NamespacedKey namespacedKey(String str) {
        return new NamespacedKey(Stonks.plugin, str);
    }

    /**
     * @param x        Number to truncate
     * @param decimals Amount of decimals
     * @return Double truncated to X decimals
     */
    public static double truncate(double x, int decimals) {
        double pow = Math.pow(10, decimals);
        return Math.floor(x * pow) / pow;
    }

    /**
     * @return The direction to follow to place the item frames
     *         on a stock board
     */
    public static BlockFace rotateAroundY(BlockFace blockFace) {
        switch (blockFace) {
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            default:
                throw new IllegalArgumentException("Could not match any direction");
        }
    }

    public static String formatRate(double growthRate) {
        return formatGain(growthRate) + "%";
    }

    public static String formatGain(double d) {
        if (d == 0)
            return ChatColor.WHITE + "0";

        DecimalFormat format = Stonks.plugin.configManager.stockPriceFormat;
        return d < 0 ? ChatColor.RED + format.format(d) : ChatColor.GREEN + "+" + format.format(d);
    }
}
