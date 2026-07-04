package net.blueva.foundation.npc;

import net.blueva.foundation.npc.util.NpcPose;
import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Set;

/**
 * Builds and sends NMS packets for player NPCs.
 *
 * <p>All packet construction is reflection-based so the same code compiles
 * against Spigot 1.8.8 while still working on modern Paper versions.</p>
 */
final class NpcPackets {

    private NpcPackets() {
    }

    static void sendInfoAdd(Player viewer, Object serverPlayer) {
        if (serverPlayer == null) {
            return;
        }
        Object packet;
        if (Version.isAtLeast(1, 19, 3)) {
            packet = newModernInfoAdd(serverPlayer);
        } else {
            packet = newLegacyInfoPacket(serverPlayer, "ADD_PLAYER");
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendInfoRemove(Player viewer, UUID uuid, Object serverPlayer) {
        Object packet;
        if (Version.isAtLeast(1, 19, 3)) {
            packet = newModernInfoRemove(uuid);
        } else {
            packet = newLegacyInfoPacket(serverPlayer, "REMOVE_PLAYER");
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendSpawn(Player viewer, Object serverPlayer) {
        if (serverPlayer == null) {
            return;
        }
        Object packet;
        if (Version.isAtLeast(1, 20)) {
            packet = newModernSpawnPacket(serverPlayer);
        } else if (Version.isAtLeast(1, 19)) {
            packet = newPacket("ClientboundAddPlayerPacket", "PacketPlayOutNamedEntitySpawn", serverPlayer);
        } else {
            packet = newPacket("PacketPlayOutNamedEntitySpawn", serverPlayer);
        }
        Reflection.sendPacket(viewer, packet);
    }



    static void sendMetadata(Player viewer, Object serverPlayer) {
        if (serverPlayer == null) {
            return;
        }
        try {
            int entityId = (Integer) serverPlayer.getClass().getMethod("getId").invoke(serverPlayer);
            Object dataWatcher = invokeEither(serverPlayer, "getEntityData", "getDataWatcher");

            Object packet;
            if (Version.isAtLeast(1, 19, 3)) {
                Class<?> packetClass = Reflection.nmsClass("ClientboundSetEntityDataPacket",
                        "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
                Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, List.class);
                constructor.setAccessible(true);
                List<?> packed = packedEntityData(dataWatcher);
                packet = constructor.newInstance(entityId, packed);
            } else if (Version.isAtLeast(1, 9)) {
                Class<?> packetClass = Reflection.nmsClass("PacketPlayOutEntityMetadata");
                Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, getDataWatcherClass(), boolean.class);
                constructor.setAccessible(true);
                packet = constructor.newInstance(entityId, dataWatcher, true);
            } else {
                Class<?> packetClass = Reflection.nmsClass("PacketPlayOutEntityMetadata");
                Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, getDataWatcherClass(), int.class);
                constructor.setAccessible(true);
                packet = constructor.newInstance(entityId, dataWatcher, 0);
            }
            Reflection.sendPacket(viewer, packet);
        } catch (Throwable ignored) {
        }
    }

    static void sendEquipment(Player viewer, int entityId, ItemStack[] equipment) {
        if (equipment == null) {
            return;
        }
        try {
            if (Version.isAtLeast(1, 16)) {
                Object packet = newModernEquipment(entityId, equipment);
                if (packet != null) {
                    Reflection.sendPacket(viewer, packet);
                }
            } else {
                List<Object> packets = newLegacyEquipment(entityId, equipment);
                for (Object packet : packets) {
                    Reflection.sendPacket(viewer, packet);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static void sendTeleport(Player viewer, Object serverPlayer, Location location) {
        if (serverPlayer == null || location == null) {
            return;
        }
        updatePosition(serverPlayer, location);
        Object packet;
        if (Version.isAtLeast(1, 21)) {
            packet = newModernTeleport(serverPlayer);
        } else if (Version.isAtLeast(1, 17)) {
            packet = newPacket("ClientboundTeleportEntityPacket", "PacketPlayOutEntityTeleport", serverPlayer);
        } else {
            packet = newPacket("PacketPlayOutEntityTeleport", serverPlayer);
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendHeadRotation(Player viewer, Object serverPlayer, float yaw) {
        if (serverPlayer == null) {
            return;
        }
        Object packet;
        if (Version.isAtLeast(1, 17)) {
            packet = newModernHeadRotation(serverPlayer, yaw);
        } else {
            packet = newLegacyHeadRotation(serverPlayer, yaw);
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendAnimation(Player viewer, Object serverPlayer, int animationId) {
        if (serverPlayer == null) {
            return;
        }
        Object packet;
        if (Version.isAtLeast(1, 17)) {
            packet = newPacket("ClientboundAnimatePacket", "PacketPlayOutAnimation", serverPlayer, animationId);
        } else {
            packet = newPacket("PacketPlayOutAnimation", serverPlayer, animationId);
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendAddEntity(Player viewer, Object entityHandle) {
        if (entityHandle == null || !Version.isAtLeast(1, 20)) {
            return;
        }
        Object packet = newAddEntityPacket(entityHandle);
        if (packet != null) {
            Reflection.sendPacket(viewer, packet);
        }
    }

    static void sendDestroy(Player viewer, int entityId) {
        Object packet;
        if (Version.isAtLeast(1, 17)) {
            packet = newModernDestroy(entityId);
        } else {
            packet = newLegacyDestroy(entityId);
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendGlow(Player viewer, Object serverPlayer, boolean glowing) {
        if (serverPlayer == null) {
            return;
        }
        try {
            Method setGlowingTag = findMethod(serverPlayer.getClass(), "setGlowingTag", boolean.class);
            if (setGlowingTag != null) {
                setGlowingTag.setAccessible(true);
                setGlowingTag.invoke(serverPlayer, glowing);
            } else {
                setSharedFlag(serverPlayer, 5, glowing);
            }
        } catch (Throwable ignored) {
            setSharedFlag(serverPlayer, 5, glowing);
        }
        sendMetadata(viewer, serverPlayer);
    }

    static void sendPose(Player viewer, Object serverPlayer, NpcPose pose) {
        if (serverPlayer == null || pose == null) {
            return;
        }
        setPose(serverPlayer, pose);
        sendMetadata(viewer, serverPlayer);
    }

    static void sendScale(Player viewer, Object serverPlayer, double scale) {
        if (serverPlayer == null || !Version.isAtLeast(1, 20, 6)) {
            return;
        }
        try {
            Object attributeInstance = getScaleAttribute(serverPlayer);
            if (attributeInstance == null) {
                return;
            }
            Method setBase = findMethod(attributeInstance.getClass(), "setBaseValue", double.class);
            if (setBase != null) {
                setBase.setAccessible(true);
                setBase.invoke(attributeInstance, scale);
            }

            int entityId = intValue(serverPlayer, "getId");
            Class<?> packetClass = Reflection.nmsClass("ClientboundUpdateAttributesPacket",
                    "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket");
            if (packetClass == null) {
                return;
            }
            List<Object> list = Collections.singletonList(attributeInstance);
            Constructor<?> publicConstructor = null;
            Constructor<?> fallbackConstructor = null;
            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2 && params[0] == int.class) {
                    if (params[1] == Collection.class) {
                        publicConstructor = constructor;
                    } else if (Collection.class.isAssignableFrom(params[1]) && fallbackConstructor == null) {
                        fallbackConstructor = constructor;
                    }
                }
            }
            Constructor<?> constructor = publicConstructor != null ? publicConstructor : fallbackConstructor;
            if (constructor != null) {
                Object packet = constructor.newInstance(entityId, list);
                Reflection.sendPacket(viewer, packet);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    static void sendTeam(Player viewer, String teamName, String entry, org.bukkit.ChatColor color,
                         org.bukkit.scoreboard.NameTagVisibility nameTagVisibility) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundSetPlayerTeamPacket",
                    "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
            if (packetClass == null) {
                return;
            }

            // Modern versions: static createAddOrModifyPacket(String name, String displayName,
            // String prefix, String suffix, boolean friendlyFire, boolean seeFriendlyInvisibles,
            // NameTagVisibility tagVisibility, CollisionRule collisionRule, EnumChatFormat color, Collection<String> players)
            for (Method method : packetClass.getDeclaredMethods()) {
                if (!method.getName().equals("createAddOrModifyPacket")) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length >= 10) {
                    method.setAccessible(true);
                    Class<?> nameTagVisibilityClass = Reflection.nmsClass("Team$Visibility",
                            "net.minecraft.world.scores.ScoreboardTeamBase$EnumNameTagVisibility");
                    Class<?> collisionRuleClass = Reflection.nmsClass("Team$CollisionRule",
                            "net.minecraft.world.scores.ScoreboardTeamBase$EnumTeamPush");
                    Class<?> chatFormatClass = Reflection.nmsClass("ChatFormatting", "net.minecraft.ChatFormatting");
                    Object nmsVisibility = enumByName(nameTagVisibilityClass,
                            nameTagVisibility == org.bukkit.scoreboard.NameTagVisibility.ALWAYS ? "ALWAYS" : "NEVER");
                    Object nmsCollision = enumByName(collisionRuleClass, "ALWAYS");
                    Object nmsColor = enumByName(chatFormatClass, color == null ? "WHITE" : color.name());
                    Object packet = method.invoke(null, teamName, teamName, "", "", false, false,
                            nmsVisibility, nmsCollision, nmsColor, Collections.singletonList(entry));
                    Reflection.sendPacket(viewer, packet);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object enumByName(Class<?> enumClass, String name) {
        if (enumClass == null || name == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getScaleAttribute(Object serverPlayer) {
        try {
            Class<?> attributesClass = Reflection.findClass("net.minecraft.world.entity.ai.attributes.Attributes");
            if (attributesClass == null) {
                return null;
            }
            Field scaleField = findField(attributesClass, "SCALE");
            if (scaleField == null) {
                return null;
            }
            scaleField.setAccessible(true);
            Object scaleAttribute = scaleField.get(null);
            if (scaleAttribute == null) {
                return null;
            }
            for (Method method : serverPlayer.getClass().getMethods()) {
                if (!method.getName().equals("getAttribute") || method.getParameterCount() != 1) {
                    continue;
                }
                method.setAccessible(true);
                Object instance = method.invoke(serverPlayer, scaleAttribute);
                if (instance != null) {
                    return instance;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void setSharedFlag(Object serverPlayer, int flagIndex, boolean value) {
        try {
            Object dataWatcher = invokeEither(serverPlayer, "getEntityData", "getDataWatcher");
            Class<?> entityClass = Reflection.findClass("net.minecraft.world.entity.Entity");
            if (entityClass == null) {
                return;
            }
            Field accessorField = findField(entityClass, "DATA_SHARED_FLAGS_ID");
            if (accessorField == null) {
                return;
            }
            accessorField.setAccessible(true);
            Object accessor = accessorField.get(null);
            if (accessor == null) {
                return;
            }
            Method get = findMethod(dataWatcher.getClass(), "get", accessor.getClass());
            if (get == null) {
                return;
            }
            get.setAccessible(true);
            Byte current = (Byte) get.invoke(dataWatcher, accessor);
            byte bits = current == null ? 0 : current;
            int flag = 1 << flagIndex;
            byte newBits = value ? (byte) (bits | flag) : (byte) (bits & ~flag);
            Method set = findMethod(dataWatcher.getClass(), "set", accessor.getClass(), Object.class);
            if (set == null) {
                return;
            }
            set.setAccessible(true);
            set.invoke(dataWatcher, accessor, newBits);
        } catch (Throwable ignored) {
        }
    }

    private static void setPose(Object serverPlayer, NpcPose pose) {
        try {
            Object dataWatcher = invokeEither(serverPlayer, "getEntityData", "getDataWatcher");
            Class<?> entityClass = Reflection.findClass("net.minecraft.world.entity.Entity");
            if (entityClass == null) {
                return;
            }
            Field accessorField = findField(entityClass, "DATA_POSE");
            if (accessorField == null) {
                accessorField = findField(Reflection.findClass("net.minecraft.world.entity.LivingEntity"), "DATA_POSE");
            }
            if (accessorField == null) {
                return;
            }
            accessorField.setAccessible(true);
            Object accessor = accessorField.get(null);
            if (accessor == null) {
                return;
            }
            Class<?> poseClass = Reflection.findClass("net.minecraft.world.entity.Pose");
            if (poseClass == null) {
                return;
            }
            Object nmsPose = Enum.valueOf(poseClass.asSubclass(Enum.class), pose.name());
            Method set = findMethod(dataWatcher.getClass(), "set", accessor.getClass(), Object.class);
            if (set == null) {
                return;
            }
            set.setAccessible(true);
            set.invoke(dataWatcher, accessor, nmsPose);
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

    private static Object newModernInfoAdd(Object serverPlayer) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundPlayerInfoUpdatePacket",
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            if (packetClass == null) {
                return null;
            }
            // Paper 1.21.5+ added createSinglePlayerInitializing which respects the listed flag.
            try {
                Method method = packetClass.getMethod("createSinglePlayerInitializing", serverPlayer.getClass(), boolean.class);
                method.setAccessible(true);
                return method.invoke(null, serverPlayer, false);
            } catch (NoSuchMethodException ignored) {
            }
            Method method = packetClass.getMethod("createPlayerInitializing", java.util.Collection.class);
            method.setAccessible(true);
            return method.invoke(null, Collections.singletonList(serverPlayer));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newModernInfoRemove(UUID uuid) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundPlayerInfoRemovePacket",
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
            if (packetClass == null) {
                return null;
            }
            Constructor<?> constructor = packetClass.getDeclaredConstructor(List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(Collections.singletonList(uuid));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newLegacyInfoPacket(Object serverPlayer, String actionName) {
        try {
            Class<?> packetClass = Reflection.nmsClass("PacketPlayOutPlayerInfo");
            Class<?> actionClass = Reflection.nmsClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction",
                    "EnumPlayerInfoAction");
            if (packetClass == null || actionClass == null) {
                return null;
            }
            Object action = Enum.valueOf(actionClass.asSubclass(Enum.class), actionName);

            Object array = java.lang.reflect.Array.newInstance(serverPlayer.getClass(), 1);
            java.lang.reflect.Array.set(array, 0, serverPlayer);
            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2 && params[0] == actionClass) {
                    if (params[1].isArray() && params[1].getComponentType().isAssignableFrom(serverPlayer.getClass())) {
                        return constructor.newInstance(action, array);
                    }
                    if (Iterable.class.isAssignableFrom(params[1])) {
                        return constructor.newInstance(action, Collections.singletonList(serverPlayer));
                    }
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }



    private static Object newPacket(String modernName, String legacyName, Object... args) {
        Class<?> packetClass = Reflection.nmsClass(legacyName, "net.minecraft.network.protocol.game." + modernName);
        if (packetClass == null) {
            return null;
        }
        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            constructor.setAccessible(true);
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != args.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                if (args[i] != null && !wrap(params[i]).isAssignableFrom(wrap(args[i].getClass()))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                try {
                    return constructor.newInstance(args);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object newPacket(String legacyName, Object... args) {
        Class<?> packetClass = Reflection.nmsClass(legacyName);
        if (packetClass == null) {
            return null;
        }
        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            constructor.setAccessible(true);
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != args.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < params.length; i++) {
                if (args[i] != null && !wrap(params[i]).isAssignableFrom(wrap(args[i].getClass()))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                try {
                    return constructor.newInstance(args);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object newModernSpawnPacket(Object serverPlayer) {
        return newAddEntityPacket(serverPlayer);
    }

    private static Object newAddEntityPacket(Object entityHandle) {
        // 1.21+ requires a ServerEntity when building the spawn packet for some
        // entity types (especially hostile mobs), so try that first.
        if (Version.isAtLeast(1, 21)) {
            Object packet = newAddEntityPacketWithServerEntity(entityHandle);
            if (packet != null) {
                return packet;
            }
        }
        Object built = invokeNoArg(entityHandle, "getAddEntityPacket");
        if (built != null) {
            return built;
        }
        // Fallback: build a generic ClientboundAddEntityPacket manually.
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundAddEntityPacket",
                    "net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            Class<?> entityTypeClass = Reflection.findClass("net.minecraft.world.entity.EntityType");
            Class<?> vec3Class = Reflection.findClass("net.minecraft.world.phys.Vec3");
            if (packetClass == null || entityTypeClass == null || vec3Class == null) {
                return null;
            }

            Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, UUID.class,
                    double.class, double.class, double.class, float.class, float.class,
                    entityTypeClass, int.class, vec3Class, double.class);
            constructor.setAccessible(true);
            return constructor.newInstance(
                    intValue(entityHandle, "getId"),
                    uuidValue(entityHandle),
                    doubleValue(entityHandle, "getX"),
                    doubleValue(entityHandle, "getY"),
                    doubleValue(entityHandle, "getZ"),
                    floatValue(entityHandle, "getXRot"),
                    floatValue(entityHandle, "getYRot"),
                    invokeNoArg(entityHandle, "getType"),
                    0,
                    vec3Value(entityHandle),
                    (double) floatValue(entityHandle, "getYRot"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newAddEntityPacketWithServerEntity(Object entityHandle) {
        try {
            Object level = invokeNoArg(entityHandle, "level");
            if (level == null) {
                return null;
            }
            Class<?> serverEntityClass = Reflection.findClass("net.minecraft.server.level.ServerEntity");
            if (serverEntityClass == null) {
                return null;
            }
            Object serverEntity = createServerEntity(level, entityHandle, serverEntityClass);
            if (serverEntity == null) {
                return null;
            }
            Class<?> packetClass = Reflection.nmsClass("ClientboundAddEntityPacket",
                    "net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            if (packetClass == null) {
                return null;
            }
            Constructor<?> constructor = findAddEntityPacketConstructor(packetClass, entityHandle.getClass(), serverEntityClass);
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            if (constructor.getParameterCount() == 2) {
                return constructor.newInstance(entityHandle, serverEntity);
            } else {
                return constructor.newInstance(entityHandle, serverEntity, 0);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Constructor<?> findAddEntityPacketConstructor(Class<?> packetClass, Class<?> handleClass, Class<?> serverEntityClass) {
        Class<?> entityClass = Reflection.findClass("net.minecraft.world.entity.Entity");
        for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length < 2 || params.length > 3) {
                continue;
            }
            if (!isAssignable(params[0], entityClass == null ? Object.class : entityClass)) {
                continue;
            }
            if (!isAssignable(params[1], serverEntityClass)) {
                continue;
            }
            if (params.length == 3 && !isAssignable(params[2], int.class)) {
                continue;
            }
            return constructor;
        }
        return null;
    }

    private static Object createServerEntity(Object level, Object entityHandle, Class<?> serverEntityClass) {
        try {
            Class<?> entityClass = Reflection.findClass("net.minecraft.world.entity.Entity");
            Class<?> serverLevelClass = Reflection.findClass("net.minecraft.server.level.ServerLevel");
            if (entityClass == null || serverLevelClass == null) {
                return null;
            }
            for (Constructor<?> constructor : serverEntityClass.getDeclaredConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length < 5 || params.length > 6) {
                    continue;
                }
                if (!isAssignable(params[0], serverLevelClass)) {
                    continue;
                }
                if (!isAssignable(params[1], entityClass)) {
                    continue;
                }
                if (!isAssignable(params[2], int.class)) {
                    continue;
                }
                if (!isAssignable(params[3], boolean.class)) {
                    continue;
                }
                Object fifthArg;
                if (isAssignable(Class.forName("java.util.function.Consumer"), params[4])) {
                    fifthArg = (java.util.function.Consumer<Object>) packet -> {};
                } else if (params[4].isInterface()) {
                    fifthArg = Proxy.newProxyInstance(params[4].getClassLoader(), new Class<?>[]{params[4]},
                            (proxy, method, args) -> null);
                } else {
                    continue;
                }
                Object[] args = new Object[params.length];
                args[0] = level;
                args[1] = entityHandle;
                args[2] = 0;
                args[3] = false;
                args[4] = fifthArg;
                if (params.length == 6) {
                    if (!java.util.Set.class.isAssignableFrom(params[5])) {
                        continue;
                    }
                    args[5] = java.util.Collections.emptySet();
                }
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isAssignable(Class<?> to, Class<?> from) {
        if (to == null || from == null) {
            return false;
        }
        if (to.isPrimitive() || from.isPrimitive()) {
            if (to == int.class || to == Integer.class) {
                return from == int.class || from == Integer.class;
            }
            if (to == boolean.class || to == Boolean.class) {
                return from == boolean.class || from == Boolean.class;
            }
            if (to == double.class || to == Double.class) {
                return from == double.class || from == Double.class;
            }
            if (to == float.class || to == Float.class) {
                return from == float.class || from == Float.class;
            }
            if (to == long.class || to == Long.class) {
                return from == long.class || from == Long.class;
            }
            if (to == byte.class || to == Byte.class) {
                return from == byte.class || from == Byte.class;
            }
            if (to == short.class || to == Short.class) {
                return from == short.class || from == Short.class;
            }
            return to == from;
        }
        return to.isAssignableFrom(from);
    }

    private static Object newModernTeleport(Object serverPlayer) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundTeleportEntityPacket",
                    "net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket");
            Class<?> entityClass = Reflection.findClass("net.minecraft.world.entity.Entity");
            Class<?> positionMoveRotationClass = Reflection.findClass("net.minecraft.world.entity.PositionMoveRotation");
            if (packetClass == null || entityClass == null || positionMoveRotationClass == null) {
                return null;
            }

            Method of = positionMoveRotationClass.getMethod("of", entityClass);
            of.setAccessible(true);
            Object change = of.invoke(null, serverPlayer);
            Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, positionMoveRotationClass, Set.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(intValue(serverPlayer, "getId"), change, Collections.emptySet(), booleanValue(serverPlayer, "onGround"));
        } catch (Throwable ignored) {
            return null;
        }
    }


    private static Object newModernEquipment(int entityId, ItemStack[] equipment) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundSetEquipmentPacket",
                    "net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket");
            Class<?> slotClass = Reflection.nmsClass("EnumItemSlot", "net.minecraft.world.entity.EquipmentSlot");
            Class<?> pairClass = Reflection.findClass("com.mojang.datafixers.util.Pair");
            if (packetClass == null || slotClass == null || pairClass == null) {
                return null;
            }

            Object[] slots = slotClass.getEnumConstants();
            List<EquipmentSlotPair> pairs = new ArrayList<>();
            for (int i = 0; i < Math.min(equipment.length, slots.length); i++) {
                if (equipment[i] == null) {
                    continue;
                }
                Object nmsItem = toNmsItem(equipment[i]);
                if (nmsItem == null) {
                    continue;
                }
                Object pair = pairClass.getMethod("of", Object.class, Object.class)
                        .invoke(null, slots[i], nmsItem);
                pairs.add(new EquipmentSlotPair(((Enum<?>) slots[i]).ordinal(), pair));
            }
            if (pairs.isEmpty()) {
                return null;
            }
            Collections.sort(pairs);
            List<Object> sorted = new ArrayList<>();
            for (EquipmentSlotPair pair : pairs) {
                sorted.add(pair.pair);
            }
            Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(entityId, sorted);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Object> newLegacyEquipment(int entityId, ItemStack[] equipment) {
        List<Object> packets = new ArrayList<>();
        try {
            Class<?> packetClass = Reflection.nmsClass("PacketPlayOutEntityEquipment");
            if (packetClass == null) {
                return packets;
            }
            Class<?> slotClass = Reflection.nmsClass("EnumItemSlot");
            Object[] slots = slotClass != null ? slotClass.getEnumConstants() : null;

            for (int i = 0; i < Math.min(equipment.length, slots != null ? slots.length : 4); i++) {
                if (equipment[i] == null) {
                    continue;
                }
                Object nmsItem = toNmsItem(equipment[i]);
                if (nmsItem == null) {
                    continue;
                }
                Object packet;
                if (slots != null) {
                    Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, slotClass, nmsItem.getClass());
                    constructor.setAccessible(true);
                    packet = constructor.newInstance(entityId, slots[i], nmsItem);
                } else {
                    Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, int.class, nmsItem.getClass());
                    constructor.setAccessible(true);
                    packet = constructor.newInstance(entityId, i, nmsItem);
                }
                packets.add(packet);
            }
        } catch (Throwable ignored) {
        }
        return packets;
    }

    private static Object newModernHeadRotation(Object serverPlayer, float yaw) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundRotateHeadPacket",
                    "net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
            if (packetClass == null) {
                return null;
            }
            Constructor<?> constructor = findConstructor(packetClass, serverPlayer.getClass(), byte.class);
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            return constructor.newInstance(serverPlayer, angleToByte(yaw));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newLegacyHeadRotation(Object serverPlayer, float yaw) {
        return newPacket("PacketPlayOutEntityHeadRotation", serverPlayer, angleToByte(yaw));
    }

    private static Object newLegacyDestroy(int entityId) {
        return newPacket("PacketPlayOutEntityDestroy", new int[]{entityId});
    }

    private static Object newModernDestroy(int entityId) {
        try {
            Class<?> packetClass = Reflection.nmsClass("ClientboundRemoveEntitiesPacket",
                    "net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
            if (packetClass == null) {
                return null;
            }
            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 1) {
                    if (params[0].isArray() && params[0].getComponentType() == int.class) {
                        return constructor.newInstance(new int[]{entityId});
                    }
                    if (params[0] == int.class) {
                        return constructor.newInstance(entityId);
                    }
                    if (params[0].getName().contains("IntList")) {
                        Object intList = createIntList(entityId);
                        if (intList != null) {
                            return constructor.newInstance(intList);
                        }
                    }
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createIntList(int entityId) {
        try {
            Class<?> intListClass = Reflection.findClass("it.unimi.dsi.fastutil.ints.IntArrayList");
            if (intListClass == null) {
                return null;
            }
            Object list = intListClass.getConstructor().newInstance();
            intListClass.getMethod("add", int.class).invoke(list, entityId);
            return list;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class EquipmentSlotPair implements Comparable<EquipmentSlotPair> {
        private final int ordinal;
        private final Object pair;

        EquipmentSlotPair(int ordinal, Object pair) {
            this.ordinal = ordinal;
            this.pair = pair;
        }

        @Override
        public int compareTo(EquipmentSlotPair other) {
            return Integer.compare(ordinal, other.ordinal);
        }
    }

    private static void updatePosition(Object serverPlayer, Location location) {
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
            Method setPosRot = findMethod(serverPlayer.getClass(), "setPosRot", double.class, double.class, double.class, float.class, float.class);
            if (setPosRot != null) {
                setPosRot.setAccessible(true);
                setPosRot.invoke(serverPlayer, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object toNmsItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            Class<?> craftItemStack = Reflection.craftBukkitClass("inventory.CraftItemStack");
            Method method = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            method.setAccessible(true);
            return method.invoke(null, item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> getDataWatcherClass() {
        return Reflection.nmsClass("DataWatcher", "net.minecraft.network.syncher.SynchedEntityData");
    }

    private static Object invokeEither(Object target, String first, String second) {
        try {
            return target.getClass().getMethod(first).invoke(target);
        } catch (Throwable ignored) {
        }
        try {
            return target.getClass().getMethod(second).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static byte angleToByte(float angle) {
        return (byte) ((angle * 256.0F) / 360.0F);
    }

    private static List<?> packedEntityData(Object dataWatcher) {
        if (dataWatcher == null) {
            return Collections.emptyList();
        }
        try {
            Method packAll = dataWatcher.getClass().getMethod("packAll");
            packAll.setAccessible(true);
            Object values = packAll.invoke(dataWatcher);
            if (values instanceof List<?>) {
                return (List<?>) values;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method nonDefault = dataWatcher.getClass().getMethod("getNonDefaultValues");
            nonDefault.setAccessible(true);
            Object values = nonDefault.invoke(dataWatcher);
            if (values instanceof List<?>) {
                return (List<?>) values;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
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
                if (argumentType == null || !wrap(parameters[i]).isAssignableFrom(wrap(argumentType))) {
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

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), methodName);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int intValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static double doubleValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    private static float floatValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static boolean booleanValue(Object target, String methodName) {
        Object value = invokeNoArg(target, methodName);
        return value instanceof Boolean && (Boolean) value;
    }

    private static UUID uuidValue(Object target) {
        Object value = invokeNoArg(target, "getUUID");
        return value instanceof UUID ? (UUID) value : UUID.randomUUID();
    }

    private static Object vec3Value(Object serverPlayer) {
        Object movement = invokeNoArg(serverPlayer, "getDeltaMovement");
        if (movement != null) {
            return movement;
        }
        try {
            Class<?> vec3Class = Reflection.findClass("net.minecraft.world.phys.Vec3");
            Constructor<?> constructor = vec3Class.getDeclaredConstructor(double.class, double.class, double.class);
            constructor.setAccessible(true);
            return constructor.newInstance(0.0D, 0.0D, 0.0D);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> wrap(Class<?> clazz) {
        if (clazz == int.class) {
            return Integer.class;
        }
        if (clazz == long.class) {
            return Long.class;
        }
        if (clazz == byte.class) {
            return Byte.class;
        }
        if (clazz == short.class) {
            return Short.class;
        }
        if (clazz == float.class) {
            return Float.class;
        }
        if (clazz == double.class) {
            return Double.class;
        }
        if (clazz == boolean.class) {
            return Boolean.class;
        }
        return clazz;
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
