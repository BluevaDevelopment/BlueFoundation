package net.blueva.foundation.text.serializer;

import net.blueva.foundation.text.component.BfColor;
import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.component.BfStyle;

/**
 * Serializes a BlueFoundation component tree to a legacy section-string.
 *
 * <p>On 1.16+ hex colors are emitted as {@code §x§R§R§G§G§B§B}; on older
 * servers they are down-sampled to the nearest legacy color.</p>
 */
public final class LegacySerializer {

    private static final char SECTION = '\u00A7';

    private LegacySerializer() {
    }

    public static String serialize(BfComponent component) {
        if (component == null) {
            return "";
        }
        Context context = new Context();
        append(component, context, new BfStyle());
        return context.builder.toString();
    }

    private static void append(BfComponent component, Context context, BfStyle parentStyle) {
        BfStyle style = parentStyle.merge(component.style());
        String content = component.content();
        if (!content.isEmpty()) {
            emitStyleChange(context, style);
            context.builder.append(content);
        }
        for (BfComponent child : component.children()) {
            append(child, context, style);
        }
    }

    private static void emitStyleChange(Context context, BfStyle newStyle) {
        BfStyle oldStyle = context.currentStyle;

        boolean needsReset = false;
        if (oldStyle.obfuscated() && !newStyle.obfuscated()) needsReset = true;
        if (oldStyle.bold() && !newStyle.bold()) needsReset = true;
        if (oldStyle.strikethrough() && !newStyle.strikethrough()) needsReset = true;
        if (oldStyle.underlined() && !newStyle.underlined()) needsReset = true;
        if (oldStyle.italic() && !newStyle.italic()) needsReset = true;

        if (needsReset) {
            context.builder.append(SECTION).append('r');
            oldStyle = new BfStyle();
        }

        BfColor newColor = newStyle.color();
        BfColor oldColor = oldStyle.color();
        boolean colorChanged = newColor != null && (oldColor == null || !newColor.equals(oldColor) || needsReset);
        if (colorChanged) {
            context.builder.append(newColor.legacySection());
        }

        // Re-emit decorations whenever the color changes as well; some clients reset
        // decorations after a color code (especially hex colors), so we make sure
        // bold/italic/etc. are applied again after each color transition.
        boolean reapplyDecorations = needsReset || colorChanged;
        if (newStyle.obfuscated() && (!oldStyle.obfuscated() || reapplyDecorations)) {
            context.builder.append(SECTION).append('k');
        }
        if (newStyle.bold() && (!oldStyle.bold() || reapplyDecorations)) {
            context.builder.append(SECTION).append('l');
        }
        if (newStyle.strikethrough() && (!oldStyle.strikethrough() || reapplyDecorations)) {
            context.builder.append(SECTION).append('m');
        }
        if (newStyle.underlined() && (!oldStyle.underlined() || reapplyDecorations)) {
            context.builder.append(SECTION).append('n');
        }
        if (newStyle.italic() && (!oldStyle.italic() || reapplyDecorations)) {
            context.builder.append(SECTION).append('o');
        }

        context.currentStyle = newStyle.copy();
    }

    private static final class Context {
        private final StringBuilder builder = new StringBuilder();
        private BfStyle currentStyle = new BfStyle();
    }
}
