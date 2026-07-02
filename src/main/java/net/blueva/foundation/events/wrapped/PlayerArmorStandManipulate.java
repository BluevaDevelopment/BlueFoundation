package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Stable wrapper for PlayerArmorStandManipulateEvent (1.8+). */
public interface PlayerArmorStandManipulate extends WrappedEvent {

    /**
     * @param slotName armor stand equipment slot name.
     * @param handName player hand name when available, otherwise null.
     * @return true to cancel the event.
     */
    boolean onPlayerArmorStandManipulate(Player player, ArmorStand armorStand, ItemStack playerItem, ItemStack armorStandItem, String slotName, String handName, boolean cancelled);
}
