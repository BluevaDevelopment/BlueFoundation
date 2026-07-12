package net.blueva.foundation.inventories;

import net.blueva.foundation.reflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;

/**
 * Inventory-related helpers that smooth over Bukkit API differences between
 * server versions, so plugins can call them without version guards.
 */
public class Inventories {

    private static volatile Method viewGetTopInventory; // InventoryView#getTopInventory
    private static volatile boolean topInventoryResolved;

    /**
     * Returns the top inventory of the player's currently open inventory view.
     * <p>{@code InventoryView} changed from a class (legacy servers) to an
     * interface (modern servers), so any direct bytecode reference compiled
     * against one side throws {@code IncompatibleClassChangeError} on the
     * other. The lookup is therefore done reflectively.</p>
     *
     * @param player the player (may be {@code null})
     * @return the open top inventory, or {@code null} if unavailable
     */
    public static Inventory getOpenTopInventory(Player player) {
        if (player == null) {
            return null;
        }
        Object view = player.getOpenInventory(); // never invoke methods on this reference directly
        if (view == null) {
            return null;
        }
        if (!topInventoryResolved) {
            resolveTopInventory(view.getClass());
        }
        if (viewGetTopInventory != null) {
            try {
                Object inventory = viewGetTopInventory.invoke(view);
                if (inventory instanceof Inventory) {
                    return (Inventory) inventory;
                }
            } catch (Throwable ignored) {
                // fall through to null
            }
        }
        return null;
    }

    private static void resolveTopInventory(Class<?> viewClass) {
        synchronized (Inventories.class) {
            if (topInventoryResolved) {
                return;
            }
            viewGetTopInventory = Reflection.method(viewClass, "getTopInventory");
            topInventoryResolved = true;
        }
    }
}
