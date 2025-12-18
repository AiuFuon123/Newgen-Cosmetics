package NewgenCosmetics.util;

import org.bukkit.ChatColor;

public final class Chat {
    private Chat() {}
    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
