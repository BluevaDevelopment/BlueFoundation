package net.blueva.foundation.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Adventure-specific item helpers. These methods are only usable on servers
 * that provide Adventure natively. For Spigot/Bukkit compatibility use the
 * {@link String}-based methods in {@link Items}.
 */
public class AdventureItems {

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    protected AdventureItems() {
    }

    public static ItemStack name(ItemStack item, Component name) {
        return Items.editMeta(item, new Items.MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setDisplayName(name == null ? null : LEGACY_SECTION.serialize(name));
            }
        });
    }

    public static ItemStack loreComponents(ItemStack item, Component... lore) {
        return loreComponents(item, lore == null ? null : Arrays.asList(lore));
    }

    public static ItemStack loreComponents(ItemStack item, Collection<? extends Component> lore) {
        return Items.editMeta(item, new Items.MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setLore(legacyComponentLines(lore));
            }
        });
    }

    public static ItemStack addLoreComponents(ItemStack item, Component... lines) {
        return addLoreComponents(item, lines == null ? null : Arrays.asList(lines));
    }

    public static ItemStack addLoreComponents(ItemStack item, Collection<? extends Component> lines) {
        return Items.editMeta(item, new Items.MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
                List<String> converted = legacyComponentLines(lines);
                if (converted != null) {
                    lore.addAll(converted);
                }
                meta.setLore(lore.isEmpty() ? null : lore);
            }
        });
    }

    private static List<String> legacyComponentLines(Collection<? extends Component> lines) {
        if (lines == null) {
            return null;
        }
        List<String> legacy = new ArrayList<String>();
        for (Component line : lines) {
            legacy.add(line == null ? null : LEGACY_SECTION.serialize(line));
        }
        return legacy;
    }
}
