package net.blueva.foundation.worlds;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * World-related helpers that smooth over Bukkit API differences between
 * server versions, so plugins can call them without version guards.
 */
public class Worlds {

    private static volatile Method bukkitGetMinHeight; // World#getMinHeight (1.17+)
    private static volatile boolean minHeightResolved;

    /**
     * Returns the minimum build height of the world on any server version.
     * <p>Uses {@code World#getMinHeight()} when present (1.17+) and returns
     * {@code 0} on older servers, where the world always starts at y=0.</p>
     *
     * @param world the world (may be {@code null})
     * @return minimum build height, or {@code 0} on legacy servers
     */
    public static int getMinHeight(World world) {
        if (world == null) {
            return 0;
        }
        if (!minHeightResolved) {
            resolveMinHeight();
        }
        if (bukkitGetMinHeight != null) {
            try {
                Object value = bukkitGetMinHeight.invoke(world);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } catch (Throwable ignored) {
                // fall through to the legacy default
            }
        }
        return 0;
    }

    private static void resolveMinHeight() {
        synchronized (Worlds.class) {
            if (minHeightResolved) {
                return;
            }
            bukkitGetMinHeight = Reflection.method(World.class, "getMinHeight");
            minHeightResolved = true;
        }
    }
}
