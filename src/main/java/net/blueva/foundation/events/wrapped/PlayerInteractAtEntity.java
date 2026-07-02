package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Stable wrapper for PlayerInteractAtEntityEvent (1.8+). */
public interface PlayerInteractAtEntity extends WrappedEvent {

    /**
     * @param handName equipment hand name when available, otherwise null.
     * @return true to cancel the event.
     */
    boolean onPlayerInteractAtEntity(Player player, Entity rightClicked, Vector clickedPosition, String handName, boolean cancelled);
}
