package net.blueva.api.reflection;

import net.blueva.api.version.Version;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection helpers for Bukkit, CraftBukkit and NMS access. */
public class Reflection {

    protected Reflection() {
    }

    public static boolean classExists(String className) {
        return findClass(className) != null;
    }

    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Class<?> craftBukkitClass(String path) {
        String cleanPath = stripLeadingDot(path);
        String revision = Version.craftBukkitRevision();

        if (!isBlank(revision)) {
            Class<?> versioned = findClass("org.bukkit.craftbukkit." + revision + "." + cleanPath);
            if (versioned != null) {
                return versioned;
            }
        }

        return findClass("org.bukkit.craftbukkit." + cleanPath);
    }

    public static Class<?> nmsClass(String legacyName, String... modernNames) {
        String revision = Version.craftBukkitRevision();
        if (!isBlank(revision) && !isBlank(legacyName)) {
            Class<?> legacy = findClass("net.minecraft.server." + revision + "." + stripLeadingDot(legacyName));
            if (legacy != null) {
                return legacy;
            }
        }

        if (modernNames != null) {
            for (String modernName : modernNames) {
                if (isBlank(modernName)) {
                    continue;
                }
                Class<?> modern = findClass(modernName);
                if (modern != null) {
                    return modern;
                }
            }
        }

        if (!isBlank(legacyName)) {
            return findClass("net.minecraft." + stripLeadingDot(legacyName));
        }
        return null;
    }

    public static Object getHandle(Object object) {
        if (object == null) {
            return null;
        }
        try {
            Method method = object.getClass().getMethod("getHandle");
            method.setAccessible(true);
            return method.invoke(object);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object playerConnection(Player player) {
        Object handle = getHandle(player);
        if (handle == null) {
            return null;
        }

        try {
            Field field = handle.getClass().getField("playerConnection");
            field.setAccessible(true);
            return field.get(handle);
        } catch (Throwable ignored) {
        }

        try {
            Field field = handle.getClass().getField("connection");
            field.setAccessible(true);
            return field.get(handle);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean sendPacket(Player player, Object packet) {
        Object connection = playerConnection(player);
        if (connection == null || packet == null) {
            return false;
        }

        for (Method method : connection.getClass().getMethods()) {
            if (!method.getName().equals("sendPacket") && !method.getName().equals("send")) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !parameterTypes[0].isAssignableFrom(packet.getClass())) {
                continue;
            }
            try {
                method.setAccessible(true);
                method.invoke(connection, packet);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static Method method(Class<?> type, String name, Class<?>... parameters) {
        if (type == null) {
            return null;
        }
        try {
            Method method = type.getMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Field field(Class<?> type, String name) {
        if (type == null) {
            return null;
        }
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String stripLeadingDot(String value) {
        if (value == null) {
            return "";
        }
        while (value.startsWith(".")) {
            value = value.substring(1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
