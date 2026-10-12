package net.blueva.foundation.menus;

import net.blueva.foundation.menus.bedrock.Form;
import net.blueva.foundation.menus.bedrock.FormImage;
import net.blueva.foundation.menus.bedrock.Forms;
import net.blueva.foundation.menus.bedrock.ModalForm;
import net.blueva.foundation.menus.bedrock.SimpleForm;
import net.blueva.foundation.messages.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Draws a menu as a Bedrock form.
 *
 * <p>The same definition that becomes a chest inventory for a Java player
 * becomes a button list here. Slots are ignored - a form has no grid - so the
 * order buttons appear in is the order they were declared, with the page of
 * dynamic entries after them and navigation last, which is where a Bedrock
 * player expects to scroll to find it.</p>
 */
final class FormRenderer {

    private FormRenderer() {
    }

    /**
     * Build and send the form.
     *
     * @param plugin  the plugin opening it
     * @param context the view to draw
     * @return {@code true} if a transport accepted the form
     */
    static boolean open(Plugin plugin, MenuContext context) {
        MenuDefinition definition = context.definition();
        Player player = context.player();
        Pagination pagination = context.pagination(true);

        List<MenuButton> visible = visibleButtons(context, pagination);

        Form form = definition.formStyle() == FormStyle.MODAL && visible.size() == 2
                ? modal(plugin, context, pagination, visible)
                : simple(plugin, context, pagination, visible);

        return Forms.send(plugin, player, form);
    }

    /**
     * The buttons this form shows, in order: declared buttons, this page's
     * entries, then navigation.
     */
    static List<MenuButton> visibleButtons(MenuContext context, Pagination pagination) {
        List<MenuButton> visible = new ArrayList<MenuButton>();
        MenuDefinition definition = context.definition();
        Player player = context.player();

        for (MenuButton button : definition.buttons().values()) {
            if (ChestRenderer.visible(player, context, pagination, button)) {
                visible.add(button);
            }
        }

        List<MenuButton> entries = context.entries();
        for (int i = pagination.start(); i < pagination.end(); i++) {
            MenuButton entry = entries.get(i);
            if (entry != null && ChestRenderer.visible(player, context, pagination, entry)) {
                visible.add(entry);
            }
        }
        if (definition.layout() == MenuLayout.PAGINATED) {
            if (pagination.hasPrevious() && definition.previousButton() != null) {
                visible.add(definition.previousButton());
            }
            if (pagination.hasNext() && definition.nextButton() != null) {
                visible.add(definition.nextButton());
            }
        }

        if (definition.closeButton() != null
                && ChestRenderer.visible(player, context, pagination, definition.closeButton())) {
            visible.add(definition.closeButton());
        }
        return visible;
    }

    private static Form simple(Plugin plugin, MenuContext context,
                               Pagination pagination, List<MenuButton> buttons) {
        MenuDefinition definition = context.definition();
        Player player = context.player();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(legacy(player, context, pagination, definition.title()))
                .content(body(player, context, pagination, definition, buttons));

        for (MenuButton button : buttons) {
            builder.button(label(player, context, pagination, button),
                    image(player, context, pagination, button),
                    press(plugin, context, button));
        }
        return builder.build();
    }

    private static Form modal(Plugin plugin, MenuContext context,
                              Pagination pagination, List<MenuButton> buttons) {
        MenuDefinition definition = context.definition();
        Player player = context.player();
        MenuButton first = buttons.get(0);
        MenuButton second = buttons.get(1);

        return ModalForm.builder()
                .title(legacy(player, context, pagination, definition.title()))
                .content(body(player, context, pagination, definition, buttons))
                .first(legacy(player, context, pagination, first.name()), press(plugin, context, first))
                .second(legacy(player, context, pagination, second.name()), press(plugin, context, second))
                .build();
    }

    /**
     * A form's body carries what lore carries on Java: the menu's own content
     * lines, and the "nothing here" line when a paginated menu is empty, since
     * a form with no buttons is otherwise a blank screen with no explanation.
     */
    private static String body(Player player, MenuContext context, Pagination pagination,
                               MenuDefinition definition, List<MenuButton> buttons) {
        StringBuilder out = new StringBuilder();
        for (String line : definition.content()) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(legacy(player, context, pagination, line));
        }
        boolean empty = definition.layout() == MenuLayout.PAGINATED && context.entries().isEmpty();
        if (empty && definition.emptyMessage() != null) {
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append(legacy(player, context, pagination, definition.emptyMessage()));
        }
        return out.toString();
    }

    /**
     * A Bedrock button's label is one string, but it wraps on newlines, so the
     * lore that a Java player reads in the tooltip is folded into the label
     * rather than dropped. Losing it is the usual reason a plugin ends up
     * maintaining a second set of Bedrock-only configs.
     */
    private static String label(Player player, MenuContext context,
                                Pagination pagination, MenuButton button) {
        StringBuilder out = new StringBuilder(legacy(player, context, pagination, button.name()));
        for (String line : button.lore()) {
            String resolved = legacy(player, context, pagination, line);
            if (resolved == null) {
                continue;
            }
            out.append('\n').append(resolved);
        }
        // A trailing blank lore line is a Java layout trick; on a button it is
        // just an empty row, so the label is trimmed back.
        while (out.length() > 0 && (out.charAt(out.length() - 1) == '\n'
                || out.charAt(out.length() - 1) == ' ')) {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    private static Consumer<Player> press(final Plugin plugin, final MenuContext context,
                                          final MenuButton button) {
        return new Consumer<Player>() {
            @Override
            public void accept(Player player) {
                if (button.handler() != null) {
                    button.handler().accept(player);
                }
                MenuActionExecutor.run(plugin, player, button.actions(), null, context);
            }
        };
    }

    private static FormImage image(Player player, MenuContext context,
                                   Pagination pagination, MenuButton button) {
        if (button.image() == null || button.image().isEmpty()) {
            return null;
        }
        return FormImage.of(MenuPlaceholders.apply(player, button.image(),
                context.placeholders(), pagination));
    }

    private static String legacy(Player player, MenuContext context, Pagination pagination, String text) {
        return Messages.legacySection(
                MenuPlaceholders.apply(player, text, context.placeholders(), pagination));
    }
}
