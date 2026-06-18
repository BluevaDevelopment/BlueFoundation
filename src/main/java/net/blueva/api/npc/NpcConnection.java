package net.blueva.api.npc;

import net.blueva.api.reflection.Reflection;
import net.blueva.api.version.Version;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

/**
 * Creates a fake NMS connection for a player NPC.
 *
 * <p>The connection is intentionally non-functional: it exists only so the
 * fake {@code ServerPlayer}/{@code EntityPlayer} has a valid listener reference
 * and does not crash code that expects one.</p>
 */
final class NpcConnection {

    private final Object connection;

    private NpcConnection(Object connection) {
        this.connection = connection;
    }

    Object getHandle() {
        return connection;
    }

    static NpcConnection create() {
        Object connection = Version.isAtLeast(1, 17)
                ? createModern()
                : createLegacy();
        return new NpcConnection(connection);
    }

    private static Object createModern() {
        try {
            Class<?> connectionClass = Reflection.nmsClass("Connection", "net.minecraft.network.Connection");
            Class<?> packetFlowClass = Reflection.findClass("net.minecraft.network.protocol.PacketFlow");
            if (connectionClass == null || packetFlowClass == null) {
                return null;
            }
            Object serverbound = enumValue(packetFlowClass, "SERVERBOUND");
            Constructor<?> constructor = connectionClass.getDeclaredConstructor(packetFlowClass);
            constructor.setAccessible(true);
            Object connection = constructor.newInstance(serverbound);

            setField(connection, "channel", createEmbeddedChannel());
            setField(connection, "address", new InetSocketAddress("127.0.0.1", 0));
            setField(connection, "preparing", false);
            return connection;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createLegacy() {
        try {
            Class<?> networkManagerClass = Reflection.nmsClass("NetworkManager");
            Class<?> directionClass = Reflection.nmsClass("EnumProtocolDirection");
            if (networkManagerClass == null || directionClass == null) {
                return null;
            }
            Object serverbound = enumValue(directionClass, "SERVERBOUND");
            Constructor<?> constructor = networkManagerClass.getDeclaredConstructor(directionClass);
            constructor.setAccessible(true);
            Object connection = constructor.newInstance(serverbound);

            setField(connection, "channel", createEmbeddedChannel());
            setField(connection, "address", new InetSocketAddress("127.0.0.1", 0));
            return connection;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createEmbeddedChannel() {
        try {
            Class<?> channelClass = Class.forName("io.netty.channel.embedded.EmbeddedChannel");
            Constructor<?> constructor = channelClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object enumValue(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setField(Object target, String name, Object value) {
        if (target == null) {
            return;
        }
        try {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                field.set(target, value);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
