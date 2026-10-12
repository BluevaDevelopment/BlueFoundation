package net.blueva.foundation.menus;

import net.blueva.foundation.sounds.Sounds;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

/**
 * Cancels every interaction with a menu inventory and routes clicks to the
 * button that was clicked.
 *
 * <p>One instance per plugin, registered the first time that plugin opens a
 * menu and unregistered with the plugin, so a menu drawn by a plugin that has
 * since been disabled stops responding rather than throwing.</p>
 */
final class MenuListener implements Listener {

    private final Plugin plugin;

    MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        MenuHolder holder = holderOf(event.getInventory());
        if (holder == null || holder.plugin() != plugin) {
            return;
        }
        // Cancel before anything else: a handler that throws must not leave
        // the player holding an item that was never theirs.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        // A click in the player's own inventory while a menu is open is still
        // cancelled above, but there is no button to run.
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        MenuButton button = holder.buttonAt(event.getRawSlot());
        if (button == null) {
            return;
        }

        MenuContext context = holder.context();
        String clickSound = context.definition().clickSound();
        if (clickSound != null) {
            Sounds.play(player, 1f, 1f, clickSound);
        }
        InventoryClickEvent previous = context.clickEvent();
        context.clickEvent(event);
        try {
            if (button.handler() != null) button.handler().accept(player);
            MenuActionExecutor.run(plugin, player, button.actions(), event.getClick(), context);
        } finally {
            context.clickEvent(previous);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        MenuHolder holder = holderOf(event.getInventory());
        if (holder != null && holder.plugin() == plugin) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        MenuHolder holder = holderOf(event.getInventory());
        if (holder == null || holder.plugin() != plugin) {
            return;
        }
        HumanEntity closer = event.getPlayer();
        if (closer instanceof Player) {
            Menus.forget((Player) closer, holder);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Menus.forget(event.getPlayer(), null);
    }

    private static MenuHolder holderOf(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof MenuHolder ? (MenuHolder) holder : null;
    }
}
