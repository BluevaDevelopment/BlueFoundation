package net.blueva.foundation.text;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;

/**
 * BlueFoundation text utilities. This class does not depend on Adventure at
 * runtime; it uses Adventure natively when the server provides it and falls
 * back to BlueFoundation's own MiniMessage parser otherwise.
 */
public class Text {

    protected Text() {
    }

    /**
     * Parses a MiniMessage/legacy string into BlueFoundation's own component
     * tree. Always available, even when Adventure is not present at runtime.
     */
    public static BfComponent parse(String message) {
        if (isBlank(message)) {
            return BfComponent.empty();
        }
        return MiniMessageParser.parse(message);
    }

    /**
     * Serializes the input to a legacy section string ({@code §} codes).
     * On 1.16+ hex colors use {@code §x§R§R§G§G§B§B}; older servers get the
     * nearest legacy color.
     */
    public static String legacySection(String message) {
        TextAdapter.init();
        return TextAdapter.legacySection(message);
    }

    /**
     * Returns the plain text content of the message, stripping all formatting.
     */
    public static String plain(String message) {
        TextAdapter.init();
        return TextAdapter.plain(message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
