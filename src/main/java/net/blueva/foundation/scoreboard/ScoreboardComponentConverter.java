package net.blueva.foundation.scoreboard;

import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.minimessage.MiniMessageParser;
import net.blueva.foundation.text.serializer.LegacySerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * Converts a MiniMessage/legacy string to an NMS chat component for scoreboards.
 *
 * <p>On Paper with native Adventure support it uses
 * {@code io.papermc.paper.adventure.PaperAdventure.asVanilla(Component)} so the
 * full Adventure feature set (gradients, hover events, etc.) works.</p>
 *
 * <p>On Spigot it falls back to BlueFoundation's own parser and
 * {@code CraftChatMessage.fromString(legacy)}.</p>
 */
final class ScoreboardComponentConverter {

    private static final boolean PAPER_ADVENTURE_SUPPORT;
    private static final MethodHandle COMPONENT_METHOD;
    private static final Object EMPTY_COMPONENT;

    static {
        boolean paperAdventure = false;
        MethodHandle componentMethod = null;
        Object emptyComponent = null;

        try {
            Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method method = paperAdventureClass.getDeclaredMethod("asVanilla", net.kyori.adventure.text.Component.class);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            componentMethod = lookup.unreflect(method);
            emptyComponent = componentMethod.invoke(net.kyori.adventure.text.Component.empty());
            paperAdventure = true;
        } catch (Throwable ignored) {
            // Not Paper or Adventure not available; fall back to CraftChatMessage.
        }

        if (!paperAdventure) {
            try {
                Class<?> craftChatMessageClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".util.CraftChatMessage");
                Method method = craftChatMessageClass.getMethod("fromString", String.class);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                componentMethod = lookup.unreflect(method);
                emptyComponent = Array.get(componentMethod.invoke(""), 0);
            } catch (Throwable t) {
                throw new ExceptionInInitializerError(t);
            }
        }

        PAPER_ADVENTURE_SUPPORT = paperAdventure;
        COMPONENT_METHOD = componentMethod;
        EMPTY_COMPONENT = emptyComponent;
    }

    private ScoreboardComponentConverter() {
    }

    static Object emptyComponent() {
        return EMPTY_COMPONENT;
    }

    static Object toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return EMPTY_COMPONENT;
        }
        try {
            if (PAPER_ADVENTURE_SUPPORT) {
                Component component;
                if (text.indexOf('\u00A7') >= 0) {
                    // Scoreboard empty-line entries and other pre-colored strings use section codes.
                    component = LegacyComponentSerializer.legacySection().deserialize(text);
                } else if (text.indexOf('&') >= 0) {
                    component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                } else {
                    component = MiniMessage.miniMessage().deserialize(text);
                }
                return COMPONENT_METHOD.invoke(component);
            }
            String legacy = LegacySerializer.serialize(MiniMessageParser.parse(text));
            return Array.get(COMPONENT_METHOD.invoke(legacy), 0);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to convert scoreboard text to component: " + text, t);
        }
    }

    /**
     * Converts an Adventure component directly to an NMS chat component.
     * Only available when running on Paper/Adventure; on Spigot the component
     * is serialized to legacy and parsed through CraftChatMessage.
     */
    static Object toComponent(Component component) {
        if (component == null) {
            return EMPTY_COMPONENT;
        }
        try {
            if (PAPER_ADVENTURE_SUPPORT) {
                return COMPONENT_METHOD.invoke(component);
            }
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);
            return Array.get(COMPONENT_METHOD.invoke(legacy), 0);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to convert Adventure component to NMS component", t);
        }
    }
}
