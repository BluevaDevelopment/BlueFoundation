package net.blueva.foundation.menus;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One entry in a menu, described once for both editions.
 *
 * <p>A Java client renders this as an item in a slot; a Bedrock client renders
 * it as a button in a form. Which fields each edition reads differs -
 * {@code material} and {@code slots} mean nothing to a form, {@code image}
 * means nothing to an inventory - but the label, the visibility rule and the
 * actions are shared, which is the whole point of describing a button once.</p>
 */
public final class MenuButton {

    private final ItemStack item;
    private final String id;
    private final String name;
    private final List<String> lore;
    private final List<String> materials;
    private final int amount;
    private final List<Integer> slots;
    private final String skull;
    private final String image;
    private final boolean glow;
    private final boolean hideFlags;
    private final boolean hideTooltip;
    private final List<String> enchantments;
    private final String visibleIf;
    private final List<MenuAction> actions;
    private final Consumer<Player> handler;

    private MenuButton(Builder builder) {
        this.item = builder.item == null ? null : builder.item.clone();
        this.id = builder.id;
        this.name = builder.name;
        this.lore = Collections.unmodifiableList(new ArrayList<String>(builder.lore));
        this.materials = Collections.unmodifiableList(new ArrayList<String>(builder.materials));
        this.amount = Math.max(1, builder.amount);
        this.slots = Collections.unmodifiableList(new ArrayList<Integer>(builder.slots));
        this.skull = builder.skull;
        this.image = builder.image;
        this.glow = builder.glow;
        this.hideFlags = builder.hideFlags;
        this.hideTooltip = builder.hideTooltip;
        this.enchantments = Collections.unmodifiableList(new ArrayList<String>(builder.enchantments));
        this.visibleIf = builder.visibleIf;
        this.actions = Collections.unmodifiableList(new ArrayList<MenuAction>(builder.actions));
        this.handler = builder.handler;
    }

    /**
     * @param id the button's key within its menu
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * @return the button's key within its menu
     */
    /** Exact Java item blueprint, including PDC and all component metadata. */
    public ItemStack item() { return item == null ? null : item.clone(); }

    public String id() {
        return id;
    }

    /**
     * @return the display name, in MiniMessage
     */
    public String name() {
        return name;
    }

    /**
     * @return the lore lines, in MiniMessage
     */
    public List<String> lore() {
        return lore;
    }

    /**
     * @return candidate material names, first match wins across versions
     */
    public List<String> materials() {
        return materials;
    }

    /**
     * @return the stack size
     */
    public int amount() {
        return amount;
    }

    /**
     * @return every slot this button occupies; empty means "wherever it fits"
     */
    public List<Integer> slots() {
        return slots;
    }

    /**
     * @return the first slot, or {@code -1} if the button is unplaced
     */
    public int slot() {
        return slots.isEmpty() ? -1 : slots.get(0).intValue();
    }

    /**
     * @return a player name or a base64 texture for a head, or {@code null}
     */
    public String skull() {
        return skull;
    }

    /**
     * @return the Bedrock button icon, a URL or resource path, or {@code null}
     */
    public String image() {
        return image;
    }

    /**
     * @return whether the item should shimmer
     */
    public boolean glow() {
        return glow;
    }

    /**
     * @return whether to hide attribute and enchantment tooltips
     */
    public boolean hideFlags() {
        return hideFlags;
    }

    /**
     * Whether to hide the tooltip box entirely.
     *
     * <p>A blank name removes the text; this removes the box. A filler pane
     * wants both, or hovering the gap between two groups of buttons opens an
     * empty window.
     *
     * @return whether the tooltip is hidden
     */
    public boolean hideTooltip() {
        return hideTooltip;
    }

    /**
     * @return enchantments as {@code NAME:LEVEL} strings
     */
    public List<String> enchantments() {
        return enchantments;
    }

    /**
     * The condition deciding whether this button is shown at all.
     *
     * <p>BlueFoundation does not evaluate expressions. The string is passed
     * through the menu's placeholder resolver and the result is read as a
     * boolean, so a caller who has PlaceholderAPI wires
     * {@code "%vault_eco_balance%"} here and it works.</p>
     *
     * @return the condition, or {@code null} to always show
     */
    public String visibleIf() {
        return visibleIf;
    }

    /**
     * @return what happens when the button is pressed
     */
    public List<MenuAction> actions() {
        return actions;
    }

    /**
     * @return code to run when the button is pressed, or {@code null}
     */
    public Consumer<Player> handler() {
        return handler;
    }

    /**
     * Copy this button with every piece of text run through a replacer.
     *
     * @param replacer applied to the name, lore, skull, image, condition and
     *                 action values
     * @return the rewritten button
     */
    public MenuButton withPlaceholders(Function<String, String> replacer) {
        if (replacer == null) {
            return this;
        }
        Builder copy = toBuilder();
        copy.name = replacer.apply(name);
        copy.lore.clear();
        for (String line : lore) {
            copy.lore.add(replacer.apply(line));
        }
        copy.skull = skull == null ? null : replacer.apply(skull);
        copy.image = image == null ? null : replacer.apply(image);
        copy.visibleIf = visibleIf == null ? null : replacer.apply(visibleIf);
        copy.actions.clear();
        for (MenuAction action : actions) {
            copy.actions.add(action.withValue(replacer.apply(action.value())));
        }
        return copy.build();
    }

