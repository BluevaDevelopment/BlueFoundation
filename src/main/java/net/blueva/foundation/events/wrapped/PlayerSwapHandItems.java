package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Stable wrapper for PlayerSwapHandItemsEvent (1.9+). */
public interface PlayerSwapHandItems extends WrappedEvent {

    /** @return true to cancel the event. */
    boolean onPlayerSwapHandItems(Player player, ItemStack mainHandItem, ItemStack offHandItem, boolean cancelled);
}
