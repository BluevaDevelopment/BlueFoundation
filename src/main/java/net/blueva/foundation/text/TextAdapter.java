package net.blueva.foundation.text;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridge between BlueFoundation's public text API and the runtime text backend.
 *
 * <p>On servers that provide Adventure natively (Paper 1.16.5+) the adapter
 * loads {@link AdventureTextProvider} and delegates to Adventure directly via
 * Paper's native {@code Audience} implementations. On other servers it falls
 * back to BlueFoundation's own MiniMessage parser and legacy Bukkit APIs.</p>
 */
public final class TextAdapter {

    private static TextProvider provider;
    private static JavaPlugin plugin;

    private TextAdapter() {
    }

    /**
     * Initializes the adapter. Safe to call multiple times.
     */
    public static void init(JavaPlugin plugin) {
        TextAdapter.plugin = plugin;
        boolean nativeAdventure = hasNativeAdventure();
        if (nativeAdventure) {
            try {
                Class<?> providerClass = Class.forName("net.blueva.foundation.text.AdventureTextProvider");
                provider = (TextProvider) providerClass.getConstructor(JavaPlugin.class).newInstance(plugin);
            } catch (Throwable e) {
                provider = new FallbackTextProvider();
            }
        } else {
            provider = new FallbackTextProvider();
        }
    }

    /**
     * Initializes the adapter without a plugin reference. This enables message
     * parsing/serialization utilities but not player delivery.
     */
    public static void init() {
        if (provider == null) {
            provider = new FallbackTextProvider();
        }
    }

    public static void close() {
        if (provider instanceof AdventureTextProvider) {
            ((AdventureTextProvider) provider).close();
        }
        provider = null;
        plugin = null;
    }

    private static TextProvider provider() {
        if (provider == null) {
            init();
        }
        return provider;
    }

    public static void send(CommandSender sender, String message) {
        provider().send(sender, message);
    }

    public static void broadcast(String message) {
        provider().broadcast(message);
    }

    public static boolean actionBar(Player player, String message) {
        return provider().actionBar(player, message);
    }

    public static boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        return provider().title(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void bossBar(Player player, String title, TextProvider.BossBarColor color, TextProvider.BossBarStyle style, float progress) {
        provider().bossBar(player, title, color, style, progress);
    }

    public static String legacySection(String message) {
        return provider().legacySection(message);
    }

    public static String plain(String message) {
        return provider().plain(message);
    }

    private static boolean hasNativeAdventure() {
        try {
            // Use the default class loader so Paper's bundled Adventure classes are visible
            // even when the plugin does not ship adventure-platform-bukkit itself.
            Class<?> audience = Class.forName("net.kyori.adventure.audience.Audience");
            Class<?> sender = Class.forName("org.bukkit.command.CommandSender");
            return audience.isAssignableFrom(sender);
        } catch (Throwable e) {
            return false;
        }
    }
}
