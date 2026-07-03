package net.blueva.foundation.text.component;

import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Color representation for BlueFoundation's own message component tree.
 * Supports named Minecraft colors and RGB hex values, with automatic
 * down-sampling to legacy colors on servers older than 1.16.
 */
public final class BfColor {

    private static final Map<String, BfColor> NAMED;
    private static final Map<Integer, Character> LEGACY_BY_RGB;

    static {
        Map<String, BfColor> named = new HashMap<String, BfColor>();
        named.put("black", new BfColor("black", 0x000000));
        named.put("dark_blue", new BfColor("dark_blue", 0x0000AA));
        named.put("dark_green", new BfColor("dark_green", 0x00AA00));
        named.put("dark_aqua", new BfColor("dark_aqua", 0x00AAAA));
        named.put("dark_red", new BfColor("dark_red", 0xAA0000));
        named.put("dark_purple", new BfColor("dark_purple", 0xAA00AA));
        named.put("gold", new BfColor("gold", 0xFFAA00));
        named.put("gray", new BfColor("gray", 0xAAAAAA));
        named.put("grey", new BfColor("grey", 0xAAAAAA));
        named.put("dark_gray", new BfColor("dark_gray", 0x555555));
        named.put("dark_grey", new BfColor("dark_grey", 0x555555));
        named.put("blue", new BfColor("blue", 0x5555FF));
        named.put("green", new BfColor("green", 0x55FF55));
        named.put("aqua", new BfColor("aqua", 0x55FFFF));
        named.put("red", new BfColor("red", 0xFF5555));
        named.put("light_purple", new BfColor("light_purple", 0xFF55FF));
        named.put("yellow", new BfColor("yellow", 0xFFFF55));
        named.put("white", new BfColor("white", 0xFFFFFF));
        NAMED = Collections.unmodifiableMap(named);

        Map<Integer, Character> legacy = new HashMap<Integer, Character>();
        legacy.put(0x000000, '0');
        legacy.put(0x0000AA, '1');
        legacy.put(0x00AA00, '2');
        legacy.put(0x00AAAA, '3');
        legacy.put(0xAA0000, '4');
        legacy.put(0xAA00AA, '5');
        legacy.put(0xFFAA00, '6');
        legacy.put(0xAAAAAA, '7');
        legacy.put(0x555555, '8');
        legacy.put(0x5555FF, '9');
        legacy.put(0x55FF55, 'a');
        legacy.put(0x55FFFF, 'b');
        legacy.put(0xFF5555, 'c');
        legacy.put(0xFF55FF, 'd');
        legacy.put(0xFFFF55, 'e');
        legacy.put(0xFFFFFF, 'f');
        LEGACY_BY_RGB = Collections.unmodifiableMap(legacy);
    }

    private final String name;
    private final int rgb;

    private BfColor(String name, int rgb) {
        this.name = name;
        this.rgb = rgb;
    }

    public static BfColor named(String name) {
        return NAMED.get(name.toLowerCase());
    }

    public static BfColor hex(int rgb) {
        return new BfColor(null, rgb & 0xFFFFFF);
    }

    public static BfColor hex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() != 6) {
            return null;
        }
        try {
            return hex(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isNamed() {
        return name != null;
    }

    public String name() {
        return name;
    }

    public int rgb() {
        return rgb;
    }

    public int red() {
        return (rgb >> 16) & 0xFF;
    }

    public int green() {
        return (rgb >> 8) & 0xFF;
    }

    public int blue() {
        return rgb & 0xFF;
    }

    /**
     * Returns the legacy section code character for this color if it is a named
     * legacy color, otherwise the nearest legacy color for hex values.
     */
    public char legacyChar() {
        Character exact = LEGACY_BY_RGB.get(rgb);
        if (exact != null) {
            return exact;
        }
        return nearestLegacyChar();
    }

    /**
     * Serializes this color to a legacy section string. On 1.16+ hex colors are
     * emitted as {@code §x§R§R§G§G§B§B}; on older servers they are downsampled.
     */
    public String legacySection() {
        if (!supportsHex()) {
            return "\u00A7" + legacyChar();
        }
        if (name != null && LEGACY_BY_RGB.containsKey(rgb)) {
            return "\u00A7" + LEGACY_BY_RGB.get(rgb);
        }
        String hex = String.format("%06X", rgb);
        StringBuilder builder = new StringBuilder("\u00A7x");
        for (int i = 0; i < hex.length(); i++) {
            builder.append('\u00A7').append(hex.charAt(i));
        }
        return builder.toString();
    }

    private char nearestLegacyChar() {
        int bestDistance = Integer.MAX_VALUE;
        char best = 'f';
        for (Map.Entry<Integer, Character> entry : LEGACY_BY_RGB.entrySet()) {
            int distance = distanceSquared(rgb, entry.getKey());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getValue();
            }
        }
        return best;
    }

    private static int distanceSquared(int a, int b) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return (ar - br) * (ar - br) + (ag - bg) * (ag - bg) + (ab - bb) * (ab - bb);
    }

    private static boolean supportsHex() {
        try {
            String version = Bukkit.getBukkitVersion();
            if (version == null || version.isEmpty()) {
                return true; // assume modern when Bukkit is unavailable (tests)
            }
            String[] parts = version.split("[-.]");
            int major = parseInt(parts.length > 0 ? parts[0] : "0");
            int minor = parseInt(parts.length > 1 ? parts[1] : "0");
            return major > 1 || (major == 1 && minor >= 16);
        } catch (Throwable ignored) {
            return true; // assume modern when version detection fails
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BfColor)) return false;
        BfColor other = (BfColor) o;
        return rgb == other.rgb;
    }

    @Override
    public int hashCode() {
        return rgb;
    }
}
