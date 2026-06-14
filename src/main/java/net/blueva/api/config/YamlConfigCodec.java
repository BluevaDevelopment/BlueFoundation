package net.blueva.api.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

final class YamlConfigCodec implements ConfigCodec {

    @Override
    public ConfigDocument read(String text) {
        text = collapseBlockScalars(text);
        ConfigDocument document = new ConfigDocument();
        List<String> comments = new ArrayList<>();
        List<Frame> stack = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();
        Map<String, Object> anchors = new LinkedHashMap<>();
        stack.add(new Frame(-1, "", null, null));

        int lineNumber = 0;
        for (String line : ConfigValues.logicalLines(text, ConfigFormat.YAML)) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.indexOf('\t') >= 0) {
                throw error(lineNumber, line.indexOf('\t') + 1, "Tabs are not supported for indentation");
            }
            if (ConfigValues.hasUnclosedQuote(line)) {
                throw error(lineNumber, 1, "Unclosed quoted string");
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                comments.add(trimmed.substring(1).trim());
                continue;
            }

            int indent = indent(line);
            while (stack.size() > 1 && indent <= stack.get(stack.size() - 1).indent) {
                stack.remove(stack.size() - 1);
            }

            if (trimmed.startsWith("- ")) {
                Frame frame = stack.get(stack.size() - 1);
                if (frame.path.isEmpty()) {
                    throw error(lineNumber, indent + 1, "List item has no parent key");
                }
                ConfigNode listNode = document.nodeOrCreate(frame.path);
                List<Object> list = listNode.getValue() instanceof List
                        ? castList(listNode.getValue())
                        : new ArrayList<Object>();
                String raw = trimmed.substring(2).trim();
                int comment = ConfigValues.inlineCommentIndex(raw);
                if (comment >= 0) {
                    raw = raw.substring(0, comment).trim();
                }
                Map<String, Object> map = parseListMap(raw, anchors);
                if (map != null) {
                    list.add(map);
                    listNode.setValue(list);
                    List<Object> stored = castList(listNode.getValue());
                    Map<String, Object> storedMap = castMap(stored.get(stored.size() - 1));
                    stack.add(new Frame(indent, frame.path, activeMap(raw, storedMap), null));
                } else {
                    list.add(parseYamlValue(raw, anchors));
                    listNode.setValue(list);
                }
                comments.clear();
                continue;
            }

            int colon = ConfigValues.separatorIndex(line, ':');
            if (colon < 0) {
                throw error(lineNumber, 1, "Expected key/value separator ':'");
            }

            String key = parseKey(line.substring(indent, colon).trim());
            if (key.isEmpty()) {
                throw error(lineNumber, indent + 1, "Key cannot be empty");
            }
            String after = line.substring(colon + 1).trim();
            String inline = null;
            int comment = ConfigValues.inlineCommentIndex(after);
            if (comment >= 0) {
                inline = after.substring(comment + 1).trim();
                after = after.substring(0, comment).trim();
            }

