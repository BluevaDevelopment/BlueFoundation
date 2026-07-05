package net.blueva.foundation.scoreboard;

import net.blueva.foundation.reflection.Reflection;
import net.blueva.foundation.version.Version;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reflection cache for scoreboard packets across Minecraft versions.
 */
final class ScoreboardReflection {

    static final String[] COLOR_CODES = Arrays.stream(ChatColor.values())
            .map(Object::toString)
            .toArray(String[]::new);

    private static final boolean NMS_REPACKAGED;
    private static final boolean MOJANG_MAPPINGS;
    private static final VersionType VERSION_TYPE;

    private static final Class<?> CHAT_COMPONENT_CLASS;
    private static final Class<?> DISPLAY_SLOT_TYPE;
    private static final Object SIDEBAR_DISPLAY_SLOT;
    private static final Object ENUM_SB_HEALTH_DISPLAY_INTEGER;
    private static final Class<?> ENUM_SB_ACTION;
    private static final Object ENUM_SB_ACTION_CHANGE;
    private static final Object ENUM_SB_ACTION_REMOVE;
    private static final Object DUMMY_SCOREBOARD_CRITERIA;
    private static final Object BLANK_NUMBER_FORMAT;

    private static final MethodHandle PLAYER_GET_HANDLE;
    private static final MethodHandle PLAYER_CONNECTION;
    private static final MethodHandle SEND_PACKET;

    private static final MethodHandle OBJECTIVE_CONSTRUCTOR;
    private static final MethodHandle PACKET_SB_OBJ;
    private static final MethodHandle PACKET_SB_DISPLAY_OBJ;
    private static final MethodHandle PACKET_SB_SET_SCORE;
    private static final MethodHandle PACKET_SB_RESET_SCORE;
    private static final MethodHandle PACKET_SB_TEAM;
    private static final MethodHandle PACKET_SB_SERIALIZABLE_TEAM;
    private static final MethodHandle PLAYER_TEAM_CONSTRUCTOR;
    private static final MethodHandle FIXED_NUMBER_FORMAT;

    private static final boolean SCORE_OPTIONAL_COMPONENTS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            NMS_REPACKAGED = Reflection.classExists("net.minecraft.network.protocol.Packet");
            MOJANG_MAPPINGS = Reflection.classExists("net.minecraft.network.chat.Component");

            if (NMS_REPACKAGED) {
                VERSION_TYPE = VersionType.V1_17;
            } else if (Reflection.classExists(nmsClassName(null, "ScoreboardServer$Action"))
                    || Reflection.classExists(nmsClassName(null, "ServerScoreboard$Method"))) {
                VERSION_TYPE = VersionType.V1_13;
            } else if (Reflection.classExists(nmsClassName(null, "IScoreboardCriteria$EnumScoreboardHealthDisplay"))
                    || Reflection.classExists(nmsClassName(null, "ObjectiveCriteria$RenderType"))) {
                VERSION_TYPE = VersionType.V1_8;
            } else {
                VERSION_TYPE = VersionType.V1_7;
            }

            Class<?> craftPlayerClass = Reflection.craftBukkitClass("entity.CraftPlayer");
            Class<?> entityPlayerClass = nmsClass("server.level", "EntityPlayer", "ServerPlayer");
            Class<?> playerConnectionClass = nmsClass("server.network", "PlayerConnection", "ServerGamePacketListenerImpl");
            Class<?> packetClass = nmsClass("network.protocol", "Packet");

