package net.blueva.foundation.text.serializer;

import net.blueva.foundation.text.component.BfComponent;

/**
 * Serializes a BlueFoundation component tree to plain text.
 */
public final class PlainSerializer {

    private PlainSerializer() {
    }

    public static String serialize(BfComponent component) {
        if (component == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        append(component, builder);
        return builder.toString();
    }

    private static void append(BfComponent component, StringBuilder builder) {
        builder.append(component.content());
        for (BfComponent child : component.children()) {
            append(child, builder);
        }
    }
}
