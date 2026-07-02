package net.blueva.foundation.events.adapter;

import net.blueva.foundation.events.wrapped.EntityAirChange;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class EntityAirChangeAdapter {
    private static final String EVENT = "org.bukkit.event.entity.EntityAirChangeEvent";

    private EntityAirChangeAdapter() {}

    public static boolean isSupported() {
        return DynamicEventSupport.eventExists(EVENT);
    }

    public static boolean register(Plugin plugin, final EntityAirChange wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, EVENT, priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Entity entity = (Entity) DynamicEventSupport.invoke(event, "getEntity");
                    int amount = DynamicEventSupport.intValue(DynamicEventSupport.invoke(event, "getAmount"));
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onEntityAirChange(entity, amount, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }
}
