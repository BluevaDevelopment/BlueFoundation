package net.blueva.foundation.menus;

import net.blueva.foundation.items.Items;
import net.blueva.foundation.materials.Materials;
import net.blueva.foundation.messages.Messages;
import net.blueva.foundation.sounds.Sounds;
import net.blueva.foundation.version.Version;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws a menu as a chest inventory.
 *
 * <p>Slots are filled in the order decoration, declared buttons, navigation,
 * then dynamic entries, so a paginated menu's entries never land on top of the
 * next-page arrow.</p>
 */
final class ChestRenderer {

    /** Legacy servers truncate inventory titles at 32 characters. */
    private static final int LEGACY_TITLE_LIMIT = 32;

    private ChestRenderer() {
    }

    /**
     * Build and show the inventory.
     *
     * @param plugin  the plugin opening it
     * @param context the view to draw
     * @return the holder backing the open inventory
     */
    static MenuHolder open(Plugin plugin, MenuContext context) {
        MenuDefinition definition = context.definition();
        Player player = context.player();
        Pagination pagination = context.pagination(false);

        MenuHolder holder = new MenuHolder(plugin, context);
        Inventory inventory = Bukkit.createInventory(holder, definition.size(),
                title(player, context, pagination));
        holder.inventory(inventory);

        Set<Integer> taken = new LinkedHashSet<Integer>();

        drawDecoration(inventory, holder, context, pagination, taken);
        drawButtons(inventory, holder, context, pagination, taken);
        drawNavigation(inventory, holder, context, pagination, taken);
        drawEntries(inventory, holder, context, pagination, taken);

        player.openInventory(inventory);
        if (definition.openSound() != null) {
            Sounds.play(player, 1f, 1f, definition.openSound());
        }
        if (definition.layout() == MenuLayout.PAGINATED
                && context.entries().isEmpty()
                && definition.emptyMessage() != null) {
            Messages.send(player, resolve(player, context, pagination, definition.emptyMessage()));
        }
        return holder;
    }

    private static String title(Player player, MenuContext context, Pagination pagination) {
        String resolved = resolve(player, context, pagination, context.definition().title());
        String legacy = Messages.legacySection(resolved);
        if (legacy == null) {
            return "";
        }
        if (Version.isOlderThan(1, 13) && legacy.length() > LEGACY_TITLE_LIMIT) {
            return legacy.substring(0, LEGACY_TITLE_LIMIT);
        }
        return legacy;
    }

