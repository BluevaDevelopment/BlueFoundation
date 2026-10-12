package net.blueva.foundation.menus;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about one player's view of one menu: which page they are on, what
 * data was handed in, and where "back" goes.
 *
 * <p>A {@link MenuDefinition} is shared and immutable; a context is per-open
 * and mutable, and is what a re-render reads to redraw the same view.</p>
 */
public final class MenuContext {

    private final Player player;
    private final MenuDefinition definition;
    private final List<MenuButton> entries = new ArrayList<MenuButton>();
    private final Map<String, String> placeholders = new LinkedHashMap<String, String>();
    private final Map<String, Object> data = new LinkedHashMap<String, Object>();
    private int page;
    private String backMenu;
    private int backPage;

    /**
     * @param player     who is looking
     * @param definition what they are looking at
     */
    public MenuContext(Player player, MenuDefinition definition) {
        this.player = player;
        this.definition = definition;
    }

    /**
     * @return who the menu is being drawn for
     */
    public Player player() {
        return player;
    }

    /**
     * @return the menu being drawn
     */
    public MenuDefinition definition() {
        return definition;
    }

    /**
     * @return the current page, zero-based
     */
    public int page() {
        return page;
    }

    /**
     * @param page the page to show, zero-based
     * @return this context
     */
    public MenuContext page(int page) {
        this.page = definition.layout() == MenuLayout.PAGINATED ? Math.max(0, page) : 0;
        return this;
    }

    /**
     * Runtime entries, drawn in both static and paginated menus.
     *
     * @return the live list, safe to add to before opening
     */
    public List<MenuButton> entries() {
        return entries;
    }

    /**
     * @param entries the runtime entries to draw
     * @return this context
     */
    public MenuContext entries(Collection<MenuButton> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    /**
     * Literal text replacements applied to every string in the menu before it
     * is drawn, on top of whatever global resolver is installed.
     *
     * @return the live map
     */
    public Map<String, String> placeholders() {
        return placeholders;
    }

    /**
     * @param key   the text to replace, such as {@code "{house}"}
     * @param value what to replace it with
     * @return this context
     */
    public MenuContext placeholder(String key, String value) {
        if (key != null) {
            placeholders.put(key, value == null ? "" : value);
        }
        return this;
    }

    /**
     * Arbitrary state the opening plugin wants back in its action handlers -
     * the house being edited, the game being joined - without having to keep
     * its own map keyed by player.
     *
     * @return the live map
     */
    public Map<String, Object> data() {
        return data;
    }

    /**
     * @param key   a name
     * @param value the value
     * @return this context
     */
    public MenuContext data(String key, Object value) {
        if (key != null) {
            data.put(key, value);
        }
        return this;
    }

    /**
     * @param key a name
     * @return the value, or {@code null}
     */
    public Object data(String key) {
        return data.get(key);
    }

    /**
     * @return the menu a {@code back} action returns to, or {@code null}
     */
    public String backMenu() {
        return backMenu;
    }

    /**
     * @return the page a {@code back} action returns to
     */
    public int backPage() {
        return backPage;
    }

    /**
     * Remember where the player came from, so {@code back} has somewhere to go.
     *
     * @param menuId the menu id
     * @param page   the page within it
     * @return this context
     */
    public MenuContext back(String menuId, int page) {
        this.backMenu = menuId;
        this.backPage = Math.max(0, page);
        return this;
    }

    /**
     * @return the pagination state for this view
     */
    public Pagination pagination(boolean forForm) {
        if (definition.layout() != MenuLayout.PAGINATED) {
            return new Pagination(0, Math.max(1, entries.size()), entries.size());
        }
        return new Pagination(page, definition.pageSize(forForm), entries.size());
    }

    /**
     * @return an unmodifiable snapshot of the entries
     */
    public List<MenuButton> entriesView() {
        return Collections.unmodifiableList(entries);
    }
}
