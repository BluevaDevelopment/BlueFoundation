package net.blueva.foundation.hologram;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Builds and sends NMS packets for fake {@code TextDisplay} hologram lines.
 *
 * <p>Deliberately self-contained (not shared with {@code net.blueva.foundation.npc.NpcPackets})
 * even though several helpers below are near-identical in spirit - the NPC system is already
 * confirmed working in production and this module avoids touching it at all. All packet
 * construction is reflection-based, same "resolve by name, fall back to structural shape"
 * convention already established for the skin-layers accessor lookup.</p>
 */
final class HologramPackets {

    private static volatile boolean textAccessorFailureLogged = false;
    private static volatile boolean billboardAccessorFailureLogged = false;
    private static volatile boolean scaleAccessorFailureLogged = false;
    private static volatile boolean backgroundAccessorFailureLogged = false;
    private static volatile boolean styleFlagsAccessorFailureLogged = false;
    private static volatile boolean teleportDurationAccessorFailureLogged = false;

    private HologramPackets() {
    }

    static int resolveEntityId(Object handle) {
        if (handle == null) {
            return -1;
        }
        try {
            Method method = handle.getClass().getMethod("getId");
            method.setAccessible(true);
            return (Integer) method.invoke(handle);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /** Sets the line's text on the entity handle's metadata - in memory only, no packet sent
     * yet. Call {@link #sendMetadata(Player, Object)} afterwards to push it to a viewer. */
    static void setText(Object entityHandle, String legacyText) {
        if (entityHandle == null) {
            return;
        }
        try {
            Component adventureComponent = LegacyComponentSerializer.legacySection()
                    .deserialize(legacyText == null ? "" : legacyText);
            Object nmsComponent = toNmsComponent(adventureComponent);
            if (nmsComponent == null) {
                return;
            }
            Class<?> textDisplayClass = Reflection.nmsClass("Display$TextDisplay",
                    "net.minecraft.world.entity.Display$TextDisplay");
            if (textDisplayClass == null) {
                return;
            }
            Object accessor = findTextAccessor(textDisplayClass);
            if (accessor == null) {
                if (!textAccessorFailureLogged) {
                    textAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "DATA_TEXT_ID accessor on " + textDisplayClass.getName() + " - hologram "
                            + "lines will not show any text.");
                    logAllStaticFields(textDisplayClass);
                }
                return;
            }
            setAccessorValue(entityHandle, accessor, nmsComponent);
        } catch (Throwable ignored) {
        }
    }

    /** Forces the billboard render mode to CENTER (always face the viewer) - the vanilla default
     * is FIXED, so this must be set explicitly or the hologram never rotates to face anyone. */
    static void setBillboardCenter(Object entityHandle) {
        if (entityHandle == null) {
            return;
        }
        try {
            Class<?> displayClass = Reflection.nmsClass("Display", "net.minecraft.world.entity.Display");
            if (displayClass == null) {
                return;
            }
            Object accessor = findBillboardAccessor(displayClass);
            if (accessor == null) {
                if (!billboardAccessorFailureLogged) {
                    billboardAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "billboard accessor on " + displayClass.getName() + " - the hologram will "
                            + "not rotate to face the viewer (text will still show).");
                    logAllStaticFields(displayClass);
                }
                return;
            }
            Class<?> billboardEnumClass = Reflection.nmsClass("Display$BillboardConstraints",
                    "net.minecraft.world.entity.Display$BillboardConstraints");
            if (billboardEnumClass == null) {
                return;
            }
            Object center = Enum.valueOf(billboardEnumClass.asSubclass(Enum.class), "CENTER");
            // DATA_BILLBOARD_RENDER_CONSTRAINTS_ID es EntityDataAccessor<Byte> - el enum se
            // serializa como su id numérico (BillboardConstraints#getId()), nunca la instancia
            // del enum en crudo. Meter el enum directo rompía el codec en el envío del paquete
            // (ClassCastException: BillboardConstraints cannot be cast to Byte), confirmado en
            // producción 2026-08-15.
            Object idValue = invokeNoArg(center, "getId");
            byte billboardId = idValue instanceof Number
                    ? ((Number) idValue).byteValue()
                    : (byte) ((Enum<?>) center).ordinal();
            setAccessorValue(entityHandle, accessor, billboardId);
        } catch (Throwable ignored) {
        }
    }

