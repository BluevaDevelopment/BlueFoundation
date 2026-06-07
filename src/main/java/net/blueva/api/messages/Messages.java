package net.blueva.api.messages;

import net.blueva.api.reflection.Reflection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Multi-version player messaging helpers. */
public class Messages {

    protected Messages() {
    }

    public static String color(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(Player player, String message) {
        if (player != null) {
            player.sendMessage(color(message));
        }
    }

    public static boolean actionBar(Player player, String message) {
        if (player == null) {
            return false;
        }
        String colored = color(message);
        return sendActionBarViaSpigot(player, colored) || sendActionBarViaLegacyPacket(player, colored);
    }

    public static boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return false;
        }
        String coloredTitle = color(title);
        String coloredSubtitle = color(subtitle);
        return sendTitleViaBukkit(player, coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut)
                || sendTitleViaSimpleBukkit(player, coloredTitle, coloredSubtitle)
                || sendTitleViaLegacyPackets(player, coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
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
            Class<?> chatComponentText = Reflection.nmsClass("ChatComponentText");
            Class<?> iChatBaseComponent = Reflection.nmsClass("IChatBaseComponent");
            Class<?> packetPlayOutChat = Reflection.nmsClass("PacketPlayOutChat");
            if (chatComponentText == null || iChatBaseComponent == null || packetPlayOutChat == null) {
                return false;
            }

            Object component = chatComponentText.getConstructor(String.class).newInstance(message);
            Constructor<?> constructor = packetPlayOutChat.getConstructor(iChatBaseComponent, byte.class);
            Object packet = constructor.newInstance(component, (byte) 2);
            return Reflection.sendPacket(player, packet);
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
            Class<?> chatComponentText = Reflection.nmsClass("ChatComponentText");
            Class<?> iChatBaseComponent = Reflection.nmsClass("IChatBaseComponent");
            Class<?> packetPlayOutTitle = Reflection.nmsClass("PacketPlayOutTitle");
            Class<?> enumTitleAction = Reflection.nmsClass("PacketPlayOutTitle$EnumTitleAction");
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
            Reflection.sendPacket(player, timesConstructor.newInstance(timesAction, emptyComponent, fadeIn, stay, fadeOut));

            if (!isBlank(title)) {
                Reflection.sendPacket(player, messageConstructor.newInstance(titleAction, textConstructor.newInstance(title)));
            }

            if (!isBlank(subtitle)) {
                Reflection.sendPacket(player, messageConstructor.newInstance(subtitleAction, textConstructor.newInstance(subtitle)));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