    /**
     * @return a builder pre-filled with this button's state
     */
    public Builder toBuilder() {
        Builder builder = new Builder(id);
        builder.name = name;
        builder.lore.addAll(lore);
        builder.materials.addAll(materials);
        builder.amount = amount;
        builder.slots.addAll(slots);
        builder.skull = skull;
        builder.image = image;
        builder.glow = glow;
        builder.hideFlags = hideFlags;
        builder.enchantments.addAll(enchantments);
        builder.visibleIf = visibleIf;
        builder.actions.addAll(actions);
        builder.handler = handler;
        return builder;
    }

    /** Builds a {@link MenuButton}. */
    public static final class Builder {
        private ItemStack item;
        private final String id;
        private String name = "";
        private final List<String> lore = new ArrayList<String>();
        private final List<String> materials = new ArrayList<String>();
        private int amount = 1;
        private final List<Integer> slots = new ArrayList<Integer>();
        private String skull;
        private String image;
        private boolean glow;
        private boolean hideFlags;
        private boolean hideTooltip;
        private final List<String> enchantments = new ArrayList<String>();
        private String visibleIf;
        private final List<MenuAction> actions = new ArrayList<MenuAction>();
        private Consumer<Player> handler;

        Builder(String id) {
            this.id = id == null ? "" : id;
        }

        /**
         * @param name the display name, in MiniMessage
         * @return this builder
         */
        /** Cloned on input and output; renderers never mutate the caller's item. */
        public Builder item(ItemStack item) { this.item = item == null ? null : item.clone(); return this; }

        public Builder name(String name) {
            this.name = name == null ? "" : name;
            return this;
        }

        /**
         * @param lines lore lines, in MiniMessage
         * @return this builder
         */
        public Builder lore(String... lines) {
            return lore(Arrays.asList(lines));
        }

        /**
         * @param lines lore lines, in MiniMessage
         * @return this builder
         */
        public Builder lore(Collection<String> lines) {
            lore.clear();
            if (lines != null) {
                for (String line : lines) {
                    lore.add(line == null ? "" : line);
                }
            }
            return this;
        }

        /**
         * Set the item type, with fallbacks for older servers.
         *
         * @param names material names, first one that exists wins
         * @return this builder
         */
        public Builder material(String... names) {
            materials.clear();
            if (names != null) {
                for (String name : names) {
                    if (name != null && !name.trim().isEmpty()) {
                        materials.add(name.trim());
                    }
                }
            }
            return this;
        }

        /**
         * @param names material names, first one that exists wins
         * @return this builder
         */
        public Builder material(Collection<String> names) {
            return material(names == null ? new String[0] : names.toArray(new String[0]));
        }

        /**
         * @param amount the stack size
         * @return this builder
         */
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        /**
         * @param slots where the button sits in a chest menu
         * @return this builder
         */
        public Builder slots(int... slots) {
            this.slots.clear();
            if (slots != null) {
                for (int slot : slots) {
                    this.slots.add(Integer.valueOf(slot));
                }
            }
            return this;
        }

        /**
         * @param slots where the button sits in a chest menu
         * @return this builder
         */
        public Builder slots(Collection<Integer> slots) {
            this.slots.clear();
            if (slots != null) {
                for (Integer slot : slots) {
                    if (slot != null) {
                        this.slots.add(slot);
                    }
                }
            }
            return this;
        }

        /**
         * @param slot where the button sits in a chest menu
         * @return this builder
         */
        public Builder slot(int slot) {
            return slots(slot);
        }

        /**
         * @param skull a player name or a base64 texture
         * @return this builder
         */
        public Builder skull(String skull) {
            this.skull = skull;
            return this;
        }

        /**
         * @param image a URL or client resource path for the Bedrock button
         * @return this builder
         */
        public Builder image(String image) {
            this.image = image;
            return this;
        }

        /**
         * @param glow whether the item shimmers
         * @return this builder
         */
        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        /**
         * @param hideFlags whether to hide attribute and enchantment tooltips
         * @return this builder
         */
        public Builder hideFlags(boolean hideFlags) {
            this.hideFlags = hideFlags;
            return this;
        }

        /**
         * @param hideTooltip whether to hide the tooltip box entirely
         * @return this builder
         */
        public Builder hideTooltip(boolean hideTooltip) {
            this.hideTooltip = hideTooltip;
            return this;
        }

        /**
         * @param enchantments {@code NAME:LEVEL} strings
         * @return this builder
         */
        public Builder enchantments(Collection<String> enchantments) {
            this.enchantments.clear();
            if (enchantments != null) {
                this.enchantments.addAll(enchantments);
            }
            return this;
        }

        /**
         * @param visibleIf a condition resolved through the placeholder hook
         * @return this builder
         */
        public Builder visibleIf(String visibleIf) {
            this.visibleIf = visibleIf;
            return this;
        }

        /**
         * @param actions what happens when the button is pressed
         * @return this builder
         */
        public Builder actions(Collection<MenuAction> actions) {
            this.actions.clear();
            if (actions != null) {
                this.actions.addAll(actions);
            }
            return this;
        }

        /**
         * @param action an action to append
         * @return this builder
         */
        public Builder action(MenuAction action) {
            if (action != null) {
                actions.add(action);
            }
            return this;
        }

        /**
         * @param raw an action in short or flat form
         * @return this builder
         */
        public Builder action(String raw) {
            return action(MenuAction.parse(raw));
        }

        /**
         * Run code directly, instead of going through the action vocabulary.
         *
         * @param handler what to run when the button is pressed
         * @return this builder
         */
        public Builder onClick(Consumer<Player> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * @return the finished button
         */
        public MenuButton build() {
            return new MenuButton(this);
        }
    }
}
