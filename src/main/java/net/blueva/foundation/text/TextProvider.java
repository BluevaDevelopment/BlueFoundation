package net.blueva.foundation.text;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Internal provider used by {@link TextAdapter} to send and serialize text.
 * Two implementations exist: one backed by native Adventure audiences and one
 * that uses BlueFoundation's own parser/serializers.
 */
public interface TextProvider {

    void send(CommandSender sender, String message);

    void broadcast(String message);

    boolean actionBar(Player player, String message);

    boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void bossBar(Player player, String title, BossBarColor color, BossBarStyle style, float progress);

    String legacySection(String message);

    String plain(String message);

    enum BossBarColor {
        PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
    }

    enum BossBarStyle {
        SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20
    }
}
