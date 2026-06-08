package net.blueva.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Adventure/MiniMessage text helpers. */
public class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    protected Text() {
    }

    /**
     * Parses text as MiniMessage by default.
     *
     * <p>Legacy {@code &} colors are accepted as an input-compatibility path,
     * but BlueAPI/Blueva output should be authored as MiniMessage.</p>
     */
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

    public static String legacySection(Component component) {
        if (component == null) {
            return "";
        }
        return SECTION_SERIALIZER.serialize(component);
    }

    public static String legacySection(String message) {
        return legacySection(component(message));
    }

    public static String plain(Component component) {
        if (component == null) {
            return "";
        }
        return PLAIN_SERIALIZER.serialize(component);
    }

    public static String plain(String message) {
        return plain(component(message));
    }

    public static String miniMessage(Component component) {
        if (component == null) {
            return "";
        }
        return MINI_MESSAGE.serialize(component);
    }

    public static MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static LegacyComponentSerializer legacyAmpersand() {
        return AMPERSAND_SERIALIZER;
    }

    public static LegacyComponentSerializer legacySection() {
        return SECTION_SERIALIZER;
    }

    public static PlainTextComponentSerializer plainText() {
        return PLAIN_SERIALIZER;
    }

    private static boolean looksLegacy(String message) {
        return message.indexOf('&') >= 0 && message.indexOf('<') < 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
