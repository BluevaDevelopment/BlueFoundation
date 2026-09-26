package net.blueva.foundation.npc;

import net.blueva.foundation.npc.util.NpcAnimation;
import net.blueva.foundation.reflection.Reflection;

import java.lang.reflect.Constructor;

/**
 * Animation packets for versions that send arm swings as their own packet (26.3 and newer).
 *
 * <p>Those versions also renumbered {@code ClientboundAnimatePacket} to wake up (0), critical
 * hit (1) and magic critical hit (2), so the legacy ids in {@link NpcAnimation} no longer apply.</p>
 */
final class NpcAnimationPackets {

    private static final String GAME = "net.minecraft.network.protocol.game.";
    private static final Class<?> SWING_PACKET = Reflection.findClass(GAME + "ClientboundSwingAnimationPacket");

    private NpcAnimationPackets() {
    }

    static boolean supported() {
        return SWING_PACKET != null;
    }

    /** Returns the packet for the animation, or null when this entity cannot play it. */
    static Object create(Object entityHandle, NpcAnimation animation) {
        switch (animation) {
            case SWING_MAIN_ARM:
                return swing(entityHandle, "MAIN_HAND");
            case SWING_OFFHAND:
                return swing(entityHandle, "OFF_HAND");
            case TAKE_DAMAGE:
                return construct(Reflection.findClass(GAME + "ClientboundHurtAnimationPacket"), entityHandle);
            case LEAVE_BED:
                // The client casts the entity to a player for this action.
                return isPlayer(entityHandle) ? animate(entityHandle, 0) : null;
            case CRITICAL:
                return animate(entityHandle, 1);
            case MAGIC_CRITICAL:
                return animate(entityHandle, 2);
            default:
                return null;
        }
    }

    private static Object swing(Object entityHandle, String hand) {
        try {
            Class<?> handClass = Reflection.findClass("net.minecraft.world.InteractionHand");
            Class<?> styleClass = Reflection.findClass("net.minecraft.world.item.component.SwingAnimation");
            if (handClass == null || styleClass == null) {
                return null;
            }
            Object handValue = Enum.valueOf(handClass.asSubclass(Enum.class), hand);
            Object style = styleClass.getField("DEFAULT").get(null);
            return construct(SWING_PACKET, entityHandle, handValue, style);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object animate(Object entityHandle, int action) {
        return construct(Reflection.findClass(GAME + "ClientboundAnimatePacket"), entityHandle, action);
    }

    private static boolean isPlayer(Object entityHandle) {
        Class<?> playerClass = Reflection.findClass("net.minecraft.world.entity.player.Player");
        return playerClass != null && playerClass.isInstance(entityHandle);
    }

    private static Object construct(Class<?> packetClass, Object... args) {
        if (packetClass == null || args[0] == null) {
            return null;
        }
        for (Constructor<?> constructor : packetClass.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != args.length || !params[0].isInstance(args[0])) {
                continue;
            }
            try {
                return constructor.newInstance(args);
            } catch (Throwable ignored) {
                // Try the next overload.
            }
        }
        return null;
    }
}
