package net.blueva.api.events;

import net.blueva.api.events.adapter.DynamicEventSupport;
import net.blueva.api.events.adapter.EntityPickupAdapter;
import net.blueva.api.events.wrapped.EntityAirChange;
import net.blueva.api.events.wrapped.EntityPickup;
import net.blueva.api.events.wrapped.EntityToggleGlide;
import net.blueva.api.events.wrapped.PlayerItemMend;
import net.blueva.api.events.wrapped.PlayerSwapHandItems;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
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

    public boolean supports(Events.Type type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case ENTITY_PICKUP:
                return supportsEntityPickup();
            case PLAYER_SWAP_HAND_ITEMS:
                return supportsPlayerSwapHandItems();
            case PLAYER_ITEM_MEND:
                return supportsPlayerItemMend();
            case ENTITY_TOGGLE_GLIDE:
                return supportsEntityToggleGlide();
            case ENTITY_AIR_CHANGE:
                return supportsEntityAirChange();
            default:
                return false;
        }
    }

    public boolean supportsEntityPickup() {
        return EntityPickupAdapter.isSupported();
    }

    public boolean supportsPlayerSwapHandItems() {
        return DynamicEventSupport.eventExists("org.bukkit.event.player.PlayerSwapHandItemsEvent");
    }

    public boolean supportsPlayerItemMend() {
        return DynamicEventSupport.eventExists("org.bukkit.event.player.PlayerItemMendEvent");
    }

    public boolean supportsEntityToggleGlide() {
        return DynamicEventSupport.eventExists("org.bukkit.event.entity.EntityToggleGlideEvent");
    }

    public boolean supportsEntityAirChange() {
        return DynamicEventSupport.eventExists("org.bukkit.event.entity.EntityAirChangeEvent");
    }

    private boolean registerPlayerSwapHandItems(Plugin plugin, final PlayerSwapHandItems wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, "org.bukkit.event.player.PlayerSwapHandItemsEvent", priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Player player = (Player) DynamicEventSupport.invoke(event, "getPlayer");
                    ItemStack mainHandItem = (ItemStack) DynamicEventSupport.invoke(event, "getMainHandItem");
                    ItemStack offHandItem = (ItemStack) DynamicEventSupport.invoke(event, "getOffHandItem");
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onPlayerSwapHandItems(player, mainHandItem, offHandItem, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }

    private boolean registerPlayerItemMend(Plugin plugin, final PlayerItemMend wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, "org.bukkit.event.player.PlayerItemMendEvent", priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Player player = (Player) DynamicEventSupport.invoke(event, "getPlayer");
                    ItemStack item = (ItemStack) DynamicEventSupport.invoke(event, "getItem");
                    ExperienceOrb experienceOrb = (ExperienceOrb) DynamicEventSupport.invoke(event, "getExperienceOrb");
                    int repairAmount = DynamicEventSupport.intValue(DynamicEventSupport.invoke(event, "getRepairAmount"));
                    Object slot = DynamicEventSupport.invokeOptional(event, "getSlot");
                    String slotName = slot == null ? null : String.valueOf(slot);
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onPlayerItemMend(player, item, slotName, experienceOrb, repairAmount, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }

    private boolean registerEntityToggleGlide(Plugin plugin, final EntityToggleGlide wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, "org.bukkit.event.entity.EntityToggleGlideEvent", priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Entity entity = (Entity) DynamicEventSupport.invoke(event, "getEntity");
                    boolean gliding = DynamicEventSupport.booleanValue(DynamicEventSupport.invoke(event, "isGliding"));
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onEntityToggleGlide(entity, gliding, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }

    private boolean registerEntityAirChange(Plugin plugin, final EntityAirChange wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, "org.bukkit.event.entity.EntityAirChangeEvent", priority, new EventExecutor() {
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