            Frame parentFrame = stack.get(stack.size() - 1);
            if (parentFrame.map != null) {
                if ("<<".equals(key)) {
                    Object merge = parseYamlValue(after, anchors);
                    if (merge instanceof Map) {
                        parentFrame.map.putAll(castMap(merge));
                    }
                    comments.clear();
                    continue;
                }
                if (parentFrame.map.containsKey(key)) {
                    throw error(lineNumber, indent + 1, "Duplicate key '" + key + "'");
                }
                if (after.isEmpty()) {
                    LinkedHashMap<String, Object> child = new LinkedHashMap<>();
                    parentFrame.map.put(key, child);
                    stack.add(new Frame(indent, parentFrame.path, child, null));
                } else {
                    parentFrame.map.put(key, parseYamlValue(after, anchors));
                }
                comments.clear();
                continue;
            }
            if ("<<".equals(key) && !parentFrame.path.isEmpty()) {
                Object merge = parseYamlValue(after, anchors);
                if (merge instanceof Map) {
                    mergeIntoNode(document.nodeOrCreate(parentFrame.path), castMap(merge));
                    refreshAnchors(stack, document, anchors);
                }
                comments.clear();
                continue;
            }
            String parent = parentFrame.path;
            String path = ConfigPath.joinLiteral(parent, key);
            if (seenPaths.contains(path)) {
                throw error(lineNumber, indent + 1, "Duplicate key '" + key + "'");
            }
            seenPaths.add(path);
            ConfigNode node = document.nodeOrCreate(path);
            if (!comments.isEmpty()) {
                node.comments().clear();
                node.comments().addAll(comments);
                comments.clear();
            }
            node.setInlineComment(inline);
            if (after.isEmpty() || isAnchorOnly(after)) {
                String anchor = isAnchorOnly(after) ? anchorName(after) : null;
                if (anchor != null) {
                    anchors.put(anchor, nodeToMap(node));
                }
                stack.add(new Frame(indent, path, null, anchor));
            } else {
                node.setValue(parseYamlValue(after, anchors));
            }
            refreshAnchors(stack, document, anchors);
        }
        return document;
    }

    @Override
    public String write(ConfigDocument document) {
        StringBuilder builder = new StringBuilder();
        writeChildren(builder, document.root(), 0);
        return builder.toString();
    }

    @Override
    public String extension() {
        return "yml";
    }

    private void writeChildren(StringBuilder builder, ConfigNode node, int indent) {
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            writeNode(builder, entry.getKey(), entry.getValue(), indent);
        }
    }

    private void writeNode(StringBuilder builder, String key, ConfigNode node, int indent) {
        for (String comment : node.comments()) {
            spaces(builder, indent).append("# ").append(comment).append('\n');
        }
        spaces(builder, indent).append(ConfigPath.yamlKey(key)).append(':');
        if (node.getValue() == null && !node.children().isEmpty()) {
            appendInline(builder, node);
            builder.append('\n');
            writeChildren(builder, node, indent + 2);
        } else if (node.getValue() instanceof List) {
            appendInline(builder, node);
            builder.append('\n');
            for (Object value : castList(node.getValue())) {
                if (value instanceof Map) {
                    writeListMap(builder, castMap(value), indent + 2);
                } else {
                    spaces(builder, indent + 2).append("- ").append(ConfigValues.yaml(value)).append('\n');
                }
            }
        } else {
            builder.append(' ').append(ConfigValues.yaml(node.getValue()));
            appendInline(builder, node);
            builder.append('\n');
        }
    }

    private void appendInline(StringBuilder builder, ConfigNode node) {
        if (node.getInlineComment() != null && !node.getInlineComment().isEmpty()) {
            builder.append(" # ").append(node.getInlineComment());
        }
    }

    private StringBuilder spaces(StringBuilder builder, int count) {
        for (int i = 0; i < count; i++) {
            builder.append(' ');
        }
        return builder;
    }

    private int indent(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object value) {
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private void writeListMap(StringBuilder builder, Map<String, Object> map, int indent) {
        boolean first = true;
        for (Entry<String, Object> entry : map.entrySet()) {
            if (first) {
                spaces(builder, indent).append("- ").append(ConfigPath.yamlKey(entry.getKey()));
                first = false;
            } else {
                spaces(builder, indent + 2).append(ConfigPath.yamlKey(entry.getKey()));
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                builder.append(":\n");
                writeInlineMapChildren(builder, castMap(value), indent + 4);
            } else if (value instanceof List) {
                builder.append(":\n");
                for (Object item : castList(value)) {
                    spaces(builder, indent + 4).append("- ").append(ConfigValues.yaml(item)).append('\n');
                }
            } else {
                builder.append(": ").append(ConfigValues.yaml(value)).append('\n');
            }
        }
        if (first) {
            spaces(builder, indent).append("- {}\n");
        }
    }

    private void writeInlineMapChildren(StringBuilder builder, Map<String, Object> map, int indent) {
        for (Entry<String, Object> entry : map.entrySet()) {
            spaces(builder, indent).append(ConfigPath.yamlKey(entry.getKey())).append(": ").append(ConfigValues.yaml(entry.getValue())).append('\n');
        }
    }

    private static Map<String, Object> parseListMap(String raw, Map<String, Object> anchors) {
        int colon = ConfigValues.separatorIndex(raw, ':');
        if (colon < 0 || raw.startsWith("{") || raw.startsWith("[")) {
            return null;
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        String key = raw.substring(0, colon).trim();
        if (ConfigValues.isQuoted(key)) {
            key = ConfigValues.unquote(key);
        }
        String value = raw.substring(colon + 1).trim();
        if ("<<".equals(key)) {
            Object merge = parseYamlValue(value, anchors);
            if (merge instanceof Map) {
                return new LinkedHashMap<String, Object>(castMap(merge));
            }
        }
        map.put(key, value.isEmpty() ? new LinkedHashMap<String, Object>() : parseYamlValue(value, anchors));
        return map;
    }

    private static Object parseYamlValue(String raw, Map<String, Object> anchors) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("*")) {
            String name = value.substring(1).trim();
            if (!anchors.containsKey(name)) {
                throw new ConfigParseException(ConfigFormat.YAML, 1, 1, "Unknown alias '*" + name + "'");
            }
            return copyObject(anchors.get(name));
        }
        if (value.startsWith("&")) {
            int space = value.indexOf(' ');
            String name = space == -1 ? value.substring(1) : value.substring(1, space);
            Object parsed = space == -1 ? new LinkedHashMap<String, Object>() : ConfigValues.parse(value.substring(space + 1), ConfigFormat.YAML);
            anchors.put(name, copyObject(parsed));
            return parsed;
        }
        return ConfigValues.parse(value, ConfigFormat.YAML);
    }

    @SuppressWarnings("unchecked")
    private static Object copyObject(Object value) {
        if (value instanceof Map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
            for (Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                copy.put(entry.getKey(), copyObject(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                copy.add(copyObject(item));
            }
            return copy;
        }
        return value;
    }

    private static boolean isAnchorOnly(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("&") && trimmed.indexOf(' ') < 0;
    }

    private static String anchorName(String value) {
        return value == null ? null : value.trim().substring(1);
    }

    private static void refreshAnchors(List<Frame> stack, ConfigDocument document, Map<String, Object> anchors) {
        for (Frame frame : stack) {
            if (frame.anchor != null && !frame.path.isEmpty()) {
                anchors.put(frame.anchor, nodeToMap(document.node(frame.path)));
            }
        }
    }

    private static Map<String, Object> nodeToMap(ConfigNode node) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (node == null) {
            return map;
        }
        for (Entry<String, ConfigNode> entry : node.children().entrySet()) {
            ConfigNode child = entry.getValue();
            if (child.hasChildren() && child.getValue() == null) {
                map.put(entry.getKey(), nodeToMap(child));
            } else {
                map.put(entry.getKey(), copyObject(child.getValue()));
            }
        }
        return map;
    }

    private static void mergeIntoNode(ConfigNode node, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            if (node.child(entry.getKey()) != null) {
                continue;
            }
            ConfigNode child = new ConfigNode();
            Object value = entry.getValue();
            if (value instanceof Map) {
                mergeIntoNode(child, castMap(value));
            } else {
                child.setValue(copyObject(value));
            }
            node.putChild(entry.getKey(), child);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> activeMap(String raw, Map<String, Object> map) {
        int colon = ConfigValues.separatorIndex(raw, ':');
        String value = colon < 0 ? "" : raw.substring(colon + 1).trim();
        if (value.isEmpty() && map.size() == 1) {
            Object child = map.values().iterator().next();
            if (child instanceof Map) {
                return (Map<String, Object>) child;
            }
        }
        return map;
    }

    private String collapseBlockScalars(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (!(trimmed.endsWith(": |") || trimmed.endsWith(": >"))) {
                output.append(line).append('\n');
                continue;
            }
            boolean folded = trimmed.endsWith(": >");
            int baseIndent = indent(line);
            String prefix = line.substring(0, line.lastIndexOf(':') + 1);
            StringBuilder value = new StringBuilder();
            i++;
            while (i < lines.length) {
                String next = lines[i];
                if (!next.trim().isEmpty() && indent(next) <= baseIndent) {
                    i--;
                    break;
                }
                String content = next.length() > baseIndent + 2 ? next.substring(Math.min(next.length(), baseIndent + 2)) : "";
                if (folded) {
                    if (value.length() > 0) {
                        value.append(' ');
                    }
                    value.append(content.trim());
                } else {
                    value.append(content).append('\n');
                }
                i++;
            }
            output.append(prefix).append(" \"").append(ConfigValues.escape(value.toString())).append("\"\n");
        }
        return output.toString();
    }

    private static String parseKey(String key) {
        return ConfigValues.isQuoted(key) ? ConfigValues.unquote(key) : key;
    }

    private static ConfigParseException error(int line, int column, String message) {
        return new ConfigParseException(ConfigFormat.YAML, line, column, message);
    }

    private static final class Frame {
        private final int indent;
        private final String path;
        private final Map<String, Object> map;
        private final String anchor;

        private Frame(int indent, String path, Map<String, Object> map, String anchor) {
            this.indent = indent;
            this.path = path;
            this.map = map;
            this.anchor = anchor;
        }
    }
}
