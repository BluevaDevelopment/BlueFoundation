package net.blueva.api.events.adapter;

import net.blueva.api.events.wrapped.PlayerSwapHandItems;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class PlayerSwapHandItemsAdapter {
    private static final String EVENT = "org.bukkit.event.player.PlayerSwapHandItemsEvent";

    private PlayerSwapHandItemsAdapter() {}

    public static boolean isSupported() {
        return DynamicEventSupport.eventExists(EVENT);
    }

    public static boolean register(Plugin plugin, final PlayerSwapHandItems wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, EVENT, priority, new EventExecutor() {
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
}
