package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

/** Stable wrapper for item pickup events across old and new Bukkit APIs. */
public interface EntityPickup extends WrappedEvent {

    /** @return true to cancel the event. */
    boolean onEntityPickUpItemStack(Entity entity, Item item, int remaining, boolean cancelled);
}
