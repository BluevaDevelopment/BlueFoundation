package net.blueva.api.events;

import net.blueva.api.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Reflection-based multi-version event utilities. */
public class Events {

    protected Events() {
    }

    public static Manager manager() {
        return new Manager();
    }

    public static boolean register(Plugin plugin, WrappedEvent wrappedEvent) {
        return manager().register(plugin, wrappedEvent, EventPriority.NORMAL);
    }

    public static boolean register(Plugin plugin, WrappedEvent wrappedEvent, EventPriority priority) {
        return manager().register(plugin, wrappedEvent, priority);
    }

    public interface WrappedEvent {
    }

    /** Stable wrapper for item pickup events across old and new Bukkit APIs. */
    public interface EntityPickup extends WrappedEvent {
        /** @return true to cancel the event. */
        boolean onEntityPickUpItemStack(Entity entity, Item item, int remaining, boolean cancelled);
    }

    /** Stable wrapper for PlayerSwapHandItemsEvent (1.9+). */
    public interface PlayerSwapHandItems extends WrappedEvent {
        /** @return true to cancel the event. */
        boolean onPlayerSwapHandItems(Player player, ItemStack mainHandItem, ItemStack offHandItem, boolean cancelled);
    }

    /** Stable wrapper for PlayerItemMendEvent (1.12+). */
    public interface PlayerItemMend extends WrappedEvent {
        /**
         * @param slotName equipment slot name when available, otherwise null.
         * @return true to cancel the event.
         */
        boolean onPlayerItemMend(Player player, ItemStack item, String slotName, ExperienceOrb experienceOrb, int repairAmount, boolean cancelled);
    }

    /** Stable wrapper for EntityToggleGlideEvent (1.9+). */
    public interface EntityToggleGlide extends WrappedEvent {
        /** @return true to cancel the event. */
        boolean onEntityToggleGlide(Entity entity, boolean gliding, boolean cancelled);
    }

    /** Stable wrapper for EntityAirChangeEvent (1.12+). */
    public interface EntityAirChange extends WrappedEvent {
        /** @return true to cancel the event. */
        boolean onEntityAirChange(Entity entity, int amount, boolean cancelled);
    }

    public static class Manager {

