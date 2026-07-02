package net.blueva.foundation.events;

import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

/** Public facade for reflection-based multi-version event utilities. */
public class Events {

    protected Events() {
    }

    public enum Type {
        ENTITY_PICKUP,
        PLAYER_SWAP_HAND_ITEMS,
        PLAYER_ITEM_MEND,
        ENTITY_TOGGLE_GLIDE,
        ENTITY_AIR_CHANGE,
        PLAYER_INTERACT_AT_ENTITY,
        PLAYER_ARMOR_STAND_MANIPULATE
    }

    public static EventManager manager() {
        return new EventManager();
    }

    public static boolean supports(Type type) {
        return manager().supports(type);
    }

    public static boolean register(Plugin plugin, WrappedEvent wrappedEvent) {
        return manager().register(plugin, wrappedEvent, EventPriority.NORMAL);
    }

    public static boolean register(Plugin plugin, WrappedEvent wrappedEvent, EventPriority priority) {
        return manager().register(plugin, wrappedEvent, priority);
    }

    public interface EntityPickup extends net.blueva.foundation.events.wrapped.EntityPickup {
    }

    public interface PlayerSwapHandItems extends net.blueva.foundation.events.wrapped.PlayerSwapHandItems {
    }

    public interface PlayerItemMend extends net.blueva.foundation.events.wrapped.PlayerItemMend {
    }

    public interface EntityToggleGlide extends net.blueva.foundation.events.wrapped.EntityToggleGlide {
    }

    public interface EntityAirChange extends net.blueva.foundation.events.wrapped.EntityAirChange {
    }

    public interface PlayerInteractAtEntity extends net.blueva.foundation.events.wrapped.PlayerInteractAtEntity {
    }

    public interface PlayerArmorStandManipulate extends net.blueva.foundation.events.wrapped.PlayerArmorStandManipulate {
    }
}
