package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.Entity;

/** Stable wrapper for EntityToggleGlideEvent (1.9+). */
public interface EntityToggleGlide extends WrappedEvent {

    /** @return true to cancel the event. */
    boolean onEntityToggleGlide(Entity entity, boolean gliding, boolean cancelled);
}
