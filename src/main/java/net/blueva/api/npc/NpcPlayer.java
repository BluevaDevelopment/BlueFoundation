package net.blueva.api.npc;

import com.mojang.authlib.GameProfile;
import net.blueva.api.reflection.Reflection;
import net.blueva.api.version.Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Creates and wraps a fake NMS player entity for packet generation.
 *
 * <p>The entity is never registered in the server's {@code PlayerList}; it only
 * exists in memory so modern versions can build player-info/spawn packets via
 * their vanilla constructors.</p>
 */
final class NpcPlayer {

    private final Object handle;

    private NpcPlayer(Object handle) {
        this.handle = handle;
    }

    Object getHandle() {
        return handle;
    }

    static NpcPlayer create(UUID uuid, String internalName, Location location, NpcConnection connection) {
        Object serverPlayer = Version.isAtLeast(1, 17)
                ? createModern(uuid, internalName, location, connection)
                : createLegacy(uuid, internalName, location, connection);
        return new NpcPlayer(serverPlayer);
    }

    private static Object createModern(UUID uuid, String internalName, Location location, NpcConnection connection) {
        try {
            Object server = getMinecraftServer();
            Object level = getServerLevel(location.getWorld());
            if (server == null || level == null) {
                return null;
            }

            GameProfile profile = new GameProfile(uuid, internalName);
            Class<?> serverPlayerClass = Reflection.nmsClass("ServerPlayer", "net.minecraft.server.level.ServerPlayer");
            Class<?> clientInfoClass = Reflection.findClass("net.minecraft.server.level.ClientInformation");
            Object clientInfo = createDefaultClientInformation(clientInfoClass);

            if (serverPlayerClass == null || clientInfo == null) {
                return null;
            }

            Constructor<?> constructor = findConstructor(serverPlayerClass,
                    server.getClass(), level.getClass(), GameProfile.class, clientInfoClass);
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            Object serverPlayer = constructor.newInstance(server, level, profile, clientInfo);

            createAndSetPacketListener(serverPlayer, connection, profile);
            setLocation(serverPlayer, location);
            return serverPlayer;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createLegacy(UUID uuid, String internalName, Location location, NpcConnection connection) {
        try {
            Object server = getMinecraftServer();
            Object level = getServerLevel(location.getWorld());
            if (server == null || level == null) {
                return null;
            }

            GameProfile profile = new GameProfile(uuid, internalName);
            Class<?> entityPlayerClass = Reflection.nmsClass("EntityPlayer");
            Class<?> interactManagerClass = Reflection.nmsClass("PlayerInteractManager");
            if (entityPlayerClass == null || interactManagerClass == null) {
                return null;
            }

            Constructor<?> interactConstructor = null;
            for (Constructor<?> c : interactManagerClass.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(level.getClass())) {
                    interactConstructor = c;
                    break;
                }
            }
            if (interactConstructor == null) {
                return null;
            }
            interactConstructor.setAccessible(true);
            Object interactManager = interactConstructor.newInstance(level);

            Constructor<?> constructor = findConstructor(entityPlayerClass,
                    server.getClass(), level.getClass(), GameProfile.class, interactManagerClass);
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            Object entityPlayer = constructor.newInstance(server, level, profile, interactManager);

            createAndSetPacketListener(entityPlayer, connection, profile);
            setLocation(entityPlayer, location);
            return entityPlayer;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getMinecraftServer() {
        try {
            Object craftServer = Bukkit.getServer();
            Method method = craftServer.getClass().getMethod("getServer");
            method.setAccessible(true);
            return method.invoke(craftServer);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Constructor<?> findConstructor(Class<?> type, Class<?>... argumentTypes) {
        if (type == null || argumentTypes == null) {
            return null;
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != argumentTypes.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < parameters.length; i++) {
                Class<?> argumentType = argumentTypes[i];
                if (argumentType == null || !parameters[i].isAssignableFrom(argumentType)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return constructor;
            }
        }
        return null;
    }

    private static Object getServerLevel(World world) {
        if (world == null) {
            return null;
        }
        try {
            Method method = world.getClass().getMethod("getHandle");
            method.setAccessible(true);
            return method.invoke(world);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createDefaultClientInformation(Class<?> clientInfoClass) {
        if (clientInfoClass == null) {
            return null;
        }
        try {
            Method method = clientInfoClass.getMethod("createDefault");
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Throwable ignored) {
        }
        try {
            Constructor<?> constructor = clientInfoClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void createAndSetPacketListener(Object serverPlayer, NpcConnection connection, GameProfile profile) {
        if (serverPlayer == null || connection == null || connection.getHandle() == null) {
            return;
        }
        Object listener = createPacketListener(serverPlayer, connection, profile);
        if (listener != null) {
            setField(serverPlayer, "connection", listener);
            setField(serverPlayer, "playerConnection", listener);
        }
    }

    private static Object createPacketListener(Object serverPlayer, NpcConnection connection, GameProfile profile) {
        try {
            Object server = getMinecraftServer();
            Object nmsConnection = connection.getHandle();

            Class<?> listenerClass = Reflection.nmsClass("ServerGamePacketListenerImpl",
                    "net.minecraft.server.network.ServerGamePacketListenerImpl");
            if (listenerClass != null) {
                Class<?> cookieClass = Reflection.findClass("net.minecraft.server.network.CommonListenerCookie");
                Object cookie = null;
                if (cookieClass != null) {
                    try {
                        Method createInitial = cookieClass.getMethod("createInitial", GameProfile.class, boolean.class);
                        createInitial.setAccessible(true);
                        cookie = createInitial.invoke(null, profile, false);
                    } catch (Throwable ignored) {
                    }
                }
                for (Constructor<?> constructor : listenerClass.getDeclaredConstructors()) {
                    constructor.setAccessible(true);
                    Class<?>[] params = constructor.getParameterTypes();
                    if (params.length == 4 && cookie != null) {
                        if (params[0].isAssignableFrom(server.getClass())
                                && params[1].isAssignableFrom(nmsConnection.getClass())
                                && params[2].isAssignableFrom(serverPlayer.getClass())
                                && params[3].isAssignableFrom(cookieClass)) {
                            return constructor.newInstance(server, nmsConnection, serverPlayer, cookie);
                        }
                    }
                    if (params.length == 3) {
                        if (params[0].isAssignableFrom(server.getClass())
                                && params[1].isAssignableFrom(nmsConnection.getClass())
                                && params[2].isAssignableFrom(serverPlayer.getClass())) {
                            return constructor.newInstance(server, nmsConnection, serverPlayer);
                        }
                    }
                }
                return null;
            }

            listenerClass = Reflection.nmsClass("PlayerConnection");
            if (listenerClass == null) {
                return null;
            }
            for (Constructor<?> constructor : listenerClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 3
                        && params[0].isAssignableFrom(server.getClass())
                        && params[1].isAssignableFrom(nmsConnection.getClass())
                        && params[2].isAssignableFrom(serverPlayer.getClass())) {
                    return constructor.newInstance(server, nmsConnection, serverPlayer);
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setLocation(Object serverPlayer, Location location) {
        if (serverPlayer == null || location == null) {
            return;
        }
        try {
            Method setPos = findMethod(serverPlayer.getClass(), "setPos", double.class, double.class, double.class);
            if (setPos != null) {
                setPos.setAccessible(true);
                setPos.invoke(serverPlayer, location.getX(), location.getY(), location.getZ());
            }
            Method setRot = findMethod(serverPlayer.getClass(), "setRot", float.class, float.class);
            if (setRot != null) {
                setRot.setAccessible(true);
                setRot.invoke(serverPlayer, location.getYaw(), location.getPitch());
            }
            if (setPos == null) {
                setField(serverPlayer, "x", location.getX());
                setField(serverPlayer, "y", location.getY());
                setField(serverPlayer, "z", location.getZ());
                setField(serverPlayer, "yaw", location.getYaw());
                setField(serverPlayer, "pitch", location.getPitch());
            }
        } catch (Throwable ignored) {
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

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
