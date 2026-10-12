package net.blueva.foundation.menus;

import java.util.Locale;

/**
 * Whether runtime entries occupy one view or are split into pages.
 * Both layouts accept declared buttons and entries supplied at open time.
 */
public enum MenuLayout {

    /** Declared buttons and runtime entries in a single view, without navigation. */
    STATIC,

    /**
     * The declared buttons plus a page of entries supplied at open time, with
     * previous/next navigation.
     */
    PAGINATED;

    /**
     * @param value a name from a config, may be {@code null}
     * @return the matching layout, or {@link #STATIC}
     */
    public static MenuLayout parse(String value) {
        if (value == null) {
            return STATIC;
        }
        String name = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        // Older spellings for the same thing.
        if ("LIST".equals(name) || "GRID".equals(name) || "DYNAMIC".equals(name)) {
            return PAGINATED;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return STATIC;
        }
    }
}
