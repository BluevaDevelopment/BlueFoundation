package net.blueva.foundation.text;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.LegacySerializer;
import net.blueva.foundation.text.serializer.PlainSerializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * Text provider backed by native Adventure support (Paper and friends).
 *
 * <p>Unlike the previous implementation, this provider does <strong>not</strong>
 * use {@code adventure-platform-bukkit}. On Paper, {@link CommandSender} and
 * {@link Player} already implement {@link Audience}, so we call Adventure APIs
 * directly.</p>
 */
final class AdventureTextProvider implements TextProvider {

    private final MiniMessage miniMessage;

    public AdventureTextProvider(JavaPlugin plugin) {
        this.miniMessage = MiniMessage.miniMessage();
    }

    void close() {
        // Nothing to close without adventure-platform-bukkit.
    }

    @Override
    public void send(CommandSender sender, String message) {
        if (sender == null) {
            return;
        }
        // Paper's CommandSender implements Audience at runtime even though the
        // Spigot 1.8.8 API we compile against does not expose it.
        ((Audience) (Object) sender).sendMessage(parse(message));
    }

    @Override
    public void broadcast(String message) {
        Component component = parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            ((Audience) (Object) player).sendMessage(component);
        }
        ((Audience) (Object) Bukkit.getConsoleSender()).sendMessage(component);
    }

    @Override
    public boolean actionBar(Player player, String message) {
        if (player == null) {
            return false;
        }
        ((Audience) (Object) player).sendActionBar(parse(message));
        return true;
    }

    @Override
    public boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return false;
        }
        Title.Times times = Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut));
        ((Audience) (Object) player).showTitle(Title.title(parse(title), parse(subtitle), times));
        return true;
    }

    @Override
    public void bossBar(Player player, String title, BossBarColor color, BossBarStyle style, float progress) {
        if (player == null) {
            return;
        }
        BossBar bossBar = BossBar.bossBar(
                parse(title),
                Math.max(0.0f, Math.min(1.0f, progress)),
                BossBar.Color.valueOf(color.name()),
                BossBar.Overlay.valueOf(style.name())
        );
        ((Audience) (Object) player).showBossBar(bossBar);
    }

    @Override
    public String legacySection(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        // Items and other Bukkit legacy-only APIs use BlueFoundation's own parser
        // so gradient+style combinations serialize consistently on all servers.
        BfComponent component = MiniMessageParser.parse(message);
        return LegacySerializer.serialize(component);
    }

    @Override
    public String plain(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        BfComponent component = MiniMessageParser.parse(message);
        return PlainSerializer.serialize(component);
    }

    Component parse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Component.empty();
        }
        if (looksLegacy(message)) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }
        return miniMessage.deserialize(message);
    }

    private static boolean looksLegacy(String message) {
        return message.indexOf('&') >= 0 && message.indexOf('<') < 0;
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }
}
