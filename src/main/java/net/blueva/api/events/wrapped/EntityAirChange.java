package net.blueva.api.events.wrapped;

import net.blueva.api.events.WrappedEvent;
import org.bukkit.entity.Entity;

/** Stable wrapper for EntityAirChangeEvent (1.12+). */
public interface EntityAirChange extends WrappedEvent {

    /** @return true to cancel the event. */
    boolean onEntityAirChange(Entity entity, int amount, boolean cancelled);
}
