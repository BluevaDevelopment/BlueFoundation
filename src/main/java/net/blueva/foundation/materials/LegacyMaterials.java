package net.blueva.foundation.materials;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps modern (1.13+) material names to a usable {@link ItemStack} on any
 * server version.
 *
 * <p>On 1.13+ servers the modern constant is resolved directly via
 * {@link Materials#match(String...)}. On legacy servers (&le;1.12) the modern
 * name is translated to the equivalent legacy {@link Material} plus its data
 * value (wool / stained glass / clay / dye colour families, and renamed items
 * such as {@code PLAYER_HEAD} &rarr; {@code SKULL_ITEM:3}).</p>
 *
 * <p>This keeps coloured GUI icons working across the whole 1.8.8&rarr;modern
 * range without every plugin carrying its own material table. Names that have
 * no reasonable legacy equivalent fall back to {@code BARRIER} so the slot is
 * visibly "missing" instead of silently empty.</p>
 */
public final class LegacyMaterials {

    /** Wool / stained glass / clay data values by colour name (1.8 ordering). */
    private static final Map<String, Integer> COLOR_DATA = new HashMap<>();
    /** INK_SACK data values by colour name (dyes are inverted vs wool). */
    private static final Map<String, Integer> DYE_DATA = new HashMap<>();
    /** Simple modern &rarr; legacy renames, optionally encoded as {@code NAME:data}. */
    private static final Map<String, String> RENAMES = new HashMap<>();

    static {
        // Colour order matches legacy wool/glass/clay durability values.
        String[] colors = {
                "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
                "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
                "BROWN", "GREEN", "RED", "BLACK"
        };
        for (int i = 0; i < colors.length; i++) {
            COLOR_DATA.put(colors[i], i);
        }
        // 1.8 also accepts SILVER for light-gray wool/glass/clay.
        COLOR_DATA.put("SILVER", 8);

        // INK_SACK durability values.
        String[] dyes = {
                "BLACK", "RED", "GREEN", "BROWN", "BLUE", "PURPLE", "CYAN",
                "LIGHT_GRAY", "GRAY", "PINK", "LIME", "YELLOW", "LIGHT_BLUE",
                "MAGENTA", "ORANGE", "WHITE"
        };
        for (int i = 0; i < dyes.length; i++) {
            DYE_DATA.put(dyes[i], i);
        }
        DYE_DATA.put("SILVER", 7);

        // Plain renames (same item, different constant name on legacy).
        RENAMES.put("GRASS_BLOCK", "GRASS");
        RENAMES.put("OAK_SIGN", "SIGN");
        RENAMES.put("SIGN", "SIGN");
        RENAMES.put("COMMAND_BLOCK", "COMMAND");
        RENAMES.put("COMMAND_BLOCK_MINECART", "COMMAND_MINECART");
        RENAMES.put("REPEATER", "DIODE");
        RENAMES.put("COMPARATOR", "REDSTONE_COMPARATOR");
        RENAMES.put("IRON_BARS", "IRON_FENCE");
        RENAMES.put("LEAD", "LEASH");
        RENAMES.put("ENDER_EYE", "EYE_OF_ENDER");
        RENAMES.put("WRITABLE_BOOK", "BOOK_AND_QUILL");
        RENAMES.put("FILLED_MAP", "MAP");
        RENAMES.put("FIREWORK_ROCKET", "FIREWORK");
        RENAMES.put("FIREWORK_STAR", "FIREWORK_CHARGE");
        RENAMES.put("TIPPED_ARROW", "ARROW");
        RENAMES.put("NETHER_BRICK", "NETHER_BRICK_ITEM");

        // Renames that also carry a data value on legacy.
        RENAMES.put("PLAYER_HEAD", "SKULL_ITEM:3");
        RENAMES.put("ZOMBIE_HEAD", "SKULL_ITEM:2");
        RENAMES.put("CREEPER_HEAD", "SKULL_ITEM:4");
        RENAMES.put("SKELETON_SKULL", "SKULL_ITEM:0");
        RENAMES.put("WITHER_SKELETON_SKULL", "SKULL_ITEM:1");
        RENAMES.put("DRAGON_HEAD", "SKULL_ITEM:5");
    }

    private LegacyMaterials() {
    }

    /**
     * Resolves a modern material name to an {@link ItemStack} that is valid on
     * the running server version.
     *
     * @param name   modern material name (e.g. {@code LIME_WOOL}, {@code PLAYER_HEAD})
     * @param amount stack size (clamped to at least 1)
     * @return a version-valid ItemStack, or {@code null} if the name is blank
     */
    public static ItemStack resolve(String name, int amount) {
        if (name == null) {
            return null;
        }
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return null;
        }
        if (amount < 1) {
            amount = 1;
        }

        // 1. Direct match on the running server (modern on 1.13+, identical
        //    legacy names on older servers).
        Material direct = Materials.match(normalized);
        if (direct != null) {
            return new ItemStack(direct, amount);
        }

        // 2. Coloured families -> legacy base material + colour data value.
        ItemStack coloured = coloured(normalized, amount);
        if (coloured != null) {
            return coloured;
        }

        // 3. Simple renames (optionally with an encoded data value).
        String renamed = RENAMES.get(normalized);
        if (renamed != null) {
            return fromRename(renamed, amount);
        }

        // 4. Visible fallback so the slot is not silently empty.
        Material barrier = Materials.match("BARRIER");
        return barrier == null ? null : new ItemStack(barrier, amount);
    }

    /** Returns {@code true} when the name resolves to something on this server. */
    public static boolean isResolvable(String name) {
        return resolve(name, 1) != null;
    }

    private static ItemStack coloured(String name, int amount) {
        int split = name.indexOf('_');
        if (split <= 0) {
            return null;
        }
        String colour = name.substring(0, split);
        String base = name.substring(split + 1);
        Integer colourData = COLOR_DATA.get(colour);
        if (colourData == null) {
            return null;
        }

        switch (base) {
            case "WOOL":
                return legacy("WOOL", colourData, amount);
            case "CARPET":
                return legacy("CARPET", colourData, amount);
            case "STAINED_GLASS":
                return legacy("STAINED_GLASS", colourData, amount);
            case "STAINED_GLASS_PANE":
                return legacy("STAINED_GLASS_PANE", colourData, amount);
            case "TERRACOTTA":
                return legacy("STAINED_CLAY", colourData, amount);
            case "CONCRETE":
            case "CONCRETE_POWDER":
                // Concrete is 1.12+; approximate with same-colour wool on older servers.
                return legacy("WOOL", colourData, amount);
            case "BANNER":
                // Banner colour lives in block-entity data on legacy; the plain
                // banner item is the closest icon.
                return legacy("BANNER", 0, amount);
            case "DYE": {
                Integer dyeData = DYE_DATA.get(colour);
                return dyeData == null ? null : legacy("INK_SACK", dyeData, amount);
            }
            default:
                return null;
        }
    }

    private static ItemStack fromRename(String renamed, int amount) {
        String baseName = renamed;
        int data = 0;
        int colon = renamed.indexOf(':');
        if (colon != -1) {
            baseName = renamed.substring(0, colon);
            try {
                data = Integer.parseInt(renamed.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                data = 0;
            }
        }
        return legacy(baseName, data, amount);
    }

    private static ItemStack legacy(String legacyName, int data, int amount) {
        Material material = Materials.match(legacyName);
        if (material == null) {
            return null;
        }
        return new ItemStack(material, amount, (short) data);
    }

    private static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }
}
