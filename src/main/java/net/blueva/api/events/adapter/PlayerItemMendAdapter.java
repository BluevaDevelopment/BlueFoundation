package net.blueva.api.events.adapter;

import net.blueva.api.events.wrapped.PlayerItemMend;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class PlayerItemMendAdapter {
    private static final String EVENT = "org.bukkit.event.player.PlayerItemMendEvent";

    private PlayerItemMendAdapter() {}

    public static boolean isSupported() {
        return DynamicEventSupport.eventExists(EVENT);
    }

    public static boolean register(Plugin plugin, final PlayerItemMend wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, EVENT, priority, new EventExecutor() {
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
}
