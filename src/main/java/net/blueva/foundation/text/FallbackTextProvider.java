package net.blueva.foundation.text;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.LegacySerializer;
import net.blueva.foundation.text.serializer.PlainSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Text provider used when Adventure is not natively available.
 * Parses MiniMessage with BlueFoundation's own parser and sends messages via
 * Bukkit's legacy APIs or packets.
 */
final class FallbackTextProvider implements TextProvider {

    @Override
    public void send(CommandSender sender, String message) {
        if (sender == null) {
            return;
        }
        String legacy = legacySection(message);
        sender.sendMessage(legacy);
    }

    @Override
    public void broadcast(String message) {
        String legacy = legacySection(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(legacy);
        }
        Bukkit.getConsoleSender().sendMessage(legacy);
    }

    @Override
    public boolean actionBar(Player player, String message) {
        if (player == null) {
            return false;
        }
        String legacy = legacySection(message);
        return sendActionBarViaSpigot(player, legacy) || sendActionBarViaLegacyPacket(player, legacy);
    }

    @Override
    public boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return false;
        }
        String legacyTitle = legacySection(title);
        String legacySubtitle = legacySection(subtitle);
        return sendTitleViaBukkit(player, legacyTitle, legacySubtitle, fadeIn, stay, fadeOut)
                || sendTitleViaSimpleBukkit(player, legacyTitle, legacySubtitle)
                || sendTitleViaLegacyPackets(player, legacyTitle, legacySubtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void bossBar(Player player, String title, BossBarColor color, BossBarStyle style, float progress) {
        if (player == null) {
            return;
        }
        try {
            Class<?> barColorClass = Class.forName("org.bukkit.boss.BarColor");
            Class<?> barStyleClass = Class.forName("org.bukkit.boss.BarStyle");
            Class<?> bossBarClass = Class.forName("org.bukkit.boss.BossBar");
            Object barColor = Enum.valueOf((Class<Enum>) barColorClass.asSubclass(Enum.class), color.name());
            Object barStyle = Enum.valueOf((Class<Enum>) barStyleClass.asSubclass(Enum.class), style.name());
            Method createBossBar = Bukkit.class.getMethod("createBossBar", String.class, barColorClass, barStyleClass);
            Object bossBar = createBossBar.invoke(null, legacySection(title), barColor, barStyle);
            Method addPlayer = bossBarClass.getMethod("addPlayer", Player.class);
            addPlayer.invoke(bossBar, player);
            Method setProgress = bossBarClass.getMethod("setProgress", double.class);
            setProgress.invoke(bossBar, Math.max(0.0, Math.min(1.0, progress)));
            Method setVisible = bossBarClass.getMethod("setVisible", boolean.class);
            setVisible.invoke(bossBar, true);
        } catch (Throwable ignored) {
            // BossBar API unavailable (pre-1.9); fall back to chat message.
            send(player, title);
        }
    }

    @Override
    public String legacySection(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        BfComponent component = MiniMessageParser.parse(message);
        return LegacySerializer.serialize(component);
    }

    @Override
    public String plain(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        BfComponent component = MiniMessageParser.parse(message);
        return PlainSerializer.serialize(component);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean sendActionBarViaSpigot(Player player, String message) {
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = Enum.valueOf((Class<Enum>) chatMessageType.asSubclass(Enum.class), "ACTION_BAR");
            Class<?> baseComponent = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object component = textComponent.getConstructor(String.class).newInstance(message);

            try {
                Method single = spigot.getClass().getMethod("sendMessage", chatMessageType, baseComponent);
                single.invoke(spigot, actionBar, component);
                return true;
            } catch (Throwable ignored) {
            }

            Object components = Array.newInstance(baseComponent, 1);
            Array.set(components, 0, component);
            Method varargs = spigot.getClass().getMethod("sendMessage", chatMessageType, components.getClass());
            varargs.invoke(spigot, actionBar, components);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendActionBarViaLegacyPacket(Player player, String message) {
        try {
            Class<?> chatComponentText = nmsClass("ChatComponentText");
            Class<?> iChatBaseComponent = nmsClass("IChatBaseComponent");
            Class<?> packetPlayOutChat = nmsClass("PacketPlayOutChat");
            if (chatComponentText == null || iChatBaseComponent == null || packetPlayOutChat == null) {
                return false;
            }

            Object component = chatComponentText.getConstructor(String.class).newInstance(message);
            Constructor<?> constructor = packetPlayOutChat.getConstructor(iChatBaseComponent, byte.class);
            Object packet = constructor.newInstance(component, (byte) 2);
            return sendPacket(player, packet);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendTitleViaBukkit(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Method method = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            method.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendTitleViaSimpleBukkit(Player player, String title, String subtitle) {
        try {
            Method method = player.getClass().getMethod("sendTitle", String.class, String.class);
            method.invoke(player, title, subtitle);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean sendTitleViaLegacyPackets(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> chatComponentText = nmsClass("ChatComponentText");
            Class<?> iChatBaseComponent = nmsClass("IChatBaseComponent");
            Class<?> packetPlayOutTitle = nmsClass("PacketPlayOutTitle");
            Class<?> enumTitleAction = nmsClass("PacketPlayOutTitle$EnumTitleAction");
            if (chatComponentText == null || iChatBaseComponent == null || packetPlayOutTitle == null || enumTitleAction == null) {
                return false;
            }

            Object titleAction = Enum.valueOf((Class<Enum>) enumTitleAction.asSubclass(Enum.class), "TITLE");
            Object subtitleAction = Enum.valueOf((Class<Enum>) enumTitleAction.asSubclass(Enum.class), "SUBTITLE");
            Object timesAction = Enum.valueOf((Class<Enum>) enumTitleAction.asSubclass(Enum.class), "TIMES");

            Constructor<?> textConstructor = chatComponentText.getConstructor(String.class);
            Constructor<?> messageConstructor = packetPlayOutTitle.getConstructor(enumTitleAction, iChatBaseComponent);
            Constructor<?> timesConstructor = packetPlayOutTitle.getConstructor(enumTitleAction, iChatBaseComponent, int.class, int.class, int.class);

            Object emptyComponent = textConstructor.newInstance("");
            sendPacket(player, timesConstructor.newInstance(timesAction, emptyComponent, fadeIn, stay, fadeOut));

            if (!isBlank(title)) {
                sendPacket(player, messageConstructor.newInstance(titleAction, textConstructor.newInstance(title)));
            }

            if (!isBlank(subtitle)) {
                sendPacket(player, messageConstructor.newInstance(subtitleAction, textConstructor.newInstance(subtitle)));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> nmsClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.Packet"));
            sendPacket.invoke(connection, packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
