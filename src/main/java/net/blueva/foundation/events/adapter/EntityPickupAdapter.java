package net.blueva.foundation.events.adapter;

import net.blueva.foundation.events.wrapped.EntityPickup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Internal adapter for legacy PlayerPickupItemEvent and modern EntityPickupItemEvent. */
public final class EntityPickupAdapter {

    private final Class<? extends Event> eventClass;
    private final Method entityGetter;
    private final Method itemGetter;
    private final Method remainingGetter;

    private EntityPickupAdapter(Class<? extends Event> eventClass, Method entityGetter, Method itemGetter, Method remainingGetter) {
        this.eventClass = eventClass;
        this.entityGetter = entityGetter;
        this.itemGetter = itemGetter;
        this.remainingGetter = remainingGetter;
    }

    public static boolean isSupported() {
        return resolve() != null;
    }

    public static boolean register(Plugin plugin, EntityPickup wrappedEvent, EventPriority priority) {
        EntityPickupAdapter adapter = resolve();
        if (adapter == null) {
            return false;
        }

        Listener listener = new Listener() {
        };
        Bukkit.getPluginManager().registerEvent(adapter.eventClass, listener, priority, new Executor(adapter, wrappedEvent), plugin, false);
        return true;
    }

    private static EntityPickupAdapter resolve() {
        EntityPickupAdapter modern = create("org.bukkit.event.entity.EntityPickupItemEvent", "getEntity");
        if (modern != null) {
            return modern;
        }
        return create("org.bukkit.event.player.PlayerPickupItemEvent", "getPlayer");
    }

    private static EntityPickupAdapter create(String eventClassName, String entityGetterName) {
        try {
            Class<? extends Event> eventClass = DynamicEventSupport.eventClass(eventClassName);
            if (eventClass == null) {
                return null;
            }
            Method entityGetter = eventClass.getMethod(entityGetterName);
            Method itemGetter = eventClass.getMethod("getItem");
            Method remainingGetter = eventClass.getMethod("getRemaining");
            return new EntityPickupAdapter(eventClass, entityGetter, itemGetter, remainingGetter);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class Executor implements EventExecutor {
        private final EntityPickupAdapter adapter;
        private final EntityPickup wrappedEvent;

        private Executor(EntityPickupAdapter adapter, EntityPickup wrappedEvent) {
            this.adapter = adapter;
            this.wrappedEvent = wrappedEvent;
        }

        @Override
        public void execute(Listener listener, Event event) throws EventException {
            if (!adapter.eventClass.isInstance(event)) {
                return;
            }

            try {
                Entity entity = (Entity) adapter.entityGetter.invoke(event);
                Item item = (Item) adapter.itemGetter.invoke(event);
                int remaining = ((Number) adapter.remainingGetter.invoke(event)).intValue();
                boolean cancelled = event instanceof Cancellable && ((Cancellable) event).isCancelled();
                boolean cancel = wrappedEvent.onEntityPickUpItemStack(entity, item, remaining, cancelled);
                if (cancel && event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
            } catch (Throwable throwable) {
                throw new EventException(throwable);
            }
        }
    }
}
