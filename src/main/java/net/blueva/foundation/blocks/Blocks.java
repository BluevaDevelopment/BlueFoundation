package net.blueva.foundation.blocks;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.block.Block;

import java.lang.reflect.Method;

/**
 * Block-related helpers that smooth over Bukkit API differences between
 * server versions, so plugins can call them without version guards.
 */
public class Blocks {

    private static volatile Method bukkitIsPassable; // Block#isPassable (1.13+)
    private static volatile boolean passableResolved;

    /**
     * Returns whether a block can be passed through (has no collision) on any
     * server version.
     * <p>Uses {@code Block#isPassable()} when present (1.13+) and falls back to
     * {@code !type.isSolid()} on older servers, which is a close approximation
     * for movement and ground detection.</p>
     *
     * @param block the block (may be {@code null}, treated as passable)
     * @return {@code true} if the block can be passed through
     */
    public static boolean isPassable(Block block) {
        if (block == null) {
            return true;
        }
        if (!passableResolved) {
            resolvePassable();
        }
        if (bukkitIsPassable != null) {
            try {
                Object value = bukkitIsPassable.invoke(block);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (Throwable ignored) {
                // fall through to the legacy approximation
            }
        }
        return !block.getType().isSolid();
    }

    private static void resolvePassable() {
        synchronized (Blocks.class) {
            if (passableResolved) {
                return;
            }
            bukkitIsPassable = Reflection.method(Block.class, "isPassable");
            passableResolved = true;
        }
    }
}
