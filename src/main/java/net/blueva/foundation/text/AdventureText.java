package net.blueva.foundation.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Adventure-specific text helpers. These methods are only usable on servers
 * that provide Adventure natively (e.g. Paper). Calling them on Spigot without
 * Adventure will throw {@link IllegalStateException}.
 *
 * <p>Prefer {@link Text#parse(String)} and {@link net.blueva.foundation.messages.Messages}
 * for code that must run on both Paper and Spigot.</p>
 */
public class AdventureText {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    protected AdventureText() {
    }

    public static Component component(String message) {
        if (isBlank(message)) {
            return Component.empty();
        }
        if (looksLegacy(message)) {
            return legacy(message);
        }
        return miniMessage(message);
    }

    public static Component miniMessage(String message) {
        if (isBlank(message)) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(message);
    }

    public static Component legacy(String message) {
        if (isBlank(message)) {
            return Component.empty();
        }
        return AMPERSAND_SERIALIZER.deserialize(message);
    }

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    private static boolean looksLegacy(String message) {
        return message.indexOf('&') >= 0 && message.indexOf('<') < 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
