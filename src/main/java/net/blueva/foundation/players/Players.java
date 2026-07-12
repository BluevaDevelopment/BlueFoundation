package net.blueva.foundation.players;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Player-related helpers that smooth over Bukkit API differences between
 * server versions, so plugins can call them without version guards.
 */
public class Players {

    private static volatile Method bukkitGetPing;   // Player#getPing (1.12+)
    private static volatile Field nmsPingField;     // EntityPlayer#ping (legacy NMS)
    private static volatile boolean pingResolved;

    /**
     * Returns the player's latency in milliseconds on any server version.
     * <p>Uses the Bukkit {@code Player#getPing()} method when present
     * (1.12+) and falls back to the legacy NMS {@code EntityPlayer#ping}
     * field on older servers. Returns {@code 0} when the ping cannot be
     * determined.</p>
     *
     * @param player the player (may be {@code null})
     * @return latency in milliseconds, or {@code 0} if unknown
     */
    public static int getPing(Player player) {
        if (player == null) {
            return 0;
        }
        if (!pingResolved) {
            resolvePing();
        }
        if (bukkitGetPing != null) {
            try {
                Object value = bukkitGetPing.invoke(player);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            } catch (Throwable ignored) {
                // fall through to the legacy NMS path
            }
        }
        if (nmsPingField != null) {
            try {
                Object handle = Reflection.getHandle(player);
                if (handle != null) {
                    return nmsPingField.getInt(handle);
                }
            } catch (Throwable ignored) {
                // fall through to the default
            }
        }
        return 0;
    }

    private static void resolvePing() {
        synchronized (Players.class) {
            if (pingResolved) {
                return;
            }
            bukkitGetPing = Reflection.method(Player.class, "getPing");
            if (bukkitGetPing == null) {
                nmsPingField = resolveLegacyPingField();
            }
            pingResolved = true;
        }
    }

    private static Field resolveLegacyPingField() {
        Class<?> entityPlayer = Reflection.nmsClass("EntityPlayer");
        if (entityPlayer == null) {
            return null;
        }
        Field field = Reflection.field(entityPlayer, "ping");
        if (field == null) {
            return null;
        }
        return int.class.equals(field.getType()) ? field : null;
    }
}