            Class<?> packetSbObjClass = nmsClass("network.protocol.game", "PacketPlayOutScoreboardObjective", "ClientboundSetObjectivePacket");
            Class<?> packetSbDisplayObjClass = nmsClass("network.protocol.game", "PacketPlayOutScoreboardDisplayObjective", "ClientboundSetDisplayObjectivePacket");
            Class<?> packetSbScoreClass = nmsClass("network.protocol.game", "PacketPlayOutScoreboardScore", "ClientboundSetScorePacket");
            Class<?> packetSbTeamClass = nmsClass("network.protocol.game", "PacketPlayOutScoreboardTeam", "ClientboundSetPlayerTeamPacket");
            Class<?> sbTeamClass = VERSION_TYPE.isHigherOrEqual(VersionType.V1_17)
                    ? innerClass(packetSbTeamClass, inner -> !inner.isEnum()) : null;

            CHAT_COMPONENT_CLASS = nmsClass("network.chat", "IChatBaseComponent", "Component");

            Field playerConnectionField = Arrays.stream(entityPlayerClass.getFields())
                    .filter(field -> field.getType().isAssignableFrom(playerConnectionClass))
                    .findFirst().orElseThrow(NoSuchFieldException::new);
            Method sendPacketMethod = Stream.concat(
                            Arrays.stream(playerConnectionClass.getSuperclass().getMethods()),
                            Arrays.stream(playerConnectionClass.getMethods())
                    )
                    .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == packetClass)
                    .findFirst().orElseThrow(NoSuchMethodException::new);

            Optional<Class<?>> displaySlotEnum = optionalNmsClass("world.scores", "DisplaySlot");
            DISPLAY_SLOT_TYPE = displaySlotEnum.orElse(int.class);
            SIDEBAR_DISPLAY_SLOT = displaySlotEnum.isPresent()
                    ? enumValueOf(DISPLAY_SLOT_TYPE, "SIDEBAR", 1)
                    : 1;

            PLAYER_GET_HANDLE = lookup.findVirtual(craftPlayerClass, "getHandle", MethodType.methodType(entityPlayerClass));
            PLAYER_CONNECTION = lookup.unreflectGetter(playerConnectionField);
            SEND_PACKET = lookup.unreflect(sendPacketMethod);

            Class<?> scoreboardClass = nmsClass("world.scores", "Scoreboard");
            Class<?> playerTeamClass = nmsClass("world.scores", "ScoreboardTeam", "PlayerTeam");
            Class<?> objectiveClass = nmsClass("world.scores", "ScoreboardObjective", "Objective");
            Class<?> objectiveCriteriaClass = nmsClass("world.scores.criteria", "IScoreboardCriteria", "ObjectiveCriteria");
            PLAYER_TEAM_CONSTRUCTOR = lookup.unreflectConstructor(playerTeamClass.getConstructor(scoreboardClass, String.class));

            Class<?> objectiveRenderTypeClass = optionalNmsClass("world.scores.criteria", "IScoreboardCriteria$EnumScoreboardHealthDisplay", "ObjectiveCriteria$RenderType").orElse(null);

            Optional<Class<?>> numberFormat = optionalNmsClass("network.chat.numbers", "NumberFormat");
            MethodHandle packetSbSetScore;
            MethodHandle packetSbResetScore = null;
            MethodHandle fixedFormatConstructor = null;
            Object blankNumberFormat = null;
            boolean scoreOptionalComponents = false;

            if (numberFormat.isPresent()) { // 1.20.3+
                OBJECTIVE_CONSTRUCTOR = lookup.unreflectConstructor(objectiveClass.getConstructor(
                        scoreboardClass, String.class, objectiveCriteriaClass, CHAT_COMPONENT_CLASS,
                        objectiveRenderTypeClass, boolean.class, numberFormat.get()));
                PACKET_SB_OBJ = lookup.unreflectConstructor(packetSbObjClass.getConstructor(objectiveClass, int.class));
                PACKET_SB_DISPLAY_OBJ = lookup.unreflectConstructor(packetSbDisplayObjClass.getConstructor(DISPLAY_SLOT_TYPE, objectiveClass));

                Class<?> blankFormatClass = nmsClass("network.chat.numbers", "BlankFormat");
                Class<?> fixedFormatClass = nmsClass("network.chat.numbers", "FixedFormat");
                Class<?> resetScoreClass = nmsClass("network.protocol.game", "ClientboundResetScorePacket");
                MethodType scoreType = MethodType.methodType(void.class, String.class, String.class, int.class, CHAT_COMPONENT_CLASS, numberFormat.get());
                MethodType scoreTypeOptional = MethodType.methodType(void.class, String.class, String.class, int.class, Optional.class, Optional.class);
                MethodType removeScoreType = MethodType.methodType(void.class, String.class, String.class);
                MethodType fixedFormatType = MethodType.methodType(void.class, CHAT_COMPONENT_CLASS);
                Optional<Field> blankField = Arrays.stream(blankFormatClass.getFields())
                        .filter(f -> f.getType() == blankFormatClass).findAny();
                Optional<MethodHandle> optionalScorePacket = optionalConstructor(packetSbScoreClass, lookup, scoreTypeOptional);
                fixedFormatConstructor = lookup.findConstructor(fixedFormatClass, fixedFormatType);
                packetSbSetScore = optionalScorePacket.isPresent() ? optionalScorePacket.get()
                        : lookup.findConstructor(packetSbScoreClass, scoreType);
                scoreOptionalComponents = optionalScorePacket.isPresent();
                packetSbResetScore = lookup.findConstructor(resetScoreClass, removeScoreType);
                blankNumberFormat = blankField.isPresent() ? blankField.get().get(null) : null;
            } else if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_17)) {
                Class<?> enumSbAction = nmsClass("server", "ScoreboardServer$Action", "ServerScoreboard$Method");
                MethodType scoreType = MethodType.methodType(void.class, enumSbAction, String.class, String.class, int.class);
                packetSbSetScore = lookup.findConstructor(packetSbScoreClass, scoreType);
                OBJECTIVE_CONSTRUCTOR = lookup.unreflectConstructor(objectiveClass.getConstructor(
                        scoreboardClass, String.class, objectiveCriteriaClass, CHAT_COMPONENT_CLASS, objectiveRenderTypeClass));
                PACKET_SB_OBJ = lookup.unreflectConstructor(packetSbObjClass.getConstructor(objectiveClass, int.class));
                PACKET_SB_DISPLAY_OBJ = lookup.unreflectConstructor(packetSbDisplayObjClass.getConstructor(int.class, objectiveClass));
            } else {
                packetSbSetScore = lookup.findConstructor(packetSbScoreClass, MethodType.methodType(void.class));
                if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)) {
                    OBJECTIVE_CONSTRUCTOR = lookup.unreflectConstructor(objectiveClass.getConstructor(
                            scoreboardClass, String.class, objectiveCriteriaClass, CHAT_COMPONENT_CLASS, objectiveRenderTypeClass));
                } else {
                    OBJECTIVE_CONSTRUCTOR = lookup.unreflectConstructor(objectiveClass.getConstructor(
                            scoreboardClass, String.class, objectiveCriteriaClass));
                }
                PACKET_SB_OBJ = lookup.unreflectConstructor(packetSbObjClass.getConstructor(objectiveClass, int.class));
                PACKET_SB_DISPLAY_OBJ = lookup.unreflectConstructor(packetSbDisplayObjClass.getConstructor(int.class, objectiveClass));
            }

            PACKET_SB_SET_SCORE = packetSbSetScore;
            PACKET_SB_RESET_SCORE = packetSbResetScore;
            Constructor<?> packetSbTeamConstructor = sbTeamClass != null
                    ? packetSbTeamClass.getDeclaredConstructor(String.class, int.class, Optional.class, Collection.class)
                    : packetSbTeamClass.getDeclaredConstructor();
            packetSbTeamConstructor.setAccessible(true);
            PACKET_SB_TEAM = lookup.unreflectConstructor(packetSbTeamConstructor);
            PACKET_SB_SERIALIZABLE_TEAM = sbTeamClass != null
                    ? lookup.unreflectConstructor(sbTeamClass.getConstructor(playerTeamClass))
                    : null;
            FIXED_NUMBER_FORMAT = fixedFormatConstructor;
            BLANK_NUMBER_FORMAT = blankNumberFormat;
            SCORE_OPTIONAL_COMPONENTS = scoreOptionalComponents;

            if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_8)) {
                String enumSbActionClass = VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)
                        ? "ScoreboardServer$Action"
                        : "PacketPlayOutScoreboardScore$EnumScoreboardAction";
                Class<?> enumHealthDisplay = nmsClass("world.scores.criteria", "IScoreboardCriteria$EnumScoreboardHealthDisplay", "ObjectiveCriteria$RenderType");
                Class<?> enumAction = optionalNmsClass("server", enumSbActionClass, "ServerScoreboard$Method").orElse(null);
                ENUM_SB_ACTION = enumAction;
                ENUM_SB_HEALTH_DISPLAY_INTEGER = enumValueOf(enumHealthDisplay, "INTEGER", 0);
                ENUM_SB_ACTION_CHANGE = enumAction != null ? enumValueOf(enumAction, "CHANGE", 0) : null;
                ENUM_SB_ACTION_REMOVE = enumAction != null ? enumValueOf(enumAction, "REMOVE", 1) : null;
            } else {
                ENUM_SB_ACTION = null;
                ENUM_SB_HEALTH_DISPLAY_INTEGER = null;
                ENUM_SB_ACTION_CHANGE = null;
                ENUM_SB_ACTION_REMOVE = null;
            }

            if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)) {
                DUMMY_SCOREBOARD_CRITERIA = null;
            } else {
                Class<?> scoreboardBaseCriteriaClass = nmsClass("world.scores.criteria", "ScoreboardBaseCriteria");
                DUMMY_SCOREBOARD_CRITERIA = scoreboardBaseCriteriaClass.getConstructor(String.class).newInstance("dummy");
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private ScoreboardReflection() {
    }

    static VersionType versionType() {
        return VERSION_TYPE;
    }

    static boolean customScoresSupported() {
        return BLANK_NUMBER_FORMAT != null;
    }

    static void sendPacket(org.bukkit.entity.Player player, Object packet) throws Throwable {
        Object entityPlayer = PLAYER_GET_HANDLE.invoke(player);
        Object playerConnection = PLAYER_CONNECTION.invoke(entityPlayer);
        SEND_PACKET.invoke(playerConnection, packet);
    }

    static Object newObjective(String id, Object displayName) throws Throwable {
        if (BLANK_NUMBER_FORMAT != null) {
            return OBJECTIVE_CONSTRUCTOR.invoke(null, id, null, displayName, ENUM_SB_HEALTH_DISPLAY_INTEGER, false, null);
        } else if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)) {
            return OBJECTIVE_CONSTRUCTOR.invoke(null, id, null, displayName, ENUM_SB_HEALTH_DISPLAY_INTEGER);
        }
        Object objective = OBJECTIVE_CONSTRUCTOR.invoke(null, id, DUMMY_SCOREBOARD_CRITERIA);
        setComponentField(objective, displayName, 1);
        return objective;
    }

    static Object objectivePacket(Object objective, ObjectiveMode mode) throws Throwable {
        return PACKET_SB_OBJ.invoke(objective, mode.ordinal());
    }

    static Object displayObjectivePacket(Object objective) throws Throwable {
        return PACKET_SB_DISPLAY_OBJ.invoke(SIDEBAR_DISPLAY_SLOT, objective);
    }

    static Object scorePacket(int score, ScoreboardAction action, String objectiveId, Object customNumberFormat) throws Throwable {
        String entry = COLOR_CODES[score];
        if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_17)) {
            return modernScorePacket(score, action, objectiveId, customNumberFormat);
        }

        Object packet = PACKET_SB_SET_SCORE.invoke();
        setField(packet, String.class, entry, 0);

        if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_8)) {
            Object enumAction = action == ScoreboardAction.REMOVE ? ENUM_SB_ACTION_REMOVE : ENUM_SB_ACTION_CHANGE;
            setField(packet, ENUM_SB_ACTION.getClass().getComponentType() != null
                    ? ENUM_SB_ACTION.getClass() : ENUM_SB_ACTION.getClass(), enumAction);
        } else {
            setField(packet, int.class, action.ordinal(), 1);
        }

        if (action == ScoreboardAction.CHANGE) {
            setField(packet, String.class, objectiveId, 1);
            setField(packet, int.class, score);
        }
        return packet;
    }

    private static Object modernScorePacket(int score, ScoreboardAction action, String objectiveId, Object customNumberFormat) throws Throwable {
        String entry = COLOR_CODES[score];
        if (PACKET_SB_RESET_SCORE == null) { // 1.17–1.20.2
            Object enumAction = action == ScoreboardAction.REMOVE ? ENUM_SB_ACTION_REMOVE : ENUM_SB_ACTION_CHANGE;
            return PACKET_SB_SET_SCORE.invoke(enumAction, objectiveId, entry, score);
        }

        if (action == ScoreboardAction.REMOVE) {
            return PACKET_SB_RESET_SCORE.invoke(entry, objectiveId);
        }

        Object format = customNumberFormat != null ? customNumberFormat : BLANK_NUMBER_FORMAT;
        return SCORE_OPTIONAL_COMPONENTS
                ? PACKET_SB_SET_SCORE.invoke(entry, objectiveId, score, Optional.empty(), Optional.of(format))
                : PACKET_SB_SET_SCORE.invoke(entry, objectiveId, score, null, format);
    }

    static Object teamPacket(String teamName, int score, TeamMode mode, Object prefix, Object suffix) throws Throwable {
        if (mode == TeamMode.REMOVE) {
            if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_17)) {
                return PACKET_SB_TEAM.invoke(teamName, mode.ordinal(), Optional.empty(), Collections.emptyList());
            }
            Object packet = PACKET_SB_TEAM.invoke();
            setField(packet, String.class, teamName);
            setField(packet, int.class, mode.ordinal(), VERSION_TYPE == VersionType.V1_8 ? 1 : 0);
            return packet;
        }

        String entry = COLOR_CODES[score];
        if (VERSION_TYPE.isHigherOrEqual(VersionType.V1_17)) {
            Object team = PLAYER_TEAM_CONSTRUCTOR.invoke(null, teamName);
            setComponentField(team, null, 1);   // display name
            setComponentField(team, prefix, 2); // prefix
            setComponentField(team, suffix, 3); // suffix
            Object serializableTeam = PACKET_SB_SERIALIZABLE_TEAM.invoke(team);
            return PACKET_SB_TEAM.invoke(teamName, mode.ordinal(), Optional.of(serializableTeam),
                    mode == TeamMode.CREATE ? Collections.singletonList(entry) : Collections.emptyList());
        }

        Object packet = PACKET_SB_TEAM.invoke();
        setField(packet, String.class, teamName);
        setField(packet, int.class, mode.ordinal(), VERSION_TYPE == VersionType.V1_8 ? 1 : 0);
        setComponentField(packet, prefix, 2);
        setComponentField(packet, suffix, 3);
        setField(packet, String.class, "always", 4); // visibility
        setField(packet, String.class, "always", 5); // collisions
        if (mode == TeamMode.CREATE) {
            setField(packet, Collection.class, Collections.singletonList(entry));
        }
        return packet;
    }

    static Object fixedNumberFormat(Object component) throws Throwable {
        if (FIXED_NUMBER_FORMAT == null) {
            return null;
        }
        return FIXED_NUMBER_FORMAT.invoke(component);
    }

    static Object blankNumberFormat() {
        return BLANK_NUMBER_FORMAT;
    }

    private static void setField(Object packet, Class<?> fieldType, Object value) throws ReflectiveOperationException {
        setField(packet, fieldType, value, 0);
    }

    private static void setField(Object packet, Class<?> fieldType, Object value, int count) throws ReflectiveOperationException {
        int i = 0;
        for (Field field : packet.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if (field.getType() == fieldType && count == i++) {
                field.set(packet, value);
                return;
            }
        }
    }

    private static void setComponentField(Object packet, Object value, int count) throws Throwable {
        if (!VERSION_TYPE.isHigherOrEqual(VersionType.V1_13)) {
            String line = value != null ? value.toString() : "";
            setField(packet, String.class, line, count);
            return;
        }
        Object component = value != null ? value : ScoreboardComponentConverter.emptyComponent();
        int i = 0;
        for (Field field : packet.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            if ((field.getType() == String.class || field.getType() == CHAT_COMPONENT_CLASS) && count == i++) {
                field.set(packet, component);
                return;
            }
        }
    }

    // --- reflection helpers ---

    private static Class<?> nmsClass(String post1_17package, String className) throws ClassNotFoundException {
        return Reflection.findClass(nmsClassName(post1_17package, className));
    }

    private static Class<?> nmsClass(String post1_17package, String spigotClass, String mojangClass) throws ClassNotFoundException {
        return Reflection.findClass(nmsClassName(post1_17package, MOJANG_MAPPINGS ? mojangClass : spigotClass));
    }

    private static Optional<Class<?>> optionalNmsClass(String post1_17package, String className) {
        return Optional.ofNullable(Reflection.findClass(nmsClassName(post1_17package, className)));
    }

    private static Optional<Class<?>> optionalNmsClass(String post1_17package, String spigotClass, String mojangClass) {
        return Optional.ofNullable(Reflection.findClass(nmsClassName(post1_17package, MOJANG_MAPPINGS ? mojangClass : spigotClass)));
    }

    private static String nmsClassName(String post1_17package, String className) {
        if (NMS_REPACKAGED) {
            String pkg = post1_17package == null ? "net.minecraft" : "net.minecraft." + post1_17package;
            return pkg + "." + className;
        }
        String obcPackage = Bukkit.getServer().getClass().getPackage().getName();
        String nmsPackage = obcPackage.replace("org.bukkit.craftbukkit", "net.minecraft.server");
        return nmsPackage + "." + className;
    }

    private static Class<?> innerClass(Class<?> parentClass, java.util.function.Predicate<Class<?>> predicate) throws ClassNotFoundException {
        for (Class<?> innerClass : parentClass.getDeclaredClasses()) {
            if (predicate.test(innerClass)) {
                return innerClass;
            }
        }
        throw new ClassNotFoundException("No inner class matches predicate in " + parentClass.getName());
    }

    private static Optional<MethodHandle> optionalConstructor(Class<?> declaringClass, MethodHandles.Lookup lookup, MethodType type) throws IllegalAccessException {
        try {
            return Optional.of(lookup.findConstructor(declaringClass, type));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private static Object enumValueOf(Class<?> enumClass, String name, int fallbackOrdinal) {
        try {
            return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            Object[] constants = enumClass.getEnumConstants();
            if (constants.length > fallbackOrdinal) {
                return constants[fallbackOrdinal];
            }
            throw e;
        }
    }

    enum VersionType {
        V1_7, V1_8, V1_13, V1_17;

        boolean isHigherOrEqual(VersionType other) {
            return ordinal() >= other.ordinal();
        }
    }

    enum ObjectiveMode {
        CREATE, REMOVE, UPDATE
    }

    enum TeamMode {
        CREATE, REMOVE, UPDATE
    }

    enum ScoreboardAction {
        CHANGE, REMOVE
    }
}
