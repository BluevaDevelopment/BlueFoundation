package net.blueva.foundation.npc;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Creates a fake NMS entity handle for non-player NPC entity types.
 *
 * <p>The returned handle is not registered in the world; it only exists in memory
 * so packets can be built from it. If the current server version does not support
 * creating a fake entity directly, the method falls back to spawning (and immediately
 * removing) a real Bukkit entity and keeping its NMS handle.</p>
 */
final class NpcEntityFactory {

    private NpcEntityFactory() {
    }

    /**
     * Creates an NMS entity handle for the given Bukkit entity type at the given location.
     *
     * @param location the spawn location
     * @param type     the Bukkit entity type (must not be PLAYER)
     * @return the NMS entity handle, or null if it could not be created
     */
    static Object create(Location location, EntityType type) {
        if (location == null || location.getWorld() == null || type == null || type == EntityType.PLAYER) {
            return null;
        }

        Object handle;
        if (Version.isAtLeast(1, 21, 2)) {
            handle = createModern(location, type);
        } else if (Version.isAtLeast(1, 20, 6)) {
            handle = create1206(location, type);
        } else {
            handle = null;
        }

        if (handle == null) {
            handle = createViaBukkitSpawn(location, type);
        }

        return handle;
    }

    private static Object createModern(Location location, EntityType type) {
        try {
            Object nmsType = toNmsEntityType(type);
            Object level = getServerLevel(location.getWorld());
            if (nmsType == null || level == null) {
                return null;
            }

            Object spawnReason = enumConstant("net.minecraft.world.entity.EntitySpawnReason", "LOAD");
            if (spawnReason == null) {
                spawnReason = enumConstant("net.minecraft.world.entity.MobSpawnType", "LOAD");
            }

            // First try EntityType#create, which just constructs the entity without
            // running any spawn checks. This is required for hostile mobs on peaceful.
            Object handle = createDirect(nmsType, level, spawnReason);
            if (handle == null) {
                Object compound = newCompoundTag();
                putString(compound, "id", type.name().toLowerCase(java.util.Locale.ROOT));
                Object processor = staticField("net.minecraft.world.entity.EntityProcessor", "NOP");

                if (Version.isAtLeast(1, 21, 11)) {
                    handle = invokeStatic("net.minecraft.world.entity.EntityType", "loadEntityRecursive",
                            nmsType, compound, level, spawnReason, processor).orElse(null);
                } else {
                    handle = invokeStatic("net.minecraft.world.entity.EntityTypes", "a",
                            nmsType, compound, level, spawnReason, processor).orElse(null);
                }
            }
            if (handle != null) {
                setPositionAndRotation(handle, location);
            }
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method.invoke(target);
                }
            }
            for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        return method.invoke(target);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object createDirect(Object nmsType, Object level, Object spawnReason) {
        if (nmsType == null || level == null || spawnReason == null) {
            return null;
        }
        try {
            Method create = findMethodByParams(nmsType.getClass(), "create",
                    new Class<?>[]{level.getClass(), spawnReason.getClass()});
            if (create == null) {
                return null;
            }
            create.setAccessible(true);
            return create.invoke(nmsType, level, spawnReason);
        } catch (Throwable e) {
            java.util.logging.Logger.getLogger("BlueFoundation").log(java.util.logging.Level.WARNING,
                    "[NpcEntityFactory] createDirect failed", e);
            return null;
        }
    }

    private static Object create1206(Location location, EntityType type) {
        try {
            Object nmsType = toNmsEntityType(type);
            Object level = getServerLevel(location.getWorld());
            if (nmsType == null || level == null) {
                return null;
            }

            Object spawnReason = enumConstant("net.minecraft.world.entity.EntitySpawnReason", "LOAD");
            if (spawnReason == null) {
                spawnReason = enumConstant("net.minecraft.world.entity.MobSpawnType", "LOAD");
            }

            Object handle = createDirect(nmsType, level, spawnReason);
            if (handle == null) {
                Object compound = newCompoundTag();
                putString(compound, "id", type.name().toLowerCase(java.util.Locale.ROOT));
                handle = invokeStatic("net.minecraft.world.entity.EntityTypes", "a",
                        compound, level, spawnReason, identityFunction()).orElse(null);
            }
            if (handle != null) {
                setPositionAndRotation(handle, location);
            }
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createViaBukkitSpawn(Location location, EntityType type) {
        try {
            Entity entity = location.getWorld().spawnEntity(location, type);
            if (entity == null) {
                return null;
            }
            Object handle = Reflection.getHandle(entity);
            entity.remove();
            return handle;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object toNmsEntityType(EntityType type) {
        try {
            Class<?> craftEntityType = Reflection.findClass("org.bukkit.craftbukkit.entity.CraftEntityType");
            if (craftEntityType != null) {
                Method method = craftEntityType.getDeclaredMethod("bukkitToMinecraft", EntityType.class);
                method.setAccessible(true);
                return method.invoke(null, type);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object newCompoundTag() {
        try {
            Class<?> clazz = Reflection.findClass("net.minecraft.nbt.CompoundTag");
            if (clazz == null) {
                clazz = Reflection.findClass("net.minecraft.nbt.NBTTagCompound");
            }
            if (clazz == null) {
                return null;
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void putString(Object compound, String key, String value) {
        try {
            Method method = findMethodByName(compound.getClass(), "putString", "a");
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(compound, key, value);
        } catch (Throwable ignored) {
        }
    }

    private static Object getServerLevel(org.bukkit.World world) {
        try {
            Method method = world.getClass().getMethod("getHandle");
            method.setAccessible(true);
            return method.invoke(world);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setPositionAndRotation(Object handle, Location location) {
        try {
            Method setPos = findMethodByParams(handle.getClass(), "setPos",
                    new Class<?>[]{double.class, double.class, double.class});
            if (setPos != null) {
                setPos.setAccessible(true);
                setPos.invoke(handle, location.getX(), location.getY(), location.getZ());
            }
            Method setRot = findMethodByParams(handle.getClass(), "setRot",
                    new Class<?>[]{float.class, float.class});
            if (setRot != null) {
                setRot.setAccessible(true);
                setRot.invoke(handle, location.getYaw(), location.getPitch());
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object identityFunction() {
        try {
            Class<?> functionClass = Class.forName("java.util.function.Function");
            Method identity = functionClass.getMethod("identity");
            return identity.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.util.Optional<Object> invokeStatic(String className, String methodName, Object... args) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return java.util.Optional.empty();
            }
            Method method = findMethodByName(clazz, methodName);
            if (method == null) {
                return java.util.Optional.empty();
            }
            method.setAccessible(true);
            return java.util.Optional.ofNullable(method.invoke(null, args));
        } catch (Throwable e) {
            return java.util.Optional.empty();
        }
    }

    private static Object enumConstant(String className, String constant) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return null;
            }
            return Enum.valueOf(clazz.asSubclass(Enum.class), constant);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object staticField(String className, String fieldName) {
        try {
            Class<?> clazz = Reflection.findClass(className);
            if (clazz == null) {
                return null;
            }
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findMethodByName(Class<?> clazz, String... names) {
        for (String name : names) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Method findMethodByParams(Class<?> clazz, String name, Class<?>[] params) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name)) {
                    continue;
                }
                Class<?>[] methodParams = method.getParameterTypes();
                if (methodParams.length != params.length) {
                    continue;
                }
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (!methodParams[i].isAssignableFrom(params[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return method;
                }
            }
        }
        return null;
    }
}
