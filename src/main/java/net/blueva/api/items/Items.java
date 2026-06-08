package net.blueva.api.items;

import net.blueva.api.materials.Materials;
import net.blueva.api.text.Text;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Multi-version item helpers. Text input is MiniMessage-first. */
public class Items {

    protected Items() {
    }

    public static Builder builder(Material material) {
        return new Builder(material);
    }

    public static Builder builder(String... materialNames) {
        return new Builder(Materials.require(materialNames));
    }

    public static ItemStack of(Material material) {
        return builder(material).build();
    }

    public static ItemStack of(Material material, int amount) {
        return builder(material).amount(amount).build();
    }

    public static ItemStack of(String... materialNames) {
        return builder(materialNames).build();
    }

    public static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static ItemStack amount(ItemStack item, int amount) {
        if (item != null) {
            item.setAmount(safeAmount(amount));
        }
        return item;
    }

    public static ItemStack durability(ItemStack item, short durability) {
        if (item != null) {
            item.setDurability(durability);
        }
        return item;
    }

    public static ItemStack name(ItemStack item, String name) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setDisplayName(isBlank(name) ? null : Text.legacySection(name));
            }
        });
    }

    public static ItemStack lore(ItemStack item, String... lore) {
        return lore(item, lore == null ? null : Arrays.asList(lore));
    }

    public static ItemStack lore(ItemStack item, Collection<String> lore) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setLore(legacyLines(lore));
            }
        });
    }

    public static ItemStack addLore(ItemStack item, String... lines) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
                List<String> extra = legacyLines(lines == null ? null : Arrays.asList(lines));
                if (extra != null) {
                    lore.addAll(extra);
                }
                meta.setLore(lore.isEmpty() ? null : lore);
            }
        });
    }

    public static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        return enchant(item, enchantment, level, true);
    }

    public static ItemStack enchant(ItemStack item, Enchantment enchantment, int level, boolean unsafe) {
        if (item == null || enchantment == null || level <= 0) {
            return item;
        }
        if (unsafe) {
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            item.addEnchantment(enchantment, level);
        }
        return item;
    }

    public static ItemStack enchant(ItemStack item, String enchantmentName, int level) {
        return enchant(item, resolveEnchantment(enchantmentName), level, true);
    }

    public static ItemStack glow(ItemStack item) {
        enchant(item, Enchantment.DURABILITY, 1, true);
        return flags(item, "HIDE_ENCHANTS");
    }

    public static ItemStack flags(ItemStack item, String... flagNames) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (flagNames == null) {
                    return;
                }
                for (String flagName : flagNames) {
                    ItemFlag flag = resolveFlag(flagName);
                    if (flag != null) {
                        meta.addItemFlags(flag);
                    }
                }
            }
        });
    }

    public static ItemStack hideAllFlags(ItemStack item) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.addItemFlags(ItemFlag.values());
            }
        });
    }

    public static ItemStack unbreakable(ItemStack item, boolean unbreakable) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                setUnbreakable(meta, unbreakable);
            }
        });
    }

    public static ItemStack skullOwner(ItemStack item, String owner) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (meta instanceof SkullMeta && !isBlank(owner)) {
                    ((SkullMeta) meta).setOwner(owner);
                }
            }
        });
    }

    public static ItemStack leatherColor(ItemStack item, Color color) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (meta instanceof LeatherArmorMeta && color != null) {
                    ((LeatherArmorMeta) meta).setColor(color);
                }
            }
        });
    }

    public static Enchantment resolveEnchantment(String name) {
        if (isBlank(name)) {
            return null;
        }

        String normalized = normalize(name);
        Enchantment enchantment = Enchantment.getByName(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        String legacy = modernToLegacyEnchantment(normalized);
        if (legacy != null) {
            enchantment = Enchantment.getByName(legacy);
            if (enchantment != null) {
                return enchantment;
            }
        }

        try {
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey");
            Object key = namespacedKey.getMethod("minecraft", String.class).invoke(null, normalized.toLowerCase());
            Method getByKey = Enchantment.class.getMethod("getByKey", namespacedKey);
            Object result = getByKey.invoke(null, key);
            return result instanceof Enchantment ? (Enchantment) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack editMeta(ItemStack item, MetaEditor editor) {
        if (item == null || editor == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        editor.edit(meta);
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> legacyLines(Collection<String> lines) {
        if (lines == null) {
            return null;
        }
        List<String> legacy = new ArrayList<>();
        for (String line : lines) {
            legacy.add(Text.legacySection(line));
        }
        return legacy;
    }

    private static ItemFlag resolveFlag(String name) {
        if (isBlank(name)) {
            return null;
        }
        try {
            return ItemFlag.valueOf(normalize(name));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setUnbreakable(ItemMeta meta, boolean unbreakable) {
        try {
            Method method = meta.getClass().getMethod("setUnbreakable", boolean.class);
            method.invoke(meta, unbreakable);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Object spigot = meta.getClass().getMethod("spigot").invoke(meta);
            Method method = spigot.getClass().getMethod("setUnbreakable", boolean.class);
            method.invoke(spigot, unbreakable);
        } catch (Throwable ignored) {
        }
    }

    private static String modernToLegacyEnchantment(String normalized) {
        if ("SHARPNESS".equals(normalized)) return "DAMAGE_ALL";
        if ("SMITE".equals(normalized)) return "DAMAGE_UNDEAD";
        if ("BANE_OF_ARTHROPODS".equals(normalized)) return "DAMAGE_ARTHROPODS";
        if ("EFFICIENCY".equals(normalized)) return "DIG_SPEED";
        if ("UNBREAKING".equals(normalized)) return "DURABILITY";
        if ("FORTUNE".equals(normalized)) return "LOOT_BONUS_BLOCKS";
        if ("POWER".equals(normalized)) return "ARROW_DAMAGE";
        if ("PUNCH".equals(normalized)) return "ARROW_KNOCKBACK";
        if ("FLAME".equals(normalized)) return "ARROW_FIRE";
        if ("INFINITY".equals(normalized)) return "ARROW_INFINITE";
        return null;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private static int safeAmount(int amount) {
        return Math.max(1, amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private interface MetaEditor {
        void edit(ItemMeta meta);
    }

    public static class Builder {
        private final ItemStack item;

        protected Builder(Material material) {
            if (material == null) {
                throw new IllegalArgumentException("material cannot be null");
            }
            this.item = new ItemStack(material);
        }

        protected Builder(ItemStack item) {
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null");
            }
            this.item = item.clone();
        }

        public Builder amount(int amount) {
            Items.amount(item, amount);
            return this;
        }

        public Builder durability(short durability) {
            Items.durability(item, durability);
            return this;
        }

        public Builder data(int data) {
            Items.durability(item, (short) data);
            return this;
        }

        public Builder name(String name) {
            Items.name(item, name);
            return this;
        }

        public Builder lore(String... lore) {
            Items.lore(item, lore);
            return this;
        }

        public Builder lore(Collection<String> lore) {
            Items.lore(item, lore);
            return this;
        }

        public Builder addLore(String... lines) {
            Items.addLore(item, lines);
            return this;
        }

        public Builder enchant(Enchantment enchantment, int level) {
            Items.enchant(item, enchantment, level, true);
            return this;
        }

        public Builder enchant(String enchantment, int level) {
            Items.enchant(item, enchantment, level);
            return this;
        }

        public Builder glow() {
            Items.glow(item);
            return this;
        }

        public Builder flags(String... flagNames) {
            Items.flags(item, flagNames);
            return this;
        }

        public Builder hideAllFlags() {
            Items.hideAllFlags(item);
            return this;
        }

        public Builder unbreakable() {
            return unbreakable(true);
        }

        public Builder unbreakable(boolean unbreakable) {
            Items.unbreakable(item, unbreakable);
            return this;
        }

        public Builder skullOwner(String owner) {
            Items.skullOwner(item, owner);
            return this;
        }

        public Builder leatherColor(Color color) {
            Items.leatherColor(item, color);
            return this;
        }

        public ItemStack build() {
            return item.clone();
        }
    }
}
