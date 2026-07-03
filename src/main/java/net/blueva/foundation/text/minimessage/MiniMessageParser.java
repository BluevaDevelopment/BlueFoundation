package net.blueva.foundation.text.minimessage;

import net.blueva.foundation.text.component.BfColor;
import net.blueva.foundation.text.component.BfComponent;
import net.blueva.foundation.text.component.BfStyle;
import net.blueva.foundation.text.component.BfTextComponent;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a MiniMessage-compatible subset into BlueFoundation's own component tree.
 *
 * <p>Supported tags:</p>
 * <ul>
 *   <li>Colors: {@code <red>}, {@code <#RRGGBB>}, {@code <color:#RRGGBB>}, {@code <c:red>}</li>
 *   <li>Decorations: {@code <bold>}/{@code <b>}, {@code <italic>}/{@code <i>},
 *       {@code <underlined>}/{@code <u>}, {@code <strikethrough>}/{@code <st>}/{@code <s>},
 *       {@code <obfuscated>}/{@code <obf>}</li>
 *   <li>{@code <reset>}</li>
 *   <li>{@code <gradient:#start:#end>...text...</gradient>} (2+ colors, optional phase)</li>
 *   <li>{@code <rainbow>...text...</rainbow>} (optional saturation)</li>
 *   <li>{@code <br>}, {@code <newline>}</li>
 * </ul>
 *
 * <p>Unsupported tags are stripped. Legacy {@code &} color codes are accepted as input.</p>
 */
public final class MiniMessageParser {

    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Map<String, String> LEGACY_COLORS = new HashMap<String, String>();
    private static final Map<String, String> LEGACY_FORMATS = new HashMap<String, String>();
    private static final Map<String, String> DECORATION_ALIASES = new HashMap<String, String>();

    static {
        LEGACY_COLORS.put("0", "black");
        LEGACY_COLORS.put("1", "dark_blue");
        LEGACY_COLORS.put("2", "dark_green");
        LEGACY_COLORS.put("3", "dark_aqua");
        LEGACY_COLORS.put("4", "dark_red");
        LEGACY_COLORS.put("5", "dark_purple");
        LEGACY_COLORS.put("6", "gold");
        LEGACY_COLORS.put("7", "gray");
        LEGACY_COLORS.put("8", "dark_gray");
        LEGACY_COLORS.put("9", "blue");
        LEGACY_COLORS.put("a", "green");
        LEGACY_COLORS.put("b", "aqua");
        LEGACY_COLORS.put("c", "red");
        LEGACY_COLORS.put("d", "light_purple");
        LEGACY_COLORS.put("e", "yellow");
        LEGACY_COLORS.put("f", "white");

        LEGACY_FORMATS.put("l", "bold");
        LEGACY_FORMATS.put("m", "strikethrough");
        LEGACY_FORMATS.put("n", "underlined");
        LEGACY_FORMATS.put("o", "italic");
        LEGACY_FORMATS.put("k", "obfuscated");
        LEGACY_FORMATS.put("r", "reset");

        DECORATION_ALIASES.put("b", "bold");
        DECORATION_ALIASES.put("i", "italic");
        DECORATION_ALIASES.put("u", "underlined");
        DECORATION_ALIASES.put("st", "strikethrough");
        DECORATION_ALIASES.put("s", "strikethrough");
        DECORATION_ALIASES.put("obf", "obfuscated");
    }

    private MiniMessageParser() {
    }

    public static BfComponent parse(String input) {
        if (input == null || input.isEmpty()) {
            return BfComponent.empty();
        }
        String normalized = normalizeLegacy(input);
        Parser parser = new Parser(normalized);
        return parser.parse();
    }

    private static String normalizeLegacy(String text) {
        // Hex colors: &#RRGGBB -> <color:#RRGGBB>
        Matcher hexMatcher = HEX_AMPERSAND.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<color:#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        text = sb.toString();

        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if ((current == '&' || current == '\u00A7') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                String lower = String.valueOf(next).toLowerCase(Locale.ROOT);
                String color = LEGACY_COLORS.get(lower);
                if (color != null) {
                    result.append('<').append(color).append('>');
                    i++;
                    continue;
                }
                String format = LEGACY_FORMATS.get(lower);
                if (format != null) {
                    result.append('<').append(format).append('>');
                    i++;
                    continue;
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private static final class Parser {

        private final String input;
        private final int length;
        private int pos = 0;

        Parser(String input) {
            this.input = input;
            this.length = input.length();
        }

        BfComponent parse() {
            ParseContext context = new ParseContext();
            parseContent(context, null);
            return context.root;
        }

        private void parseContent(ParseContext context, String expectedClose) {
            while (pos < length) {
                char current = input.charAt(pos);
                if (current == '\\' && pos + 1 < length && input.charAt(pos + 1) == '<') {
                    context.appendText("<");
                    pos += 2;
                    continue;
                }
                if (current != '<') {
                    int textStart = pos;
                    while (pos < length) {
                        char c = input.charAt(pos);
                        if (c == '<' || (c == '\\' && pos + 1 < length && input.charAt(pos + 1) == '<')) {
                            break;
                        }
                        pos++;
                    }
                    context.appendText(input.substring(textStart, pos));
                    continue;
                }

                Tag tag = parseTag();
                if (tag == null) {
                    // Not a valid tag, treat '<' as literal text.
                    context.appendText("<");
                    pos++;
                    continue;
                }

                if (tag.closing) {
                    String closeName = tag.name;
                    if (expectedClose != null && expectedClose.equals(closeName)) {
                        return;
                    }
                    // Gradient/rainbow tags are parsed as self-contained units and
                    // never push a style onto the context stack; ignore their closes.
                    if (closeName.equals("gradient") || closeName.equals("rainbow")) {
                        continue;
                    }
                    // If a decoration/color tag is being closed, pop its style.
                    context.popStyleIfMatches(closeName);
                    continue;
                }

                if (tag.name.equals("br") || tag.name.equals("newline")) {
                    context.appendText("\n");
                    continue;
                }

                if (tag.name.equals("reset")) {
                    context.resetStyle();
                    continue;
                }

                String decoration = resolveDecoration(tag.name);
                if (decoration != null) {
                    context.pushStyle(decoration);
                    applyDecoration(context.currentStyle(), decoration);
                    continue;
                }

                BfColor color = resolveColorTag(tag);
                if (color != null) {
                    context.pushStyle(tag.name);
                    context.currentStyle().color(color);
                    continue;
                }

                if (tag.name.equals("gradient")) {
                    int bodyEnd = findClosingTag("gradient", pos);
                    if (bodyEnd == -1) {
                        // Malformed gradient, ignore tag.
                        continue;
                    }
                    String body = input.substring(pos, bodyEnd);
                    pos = bodyEnd + "</gradient>".length();
                    BfComponent gradient = parseGradient(body, tag.args, context.currentStyle());
                    context.appendComponent(gradient);
                    continue;
                }

                if (tag.name.equals("rainbow")) {
                    int bodyEnd = findClosingTag("rainbow", pos);
                    if (bodyEnd == -1) {
                        continue;
                    }
                    String body = input.substring(pos, bodyEnd);
                    pos = bodyEnd + "</rainbow>".length();
                    float saturation = 1.0f;
                    if (!tag.args.isEmpty()) {
                        try {
                            saturation = Float.parseFloat(tag.args.get(0));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    BfComponent rainbow = parseRainbow(body, saturation, context.currentStyle());
                    context.appendComponent(rainbow);
                    continue;
                }

                // Unknown tag: skip it.
            }
        }

        private Tag parseTag() {
            int start = pos;
            if (input.charAt(pos) != '<') {
                return null;
            }
            int close = input.indexOf('>', pos);
            if (close == -1) {
                return null;
            }
            String raw = input.substring(pos + 1, close);
            pos = close + 1;

            boolean closing = raw.startsWith("/");
            if (closing) {
                raw = raw.substring(1);
            }

            String name;
            List<String> args = new ArrayList<String>();
            int colon = raw.indexOf(':');
            if (colon == -1) {
                name = raw.toLowerCase(Locale.ROOT).trim();
            } else {
                name = raw.substring(0, colon).toLowerCase(Locale.ROOT).trim();
                String remainder = raw.substring(colon + 1);
                for (String part : remainder.split(":")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        args.add(trimmed);
                    }
                }
            }

            if (name.isEmpty()) {
                pos = start;
                return null;
            }

            return new Tag(name, args, closing, start, close + 1);
        }

        private int findClosingTag(String name, int searchPos) {
            String open = "<" + name;
            String close = "</" + name + ">";
            int depth = 1;
            int i = searchPos;
            while (i < length) {
                int nextOpen = input.indexOf(open, i);
                int nextClose = input.indexOf(close, i);
                if (nextClose == -1) {
                    return -1;
                }
                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    i = nextOpen + open.length();
                } else {
                    depth--;
                    if (depth == 0) {
                        return nextClose;
                    }
                    i = nextClose + close.length();
                }
            }
            return -1;
        }

        private static String resolveDecoration(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.equals("bold") || lower.equals("italic") || lower.equals("underlined")
                    || lower.equals("strikethrough") || lower.equals("obfuscated")) {
                return lower;
            }
            String alias = DECORATION_ALIASES.get(lower);
            return alias;
        }

        private static BfColor resolveColorTag(Tag tag) {
            String name = tag.name;
            if (name.equals("color") || name.equals("c")) {
                if (tag.args.isEmpty()) {
                    return null;
                }
                String value = tag.args.get(0);
                return parseColorValue(value);
            }
            if (name.startsWith("#")) {
                return BfColor.hex(name);
            }
            return BfColor.named(name);
        }

        private static BfColor parseColorValue(String value) {
            if (value.startsWith("#")) {
                return BfColor.hex(value);
            }
            BfColor named = BfColor.named(value);
            if (named != null) {
                return named;
            }
            return BfColor.hex(value);
        }

        private static void applyDecoration(BfStyle style, String decoration) {
            if (decoration.equals("bold")) {
                style.bold(true);
            } else if (decoration.equals("italic")) {
                style.italic(true);
            } else if (decoration.equals("underlined")) {
                style.underlined(true);
            } else if (decoration.equals("strikethrough")) {
                style.strikethrough(true);
            } else if (decoration.equals("obfuscated")) {
                style.obfuscated(true);
            }
        }

        private BfComponent parseGradient(String body, List<String> args, BfStyle parentStyle) {
            List<BfColor> colors = new ArrayList<BfColor>();
            double phase = 0;
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                BfColor color = parseColorValue(arg);
                if (color != null) {
                    colors.add(color);
                } else if (i == args.size() - 1) {
                    try {
                        phase = Double.parseDouble(arg);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (colors.size() < 2) {
                colors.clear();
                colors.add(BfColor.named("white"));
                colors.add(BfColor.named("black"));
            }

            BfComponent parsed = MiniMessageParser.parse(body);
            String plain = toPlain(parsed);
            if (plain.isEmpty()) {
                return parsed;
            }

            BfStyle baseStyle = parentStyle == null ? new BfStyle() : parentStyle.copy();
            BfComponent result = BfComponent.empty();
            int[] index = new int[]{0};
            applyGradientColor(parsed, colors, phase, baseStyle, result, index, plain.length());
            return result;
        }

        private BfComponent parseRainbow(String body, float saturation, BfStyle parentStyle) {
            BfComponent parsed = MiniMessageParser.parse(body);
            String plain = toPlain(parsed);
            if (plain.isEmpty()) {
                return parsed;
            }

            BfStyle baseStyle = parentStyle == null ? new BfStyle() : parentStyle.copy();
            BfComponent result = BfComponent.empty();
            int[] index = new int[]{0};
            applyRainbowColor(parsed, saturation, baseStyle, result, index, plain.length());
            return result;
        }

        private void applyGradientColor(BfComponent source, List<BfColor> colors, double phase,
                BfStyle parentStyle, BfComponent target, int[] index, int totalLength) {
            BfStyle mergedStyle = parentStyle.merge(source.style());
            String text = source.content();
            int colorCount = colors.size();
            for (int i = 0; i < text.length(); i++) {
                BfColor mixed = gradientColorAt(index[0], totalLength, colors, colorCount, phase);
                BfComponent child = BfComponent.text(String.valueOf(text.charAt(i)));
                BfStyle childStyle = mergedStyle.copy();
                childStyle.color(mixed);
                child.style().copyFrom(childStyle);
                target.append(child);
                index[0]++;
            }
            for (BfComponent child : source.children()) {
                applyGradientColor(child, colors, phase, mergedStyle, target, index, totalLength);
            }
        }

        private BfColor gradientColorAt(int index, int totalLength, List<BfColor> colors, int colorCount, double phase) {
            if (totalLength <= 1) {
                return lerp(colors.get(0), colors.get(colorCount - 1), 0.5f);
            }
            double position = ((double) index / (totalLength - 1)) * (colorCount - 1);
            position += phase * (colorCount - 1);
            position = position % colorCount;
            if (position < 0) {
                position += colorCount;
            }
            int low = (int) Math.floor(position);
            int high = (int) Math.ceil(position);
            if (high >= colorCount) {
                high = colorCount - 1;
            }
            float ratio = (float) (position - low);
            return lerp(colors.get(low), colors.get(high), ratio);
        }

        private void applyRainbowColor(BfComponent source, float saturation,
                BfStyle parentStyle, BfComponent target, int[] index, int totalLength) {
            BfStyle mergedStyle = parentStyle.merge(source.style());
            String text = source.content();
            for (int i = 0; i < text.length(); i++) {
                float hue = (float) index[0] / Math.max(1, totalLength - 1);
                Color color = Color.getHSBColor(hue, saturation, saturation);
                BfColor bfColor = BfColor.hex((color.getRGB() & 0x00FFFFFF));
                BfComponent child = BfComponent.text(String.valueOf(text.charAt(i)));
                BfStyle childStyle = mergedStyle.copy();
                childStyle.color(bfColor);
                child.style().copyFrom(childStyle);
                target.append(child);
                index[0]++;
            }
            for (BfComponent child : source.children()) {
                applyRainbowColor(child, saturation, mergedStyle, target, index, totalLength);
            }
        }

        private static BfColor lerp(BfColor a, BfColor b, float ratio) {
            int red = (int) (a.red() + (b.red() - a.red()) * ratio);
            int green = (int) (a.green() + (b.green() - a.green()) * ratio);
            int blue = (int) (a.blue() + (b.blue() - a.blue()) * ratio);
            return BfColor.hex((red << 16) | (green << 8) | blue);
        }

        private static String toPlain(BfComponent component) {
            StringBuilder builder = new StringBuilder();
            appendPlain(component, builder);
            return builder.toString();
        }

        private static void appendPlain(BfComponent component, StringBuilder builder) {
            builder.append(component.content());
            for (BfComponent child : component.children()) {
                appendPlain(child, builder);
            }
        }
    }

    private static final class ParseContext {

        private final BfComponent root = BfComponent.empty();
        private final Deque<BfStyle> styleStack = new ArrayDeque<BfStyle>();
        private final Deque<String> styleNames = new ArrayDeque<String>();
        private final StringBuilder textBuffer = new StringBuilder();

        ParseContext() {
            styleStack.push(new BfStyle());
        }

        BfStyle currentStyle() {
            return styleStack.peek();
        }

        void appendText(String text) {
            if (text.isEmpty()) {
                return;
            }
            BfComponent component = BfComponent.text(text);
            component.style().copyFrom(currentStyle());
            root.append(component);
        }

        void appendComponent(BfComponent component) {
            root.append(component);
        }

        void pushStyle(String name) {
            styleStack.push(currentStyle().copy());
            styleNames.push(name);
        }

        void resetStyle() {
            styleStack.clear();
            styleNames.clear();
            styleStack.push(new BfStyle());
            styleNames.push("");
        }

        void popStyleIfMatches(String name) {
            String decoration = Parser.resolveDecoration(name);
            String expected = decoration != null ? decoration : name;
            while (!styleNames.isEmpty()) {
                String topName = styleNames.pop();
                styleStack.pop();
                if (topName.equals(expected)) {
                    return;
                }
            }
            // Tag was closed without being opened; restore base style.
            styleStack.push(new BfStyle());
            styleNames.push("");
        }
    }

    private static final class Tag {
        final String name;
        final List<String> args;
        final boolean closing;
        final int start;
        final int end;

        Tag(String name, List<String> args, boolean closing, int start, int end) {
            this.name = name;
            this.args = args;
            this.closing = closing;
            this.start = start;
            this.end = end;
        }
    }
}
