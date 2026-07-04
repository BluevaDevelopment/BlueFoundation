package net.blueva.foundation.npc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.blueva.foundation.npc.event.NpcClickEvent;
import net.blueva.foundation.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects a Netty handler into player channels to detect clicks on NPCs.
 *
 * <p>The handler is installed for every online player when the module is
 * initialized and for each player that joins afterwards.</p>
 */
final class NpcPacketListener {

    private static final String HANDLER_NAME = "bluefoundation-npc-interact";
    private static final Set<UUID> INJECTED = new HashSet<>();
    private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();
    private static final long CLICK_COOLDOWN_MS = 250;

    private NpcPacketListener() {
    }

    static void injectAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    static void uninjectAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            uninject(player);
        }
        INJECTED.clear();
        LAST_CLICK.clear();
    }

    static void inject(Player player) {
        if (player == null || INJECTED.contains(player.getUniqueId())) {
            return;
        }
        try {
            Channel channel = getChannel(player);
            if (channel == null) {
                return;
            }
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                return;
            }
            if (pipeline.get("packet_handler") == null) {
                return;
            }
            pipeline.addBefore("packet_handler", HANDLER_NAME, new InterceptHandler(player));
            INJECTED.add(player.getUniqueId());
        } catch (Throwable ignored) {
        }
    }

    static void uninject(Player player) {
        if (player == null) {
            return;
        }
        try {
            Channel channel = getChannel(player);
            if (channel == null) {
                return;
            }
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (Throwable ignored) {
        }
        INJECTED.remove(player.getUniqueId());
        LAST_CLICK.remove(player.getUniqueId());
    }

    private static Channel getChannel(Player player) {
        try {
            Object handle = Reflection.getHandle(player);
            if (handle == null) {
                return null;
            }
            Object connection = getFieldValue(handle, "playerConnection", "connection");
            if (connection == null) {
                return null;
            }
            Object networkManager = getFieldValue(connection, "networkManager");
            if (networkManager == null) {
                // Modern versions: the listener has a 'connection' field of type Connection (formerly NetworkManager).
                networkManager = getFieldValue(connection, "connection");
            }
            if (networkManager == null) {
                return null;
            }
            return (Channel) getFieldValue(networkManager, "channel");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isNpcPacket(Object packet) {
        if (packet == null) {
            return false;
        }
        String className = packet.getClass().getSimpleName();
        if (!className.contains("UseEntity") && !className.contains("Interact") && !className.contains("Attack")) {
            return false;
        }
        if (detectClickType(packet) == null) {
            return false;
        }
        Integer entityId = getEntityId(packet);
        return entityId != null && NpcRegistry.byEntityId(entityId) != null;
    }

    private static void handlePacket(Player player, Object packet) {
        if (packet == null) {
            return;
        }

        // Anti double-click: clients can send the same interaction twice in a tick.
        long now = System.currentTimeMillis();
        Long last = LAST_CLICK.get(player.getUniqueId());
        if (last != null && now - last < CLICK_COOLDOWN_MS) {
            return;
        }
        LAST_CLICK.put(player.getUniqueId(), now);

        Integer entityId = getEntityId(packet);
        if (entityId == null) {
            return;
        }
        NpcImpl npc = NpcRegistry.byEntityId(entityId);
        if (npc == null) {
            return;
        }
        NpcClickEvent.ClickType clickType = detectClickType(packet);
        if (clickType == null) {
            return;
        }
        NpcClickEvent event = new NpcClickEvent(player, npc, clickType);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            npc.handleClick(event);
        }
    }

    private static Integer getEntityId(Object packet) {
        try {
            Field field = findField(packet.getClass(), "a", "entityId", "id");
            if (field != null) {
                field.setAccessible(true);
                return (Integer) field.get(packet);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static NpcClickEvent.ClickType detectClickType(Object packet) {
        try {
            String className = packet.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            if (className.contains("ATTACK")) {
                return NpcClickEvent.ClickType.LEFT;
            }
            if (!className.contains("USEENTITY") && !className.contains("INTERACT")) {
                return null;
            }

            Object action = resolveActionValue(packet);
            if (action != null) {
                NpcClickEvent.ClickType type = classifyAction(action);
                if (type != null) {
                    return type;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object resolveActionValue(Object packet) {
        // The action field is named "action" on recent versions and obfuscated on older ones.
        Field field = findField(packet.getClass(), "action", "b", "c", "d");
        if (field != null) {
            try {
                field.setAccessible(true);
                return field.get(packet);
            } catch (Throwable ignored) {
            }
        }
        // Fallback: scan declared fields for an enum or nested action-like object.
        for (Field f : packet.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object value = f.get(packet);
                if (value == null) {
                    continue;
                }
                String name = value.getClass().getSimpleName().toUpperCase(Locale.ROOT);
                if (name.contains("ACTION") || name.contains("INTERACT") || name.contains("ATTACK")
                        || value instanceof Enum) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static NpcClickEvent.ClickType classifyAction(Object action) {
        if (action == null) {
            return null;
        }

        // Modern versions (1.21+) use sealed/record action types such as
        // ServerboundInteractPacket$InteractionAction or simple inner classes.
        String className = action.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        if (className.contains("ATTACK")) {
            return NpcClickEvent.ClickType.LEFT;
        }
        if (className.contains("INTERACT_AT")) {
            return NpcClickEvent.ClickType.RIGHT;
        }
        if (className.contains("INTERACT")) {
            return NpcClickEvent.ClickType.RIGHT;
        }

        // Direct enum constant (modern/legacy versions use an enum for the action type).
        if (action instanceof Enum) {
            return classifyByName(((Enum<?>) action).name());
        }

        // Modern record-like actions expose hand and usingSecondaryAction fields.
        Boolean secondary = getBooleanField(action, "usingSecondaryAction");
        if (secondary != null && secondary) {
            return NpcClickEvent.ClickType.RIGHT;
        }
        Object hand = getFieldValue(action, "hand");
        if (hand != null) {
            // Presence of a hand (without attack) means a right-click interaction.
            return NpcClickEvent.ClickType.RIGHT;
        }

        String repr = action.toString().toUpperCase(Locale.ROOT);
        NpcClickEvent.ClickType type = classifyByName(repr);
        if (type != null) {
            return type;
        }

        // Inspect fields for wrapped enums/objects.
        for (Field field : action.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(action);
                if (value == null) {
                    continue;
                }
                if (value instanceof Enum) {
                    type = classifyByName(((Enum<?>) value).name());
                } else {
                    type = classifyByName(value.toString());
                }
                if (type != null) {
                    return type;
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static NpcClickEvent.ClickType classifyByName(String name) {
        if (name == null) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.contains("ATTACK")) {
            return NpcClickEvent.ClickType.LEFT;
        }
        if (upper.contains("INTERACT") && !upper.contains("INTERACT_AT")) {
            return NpcClickEvent.ClickType.RIGHT;
        }
        return null;
    }

    private static Boolean getBooleanField(Object target, String name) {
        try {
            Field field = findField(target.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getFieldValue(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = findField(target.getClass(), name);
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(target);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String... names) {
        while (clazz != null && clazz != Object.class) {
            for (String name : names) {
                try {
                    return clazz.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static final class InterceptHandler extends ChannelDuplexHandler {

        private final Player player;

        InterceptHandler(Player player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (isNpcPacket(msg)) {
                Bukkit.getScheduler().runTask(NpcManager.getPlugin(), () -> handlePacket(player, msg));
                return;
            }
            super.channelRead(ctx, msg);
        }
    }
}
