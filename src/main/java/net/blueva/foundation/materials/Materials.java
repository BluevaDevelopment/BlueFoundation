package net.blueva.foundation.materials;

import org.bukkit.Material;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Multi-version material helpers. */
public class Materials {

    protected Materials() {
    }

    public static Material match(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            Material material = matchOne(name);
            if (material != null) {
                return material;
            }
        }
        return null;
    }

    public static Material require(String... names) {
        Material material = match(names);
        if (material == null) {
            throw new IllegalArgumentException("Unsupported material names: " + Arrays.toString(names));
        }
        return material;
    }

    public static Material orDefault(Material fallback, String... names) {
        Material material = match(names);
        return material == null ? fallback : material;
    }

    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    private static Material matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }
        String normalized = name.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        try {
            Method method = Material.class.getMethod("matchMaterial", String.class, boolean.class);
            Object result = method.invoke(null, normalized, true);
            if (result instanceof Material) {
                return (Material) result;
            }
        } catch (Throwable ignored) {
        }

        try {
            Material material = Material.matchMaterial(normalized);
            if (material != null) {
                return material;
            }
        } catch (Throwable ignored) {
        }

        try {
            return Material.valueOf(normalized);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
