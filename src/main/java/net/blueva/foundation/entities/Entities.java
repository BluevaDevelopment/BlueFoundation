package net.blueva.foundation.entities;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Multi-version entity type helpers. */
public class Entities {

    /**
     * Entity types renamed across Bukkit versions. Each group lists every known
     * name for the same entity type, most recent name first. Matching is
     * bidirectional: any name in a group resolves on any server version.
     */
    private static final String[][] RENAMED_GROUPS = {
            // Renamed in 1.20.5 / 1.21
            {"TNT", "PRIMED_TNT"},
            {"ITEM", "DROPPED_ITEM"},
            {"CHEST_MINECART", "MINECART_CHEST"},
            {"COMMAND_BLOCK_MINECART", "MINECART_COMMAND"},
            {"FURNACE_MINECART", "MINECART_FURNACE"},
            {"HOPPER_MINECART", "MINECART_HOPPER"},
            {"SPAWNER_MINECART", "MINECART_MOB_SPAWNER"},
            {"TNT_MINECART", "MINECART_TNT"},
            {"LEASH_KNOT", "LEASH_HITCH"},
            {"EYE_OF_ENDER", "ENDER_SIGNAL"},
            {"FIREWORK_ROCKET", "FIREWORK"},
            {"EXPERIENCE_BOTTLE", "THROWN_EXP_BOTTLE"},
            // Renamed in 1.20.2
            {"POTION", "SPLASH_POTION"},
            // Renamed in 1.16
            {"ZOMBIFIED_PIGLIN", "PIG_ZOMBIE"},
            // Renamed in 1.14
            {"MOOSHROOM", "MUSHROOM_COW"},
    };

    private static final Map<String, String[]> ALIASES = new HashMap<>();

    static {
        for (String[] group : RENAMED_GROUPS) {
            for (String name : group) {
                ALIASES.put(name, group);
            }
        }
    }

    protected Entities() {
    }

    public static EntityType match(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            EntityType type = matchOne(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public static EntityType require(String... names) {
        EntityType type = match(names);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported entity type names: " + Arrays.toString(names));
        }
        return type;
    }

    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    public static Entity spawn(Location location, String... names) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        EntityType type = match(names);
        if (type == null) {
            return null;
        }
        return location.getWorld().spawnEntity(location, type);
    }

    public static <T extends Entity> T spawn(Location location, Class<T> entityClass, String... names) {
        if (entityClass == null) {
            return null;
        }
        Entity entity = spawn(location, names);
        if (!entityClass.isInstance(entity)) {
            return null;
        }
        return entityClass.cast(entity);
    }

    private static EntityType matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }

        String normalized = normalize(name);
        for (String candidate : candidates(normalized)) {
            try {
                return EntityType.valueOf(candidate);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Set<String> candidates(String normalized) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);

        String[] aliases = ALIASES.get(normalized);
        if (aliases != null) {
            candidates.addAll(Arrays.asList(aliases));
        }
        return candidates;
    }

    private static String normalize(String name) {
        String normalized = name.trim();
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('.', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
