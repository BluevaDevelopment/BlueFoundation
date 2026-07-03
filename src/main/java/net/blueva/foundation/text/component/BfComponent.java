package net.blueva.foundation.text.component;

import java.util.List;

/**
 * Lightweight component tree used by BlueFoundation's own text system when
 * Adventure is not available at runtime.
 */
public interface BfComponent {

    /**
     * Literal text content of this node. May be empty for parent/container nodes.
     */
    String content();

    /**
     * Child components appended to this node.
     */
    List<BfComponent> children();

    /**
     * Style applied to this node's own content.
     */
    BfStyle style();

    /**
     * Appends a child component and returns this component.
     */
    BfComponent append(BfComponent child);

    /**
     * Appends literal text with the current style and returns this component.
     */
    BfComponent append(String text);

    /**
     * Creates an empty component.
     */
    static BfComponent empty() {
        return new BfTextComponent("");
    }

    /**
     * Creates a component with literal text.
     */
    static BfComponent text(String text) {
        return new BfTextComponent(text);
    }
}