        protected Manager() {
        }

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
                registered = registerEntityPickup(plugin, (EntityPickup) wrappedEvent, priority) || registered;
            }
            if (wrappedEvent instanceof PlayerSwapHandItems) {
                registered = registerPlayerSwapHandItems(plugin, (PlayerSwapHandItems) wrappedEvent, priority) || registered;
            }
            if (wrappedEvent instanceof PlayerItemMend) {
                registered = registerPlayerItemMend(plugin, (PlayerItemMend) wrappedEvent, priority) || registered;
            }
            if (wrappedEvent instanceof EntityToggleGlide) {
                registered = registerEntityToggleGlide(plugin, (EntityToggleGlide) wrappedEvent, priority) || registered;
            }
            if (wrappedEvent instanceof EntityAirChange) {
                registered = registerEntityAirChange(plugin, (EntityAirChange) wrappedEvent, priority) || registered;
            }
            return registered;
        }

        public boolean supportsEntityPickup() {
            return resolveEntityPickupAdapter() != null;
        }

        public boolean supportsPlayerSwapHandItems() {
            return Reflection.classExists("org.bukkit.event.player.PlayerSwapHandItemsEvent");
        }

        public boolean supportsPlayerItemMend() {
            return Reflection.classExists("org.bukkit.event.player.PlayerItemMendEvent");
        }

        public boolean supportsEntityToggleGlide() {
            return Reflection.classExists("org.bukkit.event.entity.EntityToggleGlideEvent");
        }

        public boolean supportsEntityAirChange() {
            return Reflection.classExists("org.bukkit.event.entity.EntityAirChangeEvent");
        }

        private boolean registerEntityPickup(Plugin plugin, EntityPickup wrappedEvent, EventPriority priority) {
            EntityPickupAdapter adapter = resolveEntityPickupAdapter();
            if (adapter == null) {
                return false;
            }

            Listener listener = new Listener() {
            };

            EventExecutor executor = new EntityPickupExecutor(adapter, wrappedEvent);
            Bukkit.getPluginManager().registerEvent(adapter.eventClass, listener, priority, executor, plugin, false);
            return true;
        }

        private boolean registerPlayerSwapHandItems(Plugin plugin, PlayerSwapHandItems wrappedEvent, EventPriority priority) {
            return registerDynamic(plugin, "org.bukkit.event.player.PlayerSwapHandItemsEvent", priority, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        Player player = (Player) invoke(event, "getPlayer");
                        ItemStack mainHandItem = (ItemStack) invoke(event, "getMainHandItem");
                        ItemStack offHandItem = (ItemStack) invoke(event, "getOffHandItem");
                        boolean cancelled = isCancelled(event);
                        if (wrappedEvent.onPlayerSwapHandItems(player, mainHandItem, offHandItem, cancelled)) {
                            setCancelled(event, true);
                        }
                    } catch (Throwable throwable) {
                        throw new EventException(throwable);
                    }
                }
            });
        }

        private boolean registerPlayerItemMend(Plugin plugin, PlayerItemMend wrappedEvent, EventPriority priority) {
            return registerDynamic(plugin, "org.bukkit.event.player.PlayerItemMendEvent", priority, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        Player player = (Player) invoke(event, "getPlayer");
                        ItemStack item = (ItemStack) invoke(event, "getItem");
                        ExperienceOrb experienceOrb = (ExperienceOrb) invoke(event, "getExperienceOrb");
                        int repairAmount = intValue(invoke(event, "getRepairAmount"));
                        Object slot = invokeOptional(event, "getSlot");
                        String slotName = slot == null ? null : String.valueOf(slot);
                        boolean cancelled = isCancelled(event);
                        if (wrappedEvent.onPlayerItemMend(player, item, slotName, experienceOrb, repairAmount, cancelled)) {
                            setCancelled(event, true);
                        }
                    } catch (Throwable throwable) {
                        throw new EventException(throwable);
                    }
                }
            });
        }

        private boolean registerEntityToggleGlide(Plugin plugin, EntityToggleGlide wrappedEvent, EventPriority priority) {
            return registerDynamic(plugin, "org.bukkit.event.entity.EntityToggleGlideEvent", priority, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        Entity entity = (Entity) invoke(event, "getEntity");
                        boolean gliding = booleanValue(invoke(event, "isGliding"));
                        boolean cancelled = isCancelled(event);
                        if (wrappedEvent.onEntityToggleGlide(entity, gliding, cancelled)) {
                            setCancelled(event, true);
                        }
                    } catch (Throwable throwable) {
                        throw new EventException(throwable);
                    }
                }
            });
        }

        private boolean registerEntityAirChange(Plugin plugin, EntityAirChange wrappedEvent, EventPriority priority) {
            return registerDynamic(plugin, "org.bukkit.event.entity.EntityAirChangeEvent", priority, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        Entity entity = (Entity) invoke(event, "getEntity");
                        int amount = intValue(invoke(event, "getAmount"));
                        boolean cancelled = isCancelled(event);
                        if (wrappedEvent.onEntityAirChange(entity, amount, cancelled)) {
                            setCancelled(event, true);
                        }
                    } catch (Throwable throwable) {
                        throw new EventException(throwable);
                    }
                }
            });
        }

        @SuppressWarnings("unchecked")
        private boolean registerDynamic(Plugin plugin, String eventClassName, EventPriority priority, EventExecutor executor) {
            try {
                Class<?> rawEventClass = Class.forName(eventClassName);
                Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass.asSubclass(Event.class);
                Listener listener = new Listener() {
                };
                Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, plugin, false);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        private EntityPickupAdapter resolveEntityPickupAdapter() {
            EntityPickupAdapter modern = createEntityPickupAdapter(
                    "org.bukkit.event.entity.EntityPickupItemEvent",
                    "getEntity"
            );
            if (modern != null) {
                return modern;
            }

            return createEntityPickupAdapter(
                    "org.bukkit.event.player.PlayerPickupItemEvent",
                    "getPlayer"
            );
        }

        @SuppressWarnings("unchecked")
        private EntityPickupAdapter createEntityPickupAdapter(String eventClassName, String entityGetterName) {
            try {
                Class<?> rawEventClass = Class.forName(eventClassName);
                Class<? extends Event> eventClass = (Class<? extends Event>) rawEventClass.asSubclass(Event.class);
                Method entityGetter = rawEventClass.getMethod(entityGetterName);
                Method itemGetter = rawEventClass.getMethod("getItem");
                Method remainingGetter = rawEventClass.getMethod("getRemaining");
                return new EntityPickupAdapter(eventClass, entityGetter, itemGetter, remainingGetter);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private Object invoke(Object target, String methodName) throws Exception {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        }

        private Object invokeOptional(Object target, String methodName) {
            try {
                return invoke(target, methodName);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private boolean isCancelled(Event event) {
            return event instanceof Cancellable && ((Cancellable) event).isCancelled();
        }

        private void setCancelled(Event event, boolean cancelled) {
            if (event instanceof Cancellable) {
                ((Cancellable) event).setCancelled(cancelled);
            }
        }

        private int intValue(Object value) {
            return value instanceof Number ? ((Number) value).intValue() : 0;
        }

        private boolean booleanValue(Object value) {
            return value instanceof Boolean && (Boolean) value;
        }
    }

    private static final class EntityPickupExecutor implements EventExecutor {
        private final EntityPickupAdapter adapter;
        private final EntityPickup wrappedEvent;

        private EntityPickupExecutor(EntityPickupAdapter adapter, EntityPickup wrappedEvent) {
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

    private static final class EntityPickupAdapter {
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
    }
}
