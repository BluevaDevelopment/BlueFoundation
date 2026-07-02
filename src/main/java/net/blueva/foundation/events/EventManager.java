package net.blueva.foundation.events;

import net.blueva.foundation.events.adapter.EntityAirChangeAdapter;
import net.blueva.foundation.events.adapter.EntityPickupAdapter;
import net.blueva.foundation.events.adapter.EntityToggleGlideAdapter;
import net.blueva.foundation.events.adapter.PlayerArmorStandManipulateAdapter;
import net.blueva.foundation.events.adapter.PlayerInteractAtEntityAdapter;
import net.blueva.foundation.events.adapter.PlayerItemMendAdapter;
import net.blueva.foundation.events.adapter.PlayerSwapHandItemsAdapter;
import net.blueva.foundation.events.wrapped.EntityAirChange;
import net.blueva.foundation.events.wrapped.EntityPickup;
import net.blueva.foundation.events.wrapped.EntityToggleGlide;
import net.blueva.foundation.events.wrapped.PlayerArmorStandManipulate;
import net.blueva.foundation.events.wrapped.PlayerInteractAtEntity;
import net.blueva.foundation.events.wrapped.PlayerItemMend;
import net.blueva.foundation.events.wrapped.PlayerSwapHandItems;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

/** Multi-version wrapped event manager. */
public class EventManager {

    public boolean register(Plugin plugin, WrappedEvent wrappedEvent) {
        return register(plugin, wrappedEvent, EventPriority.NORMAL);
    }

    public boolean register(Plugin plugin, WrappedEvent wrappedEvent, EventPriority priority) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (wrappedEvent == null) {
            throw new IllegalArgumentException("wrappedEvent cannot be null");
        }
        if (priority == null) {
            priority = EventPriority.NORMAL;
        }

        boolean registered = false;
        if (wrappedEvent instanceof EntityPickup) {
            registered = EntityPickupAdapter.register(plugin, (EntityPickup) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof PlayerSwapHandItems) {
            registered = PlayerSwapHandItemsAdapter.register(plugin, (PlayerSwapHandItems) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof PlayerItemMend) {
            registered = PlayerItemMendAdapter.register(plugin, (PlayerItemMend) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof EntityToggleGlide) {
            registered = EntityToggleGlideAdapter.register(plugin, (EntityToggleGlide) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof EntityAirChange) {
            registered = EntityAirChangeAdapter.register(plugin, (EntityAirChange) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof PlayerInteractAtEntity) {
            registered = PlayerInteractAtEntityAdapter.register(plugin, (PlayerInteractAtEntity) wrappedEvent, priority) || registered;
        }
        if (wrappedEvent instanceof PlayerArmorStandManipulate) {
            registered = PlayerArmorStandManipulateAdapter.register(plugin, (PlayerArmorStandManipulate) wrappedEvent, priority) || registered;
        }
        return registered;
    }

    public boolean supports(Events.Type type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case ENTITY_PICKUP:
                return EntityPickupAdapter.isSupported();
            case PLAYER_SWAP_HAND_ITEMS:
                return PlayerSwapHandItemsAdapter.isSupported();
            case PLAYER_ITEM_MEND:
                return PlayerItemMendAdapter.isSupported();
            case ENTITY_TOGGLE_GLIDE:
                return EntityToggleGlideAdapter.isSupported();
            case ENTITY_AIR_CHANGE:
                return EntityAirChangeAdapter.isSupported();
            case PLAYER_INTERACT_AT_ENTITY:
                return PlayerInteractAtEntityAdapter.isSupported();
            case PLAYER_ARMOR_STAND_MANIPULATE:
                return PlayerArmorStandManipulateAdapter.isSupported();
            default:
                return false;
        }
    }
}
