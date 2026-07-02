package net.blueva.foundation.events.adapter;

import net.blueva.foundation.events.wrapped.PlayerArmorStandManipulate;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public final class PlayerArmorStandManipulateAdapter {
    private static final String EVENT = "org.bukkit.event.player.PlayerArmorStandManipulateEvent";

    private PlayerArmorStandManipulateAdapter() {}

    public static boolean isSupported() {
        return DynamicEventSupport.eventExists(EVENT);
    }

    public static boolean register(Plugin plugin, final PlayerArmorStandManipulate wrappedEvent, EventPriority priority) {
        return DynamicEventSupport.register(plugin, EVENT, priority, new EventExecutor() {
            @Override
            public void execute(Listener listener, Event event) throws EventException {
                try {
                    Player player = (Player) DynamicEventSupport.invoke(event, "getPlayer");
                    ArmorStand armorStand = (ArmorStand) DynamicEventSupport.invoke(event, "getRightClicked");
                    ItemStack playerItem = (ItemStack) DynamicEventSupport.invoke(event, "getPlayerItem");
                    ItemStack armorStandItem = (ItemStack) DynamicEventSupport.invoke(event, "getArmorStandItem");
                    Object slot = DynamicEventSupport.invoke(event, "getSlot");
                    Object hand = DynamicEventSupport.invokeOptional(event, "getHand");
                    String slotName = slot == null ? null : String.valueOf(slot);
                    String handName = hand == null ? null : String.valueOf(hand);
                    boolean cancelled = DynamicEventSupport.isCancelled(event);
                    if (wrappedEvent.onPlayerArmorStandManipulate(player, armorStand, playerItem, armorStandItem, slotName, handName, cancelled)) {
                        DynamicEventSupport.setCancelled(event, true);
                    }
                } catch (Throwable throwable) {
                    throw new EventException(throwable);
                }
            }
        });
    }
}
