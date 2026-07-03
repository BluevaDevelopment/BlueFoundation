package net.blueva.foundation.text.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link BfComponent}.
 */
public final class BfTextComponent implements BfComponent {

    private final String content;
    private final List<BfComponent> children = new ArrayList<BfComponent>();
    private final BfStyle style = new BfStyle();

    public BfTextComponent(String content) {
        this.content = content != null ? content : "";
    }

    @Override
    public String content() {
        return content;
    }

    @Override
    public List<BfComponent> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public BfStyle style() {
        return style;
    }

    @Override
    public BfComponent append(BfComponent child) {
        if (child != null) {
            children.add(child);
        }
        return this;
    }

    @Override
    public BfComponent append(String text) {
        children.add(new BfTextComponent(text));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BfTextComponent)) return false;
        BfTextComponent that = (BfTextComponent) o;
        return content.equals(that.content)
                && children.equals(that.children)
                && style.equals(that.style);
    }

    @Override
    public int hashCode() {
        int result = content.hashCode();
        result = 31 * result + children.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BfTextComponent{content='" + content + "', children=" + children + ", style=" + style + "}";
    }
}