    static void setScale(Object entityHandle, float scale) {
        if (entityHandle == null) {
            return;
        }
        try {
            Class<?> displayClass = Reflection.nmsClass("Display", "net.minecraft.world.entity.Display");
            if (displayClass == null) {
                return;
            }
            Object accessor = findScaleAccessor(displayClass);
            if (accessor == null) {
                if (!scaleAccessorFailureLogged) {
                    scaleAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "DATA_SCALE_ID accessor on " + displayClass.getName() + " - hologram scale "
                            + "will stay at the default (1.0).");
                    logAllStaticFields(displayClass);
                }
                return;
            }
            Class<?> vector3fClass = Reflection.findClass("org.joml.Vector3f");
            if (vector3fClass == null) {
                return;
            }
            Constructor<?> constructor = vector3fClass.getDeclaredConstructor(float.class, float.class, float.class);
            constructor.setAccessible(true);
            Object vector = constructor.newInstance(scale, scale, scale);
            setAccessorValue(entityHandle, accessor, vector);
        } catch (Throwable ignored) {
        }
    }

    static void setBackground(Object entityHandle, int argb) {
        if (entityHandle == null) {
            return;
        }
        try {
            Class<?> textDisplayClass = Reflection.nmsClass("Display$TextDisplay",
                    "net.minecraft.world.entity.Display$TextDisplay");
            if (textDisplayClass == null) {
                return;
            }
            Object accessor = findBackgroundAccessor(textDisplayClass);
            if (accessor == null) {
                if (!backgroundAccessorFailureLogged) {
                    backgroundAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "DATA_BACKGROUND_COLOR_ID accessor on " + textDisplayClass.getName() + " - hologram "
                            + "background will stay at the vanilla default.");
                    logAllStaticFields(textDisplayClass);
                }
                return;
            }
            setAccessorValue(entityHandle, accessor, argb);
        } catch (Throwable ignored) {
        }
    }

    static void setStyleFlags(Object entityHandle, byte flags) {
        if (entityHandle == null) {
            return;
        }
        try {
            Class<?> textDisplayClass = Reflection.nmsClass("Display$TextDisplay",
                    "net.minecraft.world.entity.Display$TextDisplay");
            if (textDisplayClass == null) {
                return;
            }
            Object accessor = findStyleFlagsAccessor(textDisplayClass);
            if (accessor == null) {
                if (!styleFlagsAccessorFailureLogged) {
                    styleFlagsAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "DATA_STYLE_FLAGS_ID accessor on " + textDisplayClass.getName() + " - hologram "
                            + "text shadow will stay at the vanilla default.");
                    logAllStaticFields(textDisplayClass);
                }
                return;
            }
            setAccessorValue(entityHandle, accessor, flags);
        } catch (Throwable ignored) {
        }
    }

    static void setTeleportDuration(Object entityHandle, int ticks) {
        if (entityHandle == null) {
            return;
        }
        try {
            Class<?> displayClass = Reflection.nmsClass("Display", "net.minecraft.world.entity.Display");
            if (displayClass == null) {
                return;
            }
            Object accessor = findTeleportDurationAccessor(displayClass);
            if (accessor == null) {
                if (!teleportDurationAccessorFailureLogged) {
                    teleportDurationAccessorFailureLogged = true;
                    Logger.getLogger("BlueFoundation").warning("[HologramPackets] could not find the "
                            + "DATA_TELEPORT_DURATION_ID accessor on " + displayClass.getName() + " - a moving "
                            + "hologram will snap to its new position instead of interpolating.");
                    logAllStaticFields(displayClass);
                }
                return;
            }
            setAccessorValue(entityHandle, accessor, Math.max(0, ticks));
        } catch (Throwable ignored) {
        }
    }

    static void sendAddEntity(Player viewer, Object entityHandle) {
        if (entityHandle == null || !Version.isAtLeast(1, 19, 4)) {
            return;
        }
        Object packet = newAddEntityPacket(entityHandle);
        if (packet != null) {
            Reflection.sendPacket(viewer, packet);
        }
    }

    static void sendMetadata(Player viewer, Object entityHandle) {
        if (entityHandle == null) {
            return;
        }
        try {
            int entityId = resolveEntityId(entityHandle);
            Object dataWatcher = invokeEither(entityHandle, "getEntityData", "getDataWatcher");

            Object packet;
            if (Version.isAtLeast(1, 19, 3)) {
                Class<?> packetClass = Reflection.nmsClass("ClientboundSetEntityDataPacket",
                        "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
                Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, List.class);
                constructor.setAccessible(true);
                List<?> packed = packedEntityData(dataWatcher);
                packet = constructor.newInstance(entityId, packed);
            } else {
                Class<?> packetClass = Reflection.nmsClass("PacketPlayOutEntityMetadata");
                Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, getDataWatcherClass(), boolean.class);
                constructor.setAccessible(true);
                packet = constructor.newInstance(entityId, dataWatcher, true);
            }
            Reflection.sendPacket(viewer, packet);
        } catch (Throwable ignored) {
        }
    }

    static void sendTeleport(Player viewer, Object entityHandle, Location location) {
        if (entityHandle == null || location == null) {
            return;
        }
        updatePosition(entityHandle, location);
        Object packet;
        if (Version.isAtLeast(1, 21)) {
            packet = newModernTeleport(entityHandle);
        } else {
            packet = newPacket("ClientboundTeleportEntityPacket", "PacketPlayOutEntityTeleport", entityHandle);
        }
        Reflection.sendPacket(viewer, packet);
    }

    static void sendDestroy(Player viewer, int entityId) {
        Object packet = newModernDestroy(entityId);
        Reflection.sendPacket(viewer, packet);
    }

    // ─── Component conversion ──────────────────────────────────────────────

    private static Object toNmsComponent(Component adventureComponent) {
        try {
            Class<?> paperAdventureClass = Reflection.findClass("io.papermc.paper.adventure.PaperAdventure");
            if (paperAdventureClass == null) {
                return null;
            }
            Method asVanilla = paperAdventureClass.getMethod("asVanilla", Component.class);
            asVanilla.setAccessible(true);
            return asVanilla.invoke(null, adventureComponent);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ─── Accessor lookup (name first, structural fallback) ────────────────

    private static Object findTextAccessor(Class<?> textDisplayClass) {
        Object byName = findStaticFieldValue(textDisplayClass, "DATA_TEXT_ID");
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        Class<?> componentClass = Reflection.findClass("net.minecraft.network.chat.Component");
        return findAccessorByShape(textDisplayClass, componentClass, false);
    }

    private static Object findBillboardAccessor(Class<?> displayClass) {
        Object byName = findStaticFieldValue(displayClass, "DATA_BILLBOARD_RENDER_CONSTRAINTS_ID");
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        return findAccessorByShape(displayClass, Byte.class, true);
    }

    private static Object findScaleAccessor(Class<?> displayClass) {
        Object byName = findStaticFieldValue(displayClass, "DATA_SCALE_ID");
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        Class<?> vector3fClass = Reflection.findClass("org.joml.Vector3f");
        return findAccessorByShapeUnambiguous(displayClass, vector3fClass, true, "scale (Vector3f)");
    }

    private static Object findBackgroundAccessor(Class<?> textDisplayClass) {
        Object byName = findStaticFieldValue(textDisplayClass, "DATA_BACKGROUND_COLOR_ID");
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        return findAccessorByShapeUnambiguous(textDisplayClass, Integer.class, true, "background color (Integer)");
    }

    private static Object findStyleFlagsAccessor(Class<?> textDisplayClass) {
        Object byName = findStaticFieldValue(textDisplayClass, "DATA_STYLE_FLAGS_ID");
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        return findAccessorByShapeUnambiguous(textDisplayClass, Byte.class, true, "style flags (Byte)");
    }

    private static Object findTeleportDurationAccessor(Class<?> displayClass) {
        // "DATA_TELEPORT_DURATION_ID" on 1.20.2+; "DATA_POS_ROT_INTERPOLATION_DURATION_ID" was the
        // earlier name for the same field.
        Object byName = findStaticFieldValue(displayClass, "DATA_TELEPORT_DURATION_ID");
        if (byName == null) {
            byName = findStaticFieldValue(displayClass, "DATA_POS_ROT_INTERPOLATION_DURATION_ID");
        }
        if (byName != null && isEntityDataAccessor(byName)) {
            return byName;
        }
        // Display declares several EntityDataAccessor<Integer> (interpolation duration, start delta,
        // teleport duration...) so a by-shape lookup is ambiguous - it will return null and the
        // caller logs once. The by-name path works on modern Paper (mojmap runtime).
        return findAccessorByShapeUnambiguous(displayClass, Integer.class, true, "teleport duration (Integer)");
    }

    /** One-shot diagnostic dump of every declared static field's name and generic type - used
     * when neither the by-name nor the by-shape lookup finds an accessor, so the next server log
     * shows exactly what field names this NMS build really uses instead of guessing blind. */
    private static void logAllStaticFields(Class<?> declaringClass) {
        if (declaringClass == null) {
            return;
        }
        StringBuilder dump = new StringBuilder();
        for (Field field : declaringClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (dump.length() > 0) {
                dump.append(", ");
            }
            dump.append(field.getName()).append('(').append(field.getGenericType()).append(')');
        }
        Logger.getLogger("BlueFoundation").warning("[HologramPackets] static fields on "
                + declaringClass.getName() + ": " + dump);
    }

    private static Object findStaticFieldValue(Class<?> declaringClass, String fieldName) {
        if (declaringClass == null) {
            return null;
        }
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            if (!Modifier.isStatic(field.getModifiers())) {
                return null;
            }
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Scans {@code declaringClass}'s own declared static fields (not superclasses - each
     * Display subtype declares its own accessors) for an {@code EntityDataAccessor<T>} whose
     * generic type argument matches {@code expectedType}. */
    private static Object findAccessorByShape(Class<?> declaringClass, Class<?> expectedType, boolean exactMatch) {
        if (declaringClass == null || expectedType == null) {
            return null;
        }
        for (Field field : declaringClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !matchesGenericType(field, expectedType, exactMatch)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null && isEntityDataAccessor(value)) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object findAccessorByShapeUnambiguous(Class<?> declaringClass, Class<?> expectedType, boolean exactMatch, String label) {
        if (declaringClass == null || expectedType == null) {
            return null;
        }
        List<Field> candidates = new ArrayList<>();
        for (Field field : declaringClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && matchesGenericType(field, expectedType, exactMatch)) {
                candidates.add(field);
            }
        }
        if (candidates.size() == 1) {
            try {
                Field field = candidates.get(0);
                field.setAccessible(true);
                Object value = field.get(null);
                if (value != null && isEntityDataAccessor(value)) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
        if (candidates.size() > 1) {
            StringBuilder names = new StringBuilder();
            for (Field field : candidates) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(field.getName());
            }
            Logger.getLogger("BlueFoundation").warning("[HologramPackets] " + candidates.size() + " ambiguous "
                    + label + " candidates on " + declaringClass.getName() + ": " + names);
        }
        return null;
    }

    private static boolean matchesGenericType(Field field, Class<?> expectedType, boolean exactMatch) {
        try {
            java.lang.reflect.Type generic = field.getGenericType();
            if (!(generic instanceof ParameterizedType)) {
                return false;
            }
            java.lang.reflect.Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
            if (args.length != 1 || !(args[0] instanceof Class)) {
                return false;
            }
            Class<?> actual = (Class<?>) args[0];
            return exactMatch ? actual == expectedType : expectedType.isAssignableFrom(actual) || actual.isAssignableFrom(expectedType);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Confirmed in production (2026-08-15): on this server's NMS build {@code EntityDataAccessor}
     * is a record, so the accessor method is {@code id()}, not the classic bean-style
     * {@code getId()} - the real fields (DATA_TEXT_ID etc.) DID exist under their expected name
     * and generic type (confirmed via the static-field dump), it was this check alone rejecting
     * every real accessor. Same class of bug already hit twice for GameProfile/Property in
     * SkinMirrorBridge - never decide "is this a bean or a record" by which method name resolves,
     * try both. */
    private static boolean isEntityDataAccessor(Object value) {
        if (value instanceof Number) {
            return false;
        }
        return hasNoArgIntMethod(value.getClass(), "getId") || hasNoArgIntMethod(value.getClass(), "id");
    }

    private static boolean hasNoArgIntMethod(Class<?> clazz, String name) {
        try {
            Method method = clazz.getMethod(name);
            return method.getReturnType() == int.class;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void setAccessorValue(Object entityHandle, Object accessor, Object value) {
        try {
            Object dataWatcher = invokeEither(entityHandle, "getEntityData", "getDataWatcher");
            Method set = findMethod(dataWatcher.getClass(), "set", accessor.getClass(), Object.class);
            if (set == null) {
                return;
            }
            set.setAccessible(true);
            set.invoke(dataWatcher, accessor, value);
        } catch (Throwable ignored) {
        }
    }

    // ─── Generic packet construction (adapted from the non-player branch of NpcPackets) ───

    private static Object newAddEntityPacket(Object entityHandle) {
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
                Object updateInterval = Reflection.serverEntityUpdateInterval(params[2]);
                if (updateInterval == null) {
                    continue;
                }
                if (!isAssignable(params[3], boolean.class)) {
                    continue;
                }
                Object fifthArg;
                if (isAssignable(Class.forName("java.util.function.Consumer"), params[4])) {
                    fifthArg = (java.util.function.Consumer<Object>) packet -> {
                    };
                } else if (params[4].isInterface()) {
                    fifthArg = Proxy.newProxyInstance(params[4].getClassLoader(), new Class<?>[]{params[4]},
                            (proxy, method, args) -> null);
                } else {
                    continue;
                }
                Object[] args = new Object[params.length];
                args[0] = level;
                args[1] = entityHandle;
                args[2] = updateInterval;
                args[3] = false;
                args[4] = fifthArg;
                if (params.length == 6) {
                    if (!java.util.Set.class.isAssignableFrom(params[5])) {
                        continue;
                    }
                    args[5] = Collections.emptySet();
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

    private static Object newModernTeleport(Object entityHandle) {
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
            Object change = of.invoke(null, entityHandle);
            // 4 args, not 3 - último boolean es "onGround" (irrelevante para una entidad fantasma
            // que nunca toca el suelo, se pasa false directo en vez de leerlo del handle).
            Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, positionMoveRotationClass,
                    java.util.Set.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(intValue(entityHandle, "getId"), change, Collections.emptySet(), false);
        } catch (Throwable ignored) {
        }
        return null;
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
                        return constructor.newInstance((Object) new int[]{entityId});
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

    private static Object newPacket(String modernName, String legacyName, Object... args) {
        try {
            Class<?> packetClass = Reflection.nmsClass(legacyName, modernName);
            if (packetClass == null) {
                return null;
            }
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }
            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length != args.length) {
                    continue;
                }
                boolean matches = true;
                for (int i = 0; i < params.length; i++) {
                    if (!isAssignable(params[i], argTypes[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Keeps a line's detached entity handle in sync with the anchor even when there are no viewers,
     * so a viewer joining later spawns the line at the hologram's current position. */
    static void updateEntityPosition(Object entityHandle, Location location) {
        if (entityHandle != null && location != null) {
            updatePosition(entityHandle, location);
        }
    }

    private static void updatePosition(Object entityHandle, Location location) {
        try {
            Method setPos = findMethod(entityHandle.getClass(), "setPos", double.class, double.class, double.class);
            if (setPos != null) {
                setPos.setAccessible(true);
                setPos.invoke(entityHandle, location.getX(), location.getY(), location.getZ());
            }
            Method setRot = findMethod(entityHandle.getClass(), "setRot", float.class, float.class);
            if (setRot != null) {
                setRot.setAccessible(true);
                setRot.invoke(entityHandle, location.getYaw(), location.getPitch());
            }
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> getDataWatcherClass() {
        return Reflection.nmsClass("DataWatcher", "net.minecraft.network.syncher.SynchedEntityData");
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

    private static UUID uuidValue(Object target) {
        Object value = invokeNoArg(target, "getUUID");
        return value instanceof UUID ? (UUID) value : UUID.randomUUID();
    }

    private static Object vec3Value(Object entityHandle) {
        Object movement = invokeNoArg(entityHandle, "getDeltaMovement");
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
