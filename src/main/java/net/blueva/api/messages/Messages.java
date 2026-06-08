package net.blueva.api.messages;

import net.blueva.api.reflection.Reflection;
import net.blueva.api.text.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;

/** Adventure/MiniMessage player messaging helpers. */
public class Messages {

    private static BukkitAudiences bukkitAudiences;

    protected Messages() {
    }

    /**
     * Initializes Adventure transport for Spigot/Bukkit.
     *
     * <p>Call {@code BlueAPI.Dependencies.loadAdventure(plugin)} before this
     * method on servers that do not provide Adventure natively.</p>
     */
    public static void init(JavaPlugin plugin) {
        if (plugin == null || hasNativeAudience()) {
            return;
        }
        close();
        bukkitAudiences = BukkitAudiences.create(plugin);
    }

    public static void close() {
        if (bukkitAudiences != null) {
            bukkitAudiences.close();
            bukkitAudiences = null;
        }
    }

    public static Component component(String message) {
        return Text.component(message);
    }

    /**
     * Serializes MiniMessage/Adventure input to a legacy section string for
     * Bukkit APIs that still only accept strings (inventory titles, item meta,
     * old packet constructors, etc.).
     */
    public static String legacy(String message) {
        return Text.legacySection(message);
    }

    public static void send(Player player, String message) {
        send((CommandSender) player, message);
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null) {
            return;
        }

        Component component = Text.component(message);
        Audience audience = audience(sender);
        if (audience != null) {
            audience.sendMessage(component);
            return;
        }

        sender.sendMessage(Text.legacySection(component));
    }

    public static boolean actionBar(Player player, String message) {
        if (player == null) {
            return false;
        }

        Component component = Text.component(message);
        Audience audience = audience(player);
        if (audience != null) {
            audience.sendActionBar(component);
            return true;
        }

        String legacy = Text.legacySection(component);
        return sendActionBarViaSpigot(player, legacy) || sendActionBarViaLegacyPacket(player, legacy);
    }

    public static boolean title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return false;
        }

        Component titleComponent = Text.component(title);
        Component subtitleComponent = Text.component(subtitle);
        Audience audience = audience(player);
        if (audience != null) {
            audience.showTitle(Title.title(titleComponent, subtitleComponent, titleTimes(fadeIn, stay, fadeOut)));
            return true;
        }

        String legacyTitle = Text.legacySection(titleComponent);
        String legacySubtitle = Text.legacySection(subtitleComponent);
        return sendTitleViaBukkit(player, legacyTitle, legacySubtitle, fadeIn, stay, fadeOut)
                || sendTitleViaSimpleBukkit(player, legacyTitle, legacySubtitle)
                || sendTitleViaLegacyPackets(player, legacyTitle, legacySubtitle, fadeIn, stay, fadeOut);
    }

    public static Audience audience(Player player) {
        return audience((CommandSender) player);
    }

    public static Audience audience(CommandSender sender) {
        if (sender == null) {
            return null;
        }
        if (sender instanceof Audience) {
            return (Audience) sender;
        }
        if (bukkitAudiences != null) {
            return bukkitAudiences.sender(sender);
        }
        return null;
    }

    public static Audience console() {
        return audience(Bukkit.getConsoleSender());
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

    private static Title.Times titleTimes(int fadeIn, int stay, int fadeOut) {
        Duration fadeInDuration = ticks(fadeIn);
        Duration stayDuration = ticks(stay);
        Duration fadeOutDuration = ticks(fadeOut);
        try {
            return (Title.Times) Title.Times.class
                    .getMethod("of", Duration.class, Duration.class, Duration.class)
                    .invoke(null, fadeInDuration, stayDuration, fadeOutDuration);
        } catch (Throwable ignored) {
            return Title.Times.times(fadeInDuration, stayDuration, fadeOutDuration);
        }
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private static boolean hasNativeAudience() {
        try {
            return Bukkit.getConsoleSender() instanceof Audience;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
