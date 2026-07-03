package net.blueva.foundation.messages;

import net.blueva.foundation.text.TextAdapter;
import net.blueva.foundation.text.TextProvider;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Player/console messaging helpers. Uses native Adventure audiences when the
 * server provides them, otherwise falls back to Bukkit's legacy APIs and
 * BlueFoundation's own MiniMessage parser.
 */
public class Messages {

    protected Messages() {
    }

    /**
     * Initializes the messaging system. On Paper this creates the Adventure
     * audience provider; on Spigot it prepares the fallback legacy path.
     */
    public static void init(JavaPlugin plugin) {
        TextAdapter.init(plugin);
    }

    public static void close() {
        TextAdapter.close();
    }

    public static void send(Player player, String message) {
        send((CommandSender) player, message);
    }

    public static void send(CommandSender sender, String message) {
        TextAdapter.send(sender, message);
    }

    public static void broadcast(String message) {
        TextAdapter.broadcast(message);
    }

    public static boolean actionBar(Player player, String message) {
        return TextAdapter.actionBar(player, message);
    }

    public static boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return TextAdapter.title(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Shows a boss bar to the player. On Adventure-backed servers the native
     * boss bar is used; on Spigot the Bukkit {@code BossBar} API is used if
     * available (1.9+), otherwise the title is sent as a chat message.
     */
    public static void bossBar(Player player, String title, BossBarColor color, BossBarStyle style, float progress) {
        TextAdapter.bossBar(player, title, TextProvider.BossBarColor.valueOf(color.name()), TextProvider.BossBarStyle.valueOf(style.name()), progress);
    }

    /**
     * Serializes MiniMessage/legacy input to a legacy section string for Bukkit
     * APIs that only accept strings (inventory titles, item meta, etc.).
     */
    public static String legacySection(String message) {
        return TextAdapter.legacySection(message);
    }

    /**
     * Returns the plain text content of a MiniMessage/legacy message.
     */
    public static String plain(String message) {
        return TextAdapter.plain(message);
    }

    public enum BossBarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }

    public enum BossBarStyle {
        SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    }
}
