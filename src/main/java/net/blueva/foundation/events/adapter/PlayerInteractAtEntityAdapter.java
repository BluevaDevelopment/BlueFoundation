package net.blueva.foundation.events.adapter;

import net.blueva.foundation.events.wrapped.PlayerInteractAtEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class PlayerInteractAtEntityAdapter {
    private static final String EVENT = "org.bukkit.event.player.PlayerInteractAtEntityEvent";

    private PlayerInteractAtEntityAdapter() {}

    public static boolean isSupported() {
        return DynamicEventSupport.eventExists(EVENT);
    }

    public static boolean register(Plugin plugin, final PlayerInteractAtEntity wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, EVENT, priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Player player = (Player) DynamicEventSupport.invoke(event, "getPlayer");
                    Entity rightClicked = (Entity) DynamicEventSupport.invoke(event, "getRightClicked");
                    Vector clickedPosition = (Vector) DynamicEventSupport.invoke(event, "getClickedPosition");
                    Object hand = DynamicEventSupport.invokeOptional(event, "getHand");
                    String handName = hand == null ? null : String.valueOf(hand);
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onPlayerInteractAtEntity(player, rightClicked, clickedPosition, handName, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }
}
