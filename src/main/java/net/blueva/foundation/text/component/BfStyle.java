package net.blueva.foundation.text.component;

/**
 * Style state for a {@link BfComponent}. Holds color, decorations and
 * Adventure-only interactive data (hover/click events) as opaque objects.
 */
public final class BfStyle {

    private BfColor color;
    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;
    private boolean obfuscated;
    private Object hoverEvent;
    private Object clickEvent;
    private String insertion;

    public BfStyle() {
    }

    public BfStyle copy() {
        BfStyle copy = new BfStyle();
        copy.color = this.color;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underlined = this.underlined;
        copy.strikethrough = this.strikethrough;
        copy.obfuscated = this.obfuscated;
        copy.hoverEvent = this.hoverEvent;
        copy.clickEvent = this.clickEvent;
        copy.insertion = this.insertion;
        return copy;
    }

    /**
     * Returns a new style with the non-default values of {@code other} applied
     * over this style, preserving existing values when {@code other} has not
     * explicitly set them.
     */
    public BfStyle merge(BfStyle other) {
        BfStyle merged = copy();
        if (other.color != null) {
            merged.color = other.color;
        }
        if (other.bold) merged.bold = true;
        if (other.italic) merged.italic = true;
        if (other.underlined) merged.underlined = true;
        if (other.strikethrough) merged.strikethrough = true;
        if (other.obfuscated) merged.obfuscated = true;
        if (other.hoverEvent != null) merged.hoverEvent = other.hoverEvent;
        if (other.clickEvent != null) merged.clickEvent = other.clickEvent;
        if (other.insertion != null) merged.insertion = other.insertion;
        return merged;
    }

    public BfColor color() {
        return color;
    }

    public BfStyle color(BfColor color) {
        this.color = color;
        return this;
    }

    public boolean bold() {
        return bold;
    }

    public BfStyle bold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public boolean italic() {
        return italic;
    }

    public BfStyle italic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public boolean underlined() {
        return underlined;
    }

    public BfStyle underlined(boolean underlined) {
        this.underlined = underlined;
        return this;
    }

    public boolean strikethrough() {
        return strikethrough;
    }

    public BfStyle strikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public boolean obfuscated() {
        return obfuscated;
    }

    public BfStyle obfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
        return this;
    }

    public Object hoverEvent() {
        return hoverEvent;
    }

    public BfStyle hoverEvent(Object hoverEvent) {
        this.hoverEvent = hoverEvent;
        return this;
    }

    public Object clickEvent() {
        return clickEvent;
    }

    public BfStyle clickEvent(Object clickEvent) {
        this.clickEvent = clickEvent;
        return this;
    }

    public String insertion() {
        return insertion;
    }

    public BfStyle insertion(String insertion) {
        this.insertion = insertion;
        return this;
    }

    /**
     * Copies all fields from {@code other} into this style.
     */
    public void copyFrom(BfStyle other) {
        this.color = other.color;
        this.bold = other.bold;
        this.italic = other.italic;
        this.underlined = other.underlined;
        this.strikethrough = other.strikethrough;
        this.obfuscated = other.obfuscated;
        this.hoverEvent = other.hoverEvent;
        this.clickEvent = other.clickEvent;
        this.insertion = other.insertion;
    }

    /**
     * Returns true if this style does not apply any formatting.
     */
    public boolean isEmpty() {
        return color == null
                && !bold
                && !italic
                && !underlined
                && !strikethrough
                && !obfuscated
                && hoverEvent == null
                && clickEvent == null
                && insertion == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BfStyle)) return false;
        BfStyle other = (BfStyle) o;
        return bold == other.bold
                && italic == other.italic
                && underlined == other.underlined
                && strikethrough == other.strikethrough
                && obfuscated == other.obfuscated
                && (color == null ? other.color == null : color.equals(other.color));
    }

    @Override
    public int hashCode() {
        int result = color != null ? color.hashCode() : 0;
        result = 31 * result + (bold ? 1 : 0);
        result = 31 * result + (italic ? 1 : 0);
        result = 31 * result + (underlined ? 1 : 0);
        result = 31 * result + (strikethrough ? 1 : 0);
        result = 31 * result + (obfuscated ? 1 : 0);
        return result;
    }
}
