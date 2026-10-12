package net.blueva.foundation.menus;

import net.blueva.foundation.messages.Messages;
import net.blueva.foundation.sounds.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the actions attached to a menu button.
 *
 * <p>Actions run in declaration order and keep running after one of them
 * closes the menu, because {@code ["message: bye", "close"]} and
 * {@code ["close", "message: bye"]} should both send the message. The one
 * exception is a navigation action: opening another menu or changing page
 * makes everything after it meaningless, so the run stops there.</p>
 */
final class MenuActionExecutor {

    private static final Map<String, MenuActionHandler> HANDLERS =
            new ConcurrentHashMap<String, MenuActionHandler>();

    private MenuActionExecutor() {
    }

    static void register(String id, MenuActionHandler handler) {
        if (id == null || id.trim().isEmpty() || handler == null) {
            return;
        }
        HANDLERS.put(id.trim().toLowerCase(Locale.ROOT), handler);
    }

    static void unregister(String id) {
        if (id != null) {
            HANDLERS.remove(id.trim().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * @param plugin  the plugin that opened the menu
     * @param player  who pressed
     * @param actions what to run
     * @param click   the click, or {@code null} for a form button press
     * @param context the view being acted on
     */
    static void run(Plugin plugin, Player player, List<MenuAction> actions,
                    ClickType click, MenuContext context) {
        if (actions == null || actions.isEmpty() || player == null) {
            return;
        }
        for (MenuAction action : actions) {
            if (!action.matches(click)) {
                continue;
            }
            String value = MenuPlaceholders.apply(player, action.value(),
                    context == null ? null : context.placeholders(),
                    pagination(context, player));
            if (execute(plugin, player, action.type(), value, context)) {
                return;
            }
        }
    }

    /**
     * @return whether this action navigated away, ending the run
     */
    private static boolean execute(Plugin plugin, Player player, MenuActionType type,
                                   String value, MenuContext context) {
        switch (type) {
            case COMMAND:
                player.performCommand(stripSlash(value));
                return false;
            case CONSOLE:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(value));
                return false;
            case MESSAGE:
                Messages.send(player, value);
                return false;
            case SOUND:
                Sounds.play(player, 1f, 1f, value);
                return false;
            case CLOSE:
                Menus.close(player);
                return false;
            case OPEN_MENU:
                openMenu(plugin, player, value, context);
                return true;
            case BACK:
                goBack(plugin, player, context);
                return true;
            case PAGE:
                if (context == null || context.definition().layout() != MenuLayout.PAGINATED) {
                    return false;
                }
                changePage(plugin, player, value, context);
                return true;
            case CUSTOM:
                runCustom(plugin, player, value, context);
                return false;
            case NONE:
            default:
                return false;
        }
    }

    private static void openMenu(Plugin plugin, Player player, String value, MenuContext context) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        MenuDefinition target = Menus.definition(value.trim());
        if (target == null) {
            plugin.getLogger().warning("[BlueFoundation] Menu action 'menu: " + value.trim()
                    + "' points at a menu that is not registered.");
            return;
        }
        MenuContext opened = new MenuContext(player, target);
        if (context != null) {
            opened.placeholders().putAll(context.placeholders());
            opened.data().putAll(context.data());
            opened.back(context.definition().id(), context.page());
        }
        Menus.openLater(plugin, opened);
    }

    private static void goBack(Plugin plugin, Player player, MenuContext context) {
        MenuDefinition previous = context == null ? null : Menus.definition(context.backMenu());
        if (previous == null) {
            Menus.close(player);
            return;
        }
        MenuContext target = new MenuContext(player, previous).page(context.backPage());
        target.placeholders().putAll(context.placeholders());
        target.data().putAll(context.data());
        Menus.openLater(plugin, target);
    }

    private static void changePage(Plugin plugin, Player player, String value, MenuContext context) {
        if (context == null || context.definition().layout() != MenuLayout.PAGINATED) {
            return;
        }
        Pagination pagination = pagination(context, player);
        String target = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        int page;
        if ("next".equals(target) || target.isEmpty()) {
            page = pagination.next();
        } else if ("previous".equals(target) || "prev".equals(target) || "back".equals(target)) {
            page = pagination.previous();
        } else if ("first".equals(target)) {
            page = 0;
        } else if ("last".equals(target)) {
            page = pagination.totalPages() - 1;
        } else {
            try {
                page = Integer.parseInt(target) - 1;
            } catch (NumberFormatException e) {
                return;
            }
        }
        Menus.openLater(plugin, context.page(page));
    }

    private static void runCustom(Plugin plugin, Player player, String value, MenuContext context) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String trimmed = value.trim();
        int space = trimmed.indexOf(' ');
        String id = (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT);
        String payload = space < 0 ? "" : trimmed.substring(space + 1).trim();

        MenuActionHandler handler = HANDLERS.get(id);
        if (handler == null) {
            plugin.getLogger().warning("[BlueFoundation] Menu action 'custom: " + trimmed
                    + "' has no handler registered for '" + id + "'.");
            return;
        }
        try {
            if (!handler.handle(player, payload, context)) {
                plugin.getLogger().warning("[BlueFoundation] Menu action handler '" + id
                        + "' did not handle '" + payload + "'.");
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("[BlueFoundation] Menu action handler '" + id
                    + "' threw: " + e);
        }
    }

    /**
     * Page size depends on which surface the player is looking at - a form
     * fits ten buttons, a chest fits however many slots the menu set aside -
     * so the page arithmetic has to know who is asking.
     */
    private static Pagination pagination(MenuContext context, Player player) {
        if (context == null) {
            return null;
        }
        return context.pagination(Menus.isBedrockPlayer(player));
    }

    private static String stripSlash(String command) {
        String trimmed = command == null ? "" : command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
