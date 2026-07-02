package net.blueva.foundation.events.wrapped;

import net.blueva.foundation.events.WrappedEvent;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Stable wrapper for PlayerItemMendEvent (1.12+). */
public interface PlayerItemMend extends WrappedEvent {

    /**
     * @param slotName equipment slot name when available, otherwise null.
     * @return true to cancel the event.
     */
    boolean onPlayerItemMend(Player player, ItemStack item, String slotName, ExperienceOrb experienceOrb, int repairAmount, boolean cancelled);
}
