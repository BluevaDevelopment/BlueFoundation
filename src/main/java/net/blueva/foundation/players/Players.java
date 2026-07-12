package net.blueva.foundation.players;

import net.blueva.foundation.npc.NPCs;
import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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

    // ------------------------------------------------------------------
    // Arm swing (Player#swingMainHand / swingOffHand were added in 1.14)
    // ------------------------------------------------------------------

    private static volatile Method bukkitSwingMainHand;
    private static volatile Method bukkitSwingOffHand;
    private static volatile boolean swingResolved;

    /**
     * Swings the player's main hand on any server version. On modern servers
     * this calls {@code Player#swingMainHand()}; on legacy servers it
     * broadcasts the vanilla arm-swing animation packet to nearby viewers.
     */
    public static void swingMainHand(Player player) {
        swingArm(player, true);
    }

    /**
     * Swings the player's off-hand on any server version. Off-hand does not
     * exist before 1.9, so on legacy servers this only broadcasts the
     * animation id (old clients ignore it).
     */
    public static void swingOffHand(Player player) {
        swingArm(player, false);
    }

    private static void swingArm(Player player, boolean mainHand) {
        if (player == null) {
            return;
        }
        if (!swingResolved) {
            resolveSwing();
        }
        Method bukkit = mainHand ? bukkitSwingMainHand : bukkitSwingOffHand;
        if (bukkit != null) {
            try {
                bukkit.invoke(player);
                return;
            } catch (Throwable ignored) {
                // fall through to the legacy packet broadcast
            }
        }
        Object handle = Reflection.getHandle(player);
        if (handle == null || player.getWorld() == null) {
            return;
        }
        int animationId = mainHand ? 0 : 3; // 0 = swing main arm, 3 = swing off-hand
        for (Player viewer : player.getWorld().getPlayers()) {
            NPCs.sendEntityAnimation(viewer, handle, animationId);
        }
    }

    private static void resolveSwing() {
        synchronized (Players.class) {
            if (swingResolved) {
                return;
            }
            bukkitSwingMainHand = Reflection.method(Player.class, "swingMainHand");
            bukkitSwingOffHand = Reflection.method(Player.class, "swingOffHand");
            swingResolved = true;
        }
    }

    // ------------------------------------------------------------------
    // Player visibility (hidePlayer/showPlayer Plugin form was added in 1.12.2)
    // ------------------------------------------------------------------

    private static volatile Method bukkitHidePlayer2; // hidePlayer(Plugin, Player)  (1.12.2+)
    private static volatile Method bukkitShowPlayer2; // showPlayer(Plugin, Player)  (1.12.2+)
    private static volatile Method bukkitHidePlayer1; // hidePlayer(Player)          (legacy)
    private static volatile Method bukkitShowPlayer1; // showPlayer(Player)          (legacy)
    private static volatile boolean visibilityResolved;

    /**
     * Hides {@code target} from {@code viewer} on any server version, using the
     * modern {@code hidePlayer(Plugin, Player)} when available and the legacy
     * single-argument form otherwise.
     */
    public static void hidePlayer(Player viewer, Plugin plugin, Player target) {
        setPlayerVisible(viewer, plugin, target, false);
    }

    /**
     * Shows {@code target} to {@code viewer} on any server version, using the
     * modern {@code showPlayer(Plugin, Player)} when available and the legacy
     * single-argument form otherwise.
     */
    public static void showPlayer(Player viewer, Plugin plugin, Player target) {
        setPlayerVisible(viewer, plugin, target, true);
    }

    private static void setPlayerVisible(Player viewer, Plugin plugin, Player target, boolean visible) {
        if (viewer == null || target == null) {
            return;
        }
        if (!visibilityResolved) {
            resolveVisibility();
        }
        Method modern = visible ? bukkitShowPlayer2 : bukkitHidePlayer2;
        if (modern != null) {
            try {
                modern.invoke(viewer, plugin, target);
                return;
            } catch (Throwable ignored) {
                // fall through to the legacy single-argument form
            }
        }
        Method legacy = visible ? bukkitShowPlayer1 : bukkitHidePlayer1;
        if (legacy != null) {
            try {
                legacy.invoke(viewer, target);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void resolveVisibility() {
        synchronized (Players.class) {
            if (visibilityResolved) {
                return;
            }
            bukkitHidePlayer2 = Reflection.method(Player.class, "hidePlayer", Plugin.class, Player.class);
            bukkitShowPlayer2 = Reflection.method(Player.class, "showPlayer", Plugin.class, Player.class);
            bukkitHidePlayer1 = Reflection.method(Player.class, "hidePlayer", Player.class);
            bukkitShowPlayer1 = Reflection.method(Player.class, "showPlayer", Player.class);
            visibilityResolved = true;
        }
    }
}