    private static void drawDecoration(Inventory inventory, MenuHolder holder, MenuContext context,
                                       Pagination pagination, Set<Integer> taken) {
        MenuButton decoration = context.definition().decoration();
        if (decoration == null) {
            return;
        }
        ItemStack item = item(context.player(), context, pagination, decoration);
        if (item == null) {
            return;
        }
        List<Integer> slots = decoration.slots();
        if (slots.isEmpty()) {
            // No slots given: the decoration is a background, so it goes
            // everywhere and later passes overwrite it.
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                inventory.setItem(slot, item.clone());
            }
            return;
        }
        for (Integer slot : slots) {
            int index = slot.intValue();
            if (index >= 0 && index < inventory.getSize()) {
                inventory.setItem(index, item.clone());
                taken.add(slot);
            }
        }
    }

    private static void drawButtons(Inventory inventory, MenuHolder holder, MenuContext context,
                                    Pagination pagination, Set<Integer> taken) {
        for (MenuButton button : context.definition().buttons().values()) {
            place(inventory, holder, context, pagination, button, taken);
        }
    }

    private static void drawNavigation(Inventory inventory, MenuHolder holder, MenuContext context,
                                       Pagination pagination, Set<Integer> taken) {
        MenuDefinition definition = context.definition();
        if (definition.layout() == MenuLayout.PAGINATED) {
            if (pagination.hasPrevious()) {
                place(inventory, holder, context, pagination, definition.previousButton(), taken);
            }
            if (pagination.hasNext()) {
                place(inventory, holder, context, pagination, definition.nextButton(), taken);
            }
        }
        place(inventory, holder, context, pagination, definition.closeButton(), taken);
    }

    private static void drawEntries(Inventory inventory, MenuHolder holder, MenuContext context,
                                    Pagination pagination, Set<Integer> taken) {
        MenuDefinition definition = context.definition();
        if (context.entries().isEmpty()) {
            return;
        }
        List<Integer> slots = availableSlots(inventory.getSize(), definition, taken);
        List<MenuButton> entries = context.entries();

        int slotIndex = 0;
        for (int i = pagination.start(); i < pagination.end() && slotIndex < slots.size(); i++) {
            MenuButton entry = entries.get(i);
            if (entry == null || !visible(context.player(), context, pagination, entry)) {
                continue;
            }
            int slot = slots.get(slotIndex++).intValue();
            ItemStack item = item(context.player(), context, pagination, entry);
            if (item == null) {
                continue;
            }
            inventory.setItem(slot, item);
            holder.bind(slot, entry);
        }
    }

    private static List<Integer> availableSlots(int size, MenuDefinition definition, Set<Integer> taken) {
        List<Integer> slots = new ArrayList<Integer>();
        if (!definition.dynamicSlots().isEmpty()) {
            for (Integer slot : definition.dynamicSlots()) {
                if (slot != null && slot.intValue() >= 0 && slot.intValue() < size) {
                    slots.add(slot);
                }
            }
            return slots;
        }
        for (int slot = 0; slot < size; slot++) {
            if (!taken.contains(Integer.valueOf(slot))) {
                slots.add(Integer.valueOf(slot));
            }
        }
        return slots;
    }

    private static void place(Inventory inventory, MenuHolder holder, MenuContext context,
                              Pagination pagination, MenuButton button, Set<Integer> taken) {
        if (button == null || button.slots().isEmpty()) {
            return;
        }
        if (!visible(context.player(), context, pagination, button)) {
            return;
        }
        ItemStack item = item(context.player(), context, pagination, button);
        if (item == null) {
            return;
        }
        for (Integer slot : button.slots()) {
            int index = slot.intValue();
            if (index < 0 || index >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(index, item.clone());
            holder.bind(index, button);
            taken.add(slot);
        }
    }

    /**
     * @return whether this button's condition passes for this viewer
     */
    static boolean visible(Player player, MenuContext context, Pagination pagination, MenuButton button) {
        if (button.visibleIf() == null) {
            return true;
        }
        return MenuPlaceholders.isTrue(resolve(player, context, pagination, button.visibleIf()));
    }

    /**
     * Turn a button into an item, with every string resolved.
     *
     * @return the item, or {@code null} if the button has no usable material
     */
    static ItemStack item(Player player, MenuContext context, Pagination pagination, MenuButton button) {
        ItemStack exact = button.item();
        if (exact != null) return exact;
        Material material = material(button);
        if (material == null) {
            return null;
        }
        Items.Builder builder = Items.builder(material)
                .amount(button.amount())
                .name(resolve(player, context, pagination, button.name()))
                .lore(MenuPlaceholders.apply(player, button.lore(),
                        context.placeholders(), pagination));

        if (button.skull() != null && !button.skull().isEmpty()) {
            builder.skullValue(resolve(player, context, pagination, button.skull()));
        }
        for (String enchantment : button.enchantments()) {
            applyEnchantment(builder, enchantment);
        }
        if (button.glow()) {
            builder.glow();
        }
        if (button.hideFlags()) {
            builder.hideAllFlags();
        }
        // A blank name removes the text; this removes the box. Asked for, or
        // taken as read for a button with nothing to say and nothing to do -
        // which is what a filler pane is, and hovering the gap between two
        // groups of buttons should not open a window.
        if (button.hideTooltip() || saysNothing(button)) {
            builder.hideTooltip();
        }
        return builder.build();
    }

    /** Whether this button is wallpaper: no text, and nothing to do. */
    private static boolean saysNothing(MenuButton button) {
        String name = button.name();
        boolean blank = name == null || name.replaceAll("<[^>]*>", "").trim().isEmpty();
        return blank && button.lore().isEmpty() && button.actions().isEmpty()
                && button.handler() == null;
    }

    private static Material material(MenuButton button) {
        if (button.materials().isEmpty()) {
            return button.skull() != null && !button.skull().isEmpty()
                    ? Materials.orDefault(Material.STONE, "PLAYER_HEAD", "SKULL_ITEM")
                    : Material.STONE;
        }
        String[] names = button.materials().toArray(new String[0]);
        return Materials.orDefault(Material.STONE, names);
    }

    private static void applyEnchantment(Items.Builder builder, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        String trimmed = raw.trim();
        int separator = trimmed.lastIndexOf(':');
        if (separator < 0) {
            builder.enchant(trimmed, 1);
            return;
        }
        String name = trimmed.substring(0, separator).trim();
        try {
            builder.enchant(name, Integer.parseInt(trimmed.substring(separator + 1).trim()));
        } catch (NumberFormatException e) {
            builder.enchant(name, 1);
        }
    }

    private static String resolve(Player player, MenuContext context, Pagination pagination, String text) {
        return MenuPlaceholders.apply(player, text, context.placeholders(), pagination);
    }
}
