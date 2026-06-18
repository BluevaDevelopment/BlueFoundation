package net.blueva.api.npc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.blueva.api.npc.event.NpcClickEvent;
import net.blueva.api.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Injects a Netty handler into player channels to detect clicks on NPCs.
 *
 * <p>The handler is installed for every online player when the module is
 * initialized and for each player that joins afterwards.</p>
 */
final class NpcPacketListener {

    private static final String HANDLER_NAME = "blueapi-npc-interact";
    private static final Set<UUID> INJECTED = new HashSet<>();

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
            pipeline.addLast(HANDLER_NAME, new InterceptHandler(player));
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
                return null;
            }
            return (Channel) getFieldValue(networkManager, "channel");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean handlePacket(Player player, Object packet) {
        if (packet == null) {
            return false;
        }
        String className = packet.getClass().getSimpleName();
        if (!className.contains("UseEntity") && !className.contains("Interact")) {
            return false;
        }
        Integer entityId = getEntityId(packet);
        if (entityId == null) {
            return false;
        }
        NpcImpl npc = NpcRegistry.byEntityId(entityId);
        if (npc == null) {
            return false;
        }
        NpcClickEvent.ClickType clickType = detectClickType(packet);
        NpcClickEvent event = new NpcClickEvent(player, npc, clickType);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
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
            for (Field field : packet.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value == null) {
                    continue;
                }
                if (isActionType(value.getClass(), "ATTACK")) {
                    return NpcClickEvent.ClickType.LEFT;
                }
                if (isActionType(value.getClass(), "INTERACT")) {
                    return NpcClickEvent.ClickType.RIGHT;
                }
            }
        } catch (Throwable ignored) {
        }
        return NpcClickEvent.ClickType.RIGHT;
    }

    private static boolean isActionType(Class<?> clazz, String needle) {
        String upper = needle.toUpperCase();
        if (clazz.getSimpleName().toUpperCase().contains(upper)) {
            return true;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getSimpleName().toUpperCase().contains(upper)) {
                return true;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return isActionType(superclass, needle);
        }
        return false;
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
            if (handlePacket(player, msg)) {
                return;
            }
            super.channelRead(ctx, msg);
        }
    }
}
