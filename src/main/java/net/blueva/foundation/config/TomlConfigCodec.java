package net.blueva.foundation.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final class TomlConfigCodec implements ConfigCodec {

    @Override
    public ConfigDocument read(String text) {
        text = collapseMultilineStrings(text);
        ConfigDocument document = new ConfigDocument();
        List<String> comments = new ArrayList<>();
        String section = "";
        Map<String, Object> arrayTable = null;
        Map<String, Map<String, Object>> currentArrayTables = new HashMap<>();
        Set<String> assignedPaths = new HashSet<>();
        Set<String> tablePaths = new HashSet<>();
        int lineNumber = 0;
        for (String line : ConfigValues.logicalLines(text, ConfigFormat.TOML)) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (ConfigValues.hasUnclosedQuote(line)) {
                throw error(lineNumber, 1, "Unclosed quoted string");
            }
            if (trimmed.startsWith("#")) {
                comments.add(trimmed.substring(1).trim());
                continue;
            }
            int sectionComment = ConfigValues.inlineCommentIndex(trimmed);
            String withoutComment = sectionComment >= 0 ? trimmed.substring(0, sectionComment).trim() : trimmed;
            String sectionInline = sectionComment >= 0 ? trimmed.substring(sectionComment + 1).trim() : null;
            if (withoutComment.startsWith("[") && withoutComment.endsWith("]")) {
                boolean arrayTableSection = withoutComment.startsWith("[[") && withoutComment.endsWith("]]");
                section = withoutComment.substring(arrayTableSection ? 2 : 1, withoutComment.length() - (arrayTableSection ? 2 : 1)).trim();
                if (section.isEmpty()) {
                    throw error(lineNumber, 1, "Section name cannot be empty");
                }
                if (arrayTableSection) {
                    String parent = parentPath(section);
                    String childName = leafName(section);
                    List<Object> tables;
                    if (!parent.isEmpty() && currentArrayTables.containsKey(parent)) {
                        Map<String, Object> parentTable = currentArrayTables.get(parent);
                        Object existing = parentTable.get(childName);
                        tables = existing instanceof List ? castList(existing) : new ArrayList<Object>();
                        parentTable.put(childName, tables);
                    } else {
                        ConfigNode node = document.nodeOrCreate(section);
                        tables = node.getValue() instanceof List ? castList(node.getValue()) : new ArrayList<Object>();
                    }
                    LinkedHashMap<String, Object> table = new LinkedHashMap<>();
                    tables.add(table);
                    if (!parent.isEmpty() && currentArrayTables.containsKey(parent)) {
                        arrayTable = table;
                    } else {
                        ConfigNode node = document.nodeOrCreate(section);
                        node.setValue(tables);
                        List<Object> stored = castList(node.getValue());
                        thisComments(node, comments, sectionInline);
                        arrayTable = castMap(stored.get(stored.size() - 1));
                    }
                    currentArrayTables.put(section, arrayTable);
                    comments.clear();
                    continue;
                }
                if (tablePaths.contains(section)) {
                    throw error(lineNumber, 1, "Duplicate table '" + section + "'");
                }
                tablePaths.add(section);
                arrayTable = currentArrayTables.get(section);
                ConfigNode node = document.nodeOrCreate(section);
                if (!comments.isEmpty()) {
                    node.comments().clear();
                    node.comments().addAll(comments);
                    comments.clear();
                }
                node.setInlineComment(sectionInline);
                continue;
            }
            int equals = ConfigValues.separatorIndex(trimmed, '=');
            if (equals < 0) {
                throw error(lineNumber, 1, "Expected key/value separator '='");
            }
            String key = trimmed.substring(0, equals).trim();
            if (key.isEmpty()) {
                throw error(lineNumber, 1, "Key cannot be empty");
            }
            String raw = trimmed.substring(equals + 1).trim();
            String inline = null;
            int comment = ConfigValues.inlineCommentIndex(raw);
            if (comment >= 0) {
                inline = raw.substring(comment + 1).trim();
                raw = raw.substring(0, comment).trim();
            }
            if (arrayTable != null) {
                putMapPath(arrayTable, key, ConfigValues.parse(raw, ConfigFormat.TOML), lineNumber);
                comments.clear();
                continue;
            }
            String path = section.isEmpty() ? key : section + "." + key;
            if (assignedPaths.contains(path)) {
                throw error(lineNumber, 1, "Duplicate key '" + key + "'");
            }
            assignedPaths.add(path);
            ConfigNode node = document.nodeOrCreate(path);
            if (!comments.isEmpty()) {
                node.comments().clear();
                node.comments().addAll(comments);
                comments.clear();
            }
            node.setInlineComment(inline);
            node.setValue(ConfigValues.parse(raw, ConfigFormat.TOML));
        }
        return document;
    }

    @Override
    public String write(ConfigDocument document) {
        StringBuilder builder = new StringBuilder();
        writeRootValues(builder, document.root());
        writeSections(builder, "", document.root());
        return builder.toString();
    }

    @Override
    public String extension() {
        return "toml";
    }

    private void writeRootValues(StringBuilder builder, ConfigNode root) {
        for (Map.Entry<String, ConfigNode> entry : root.children().entrySet()) {
            ConfigNode node = entry.getValue();
            if (isListOfMaps(node.getValue())) {
                writeArrayTables(builder, entry.getKey(), castList(node.getValue()));
                continue;
            }
            if (node.getValue() != null || node.children().isEmpty()) {
                writeKey(builder, entry.getKey(), node);
            }
        }
    }

    private void writeSections(StringBuilder builder, String path, ConfigNode node) {
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            ConfigNode child = entry.getValue();
            String section = ConfigPath.joinLiteral(path, entry.getKey());
            if (isListOfMaps(child.getValue())) {
                writeArrayTables(builder, section, castList(child.getValue()));
                continue;
            }
            if (child.getValue() != null || child.children().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                if (builder.charAt(builder.length() - 1) != '\n') {
                    builder.append('\n');
                }
                builder.append('\n');
            }
            for (String comment : child.comments()) {
                builder.append("# ").append(comment).append('\n');
            }
            builder.append('[').append(ConfigPath.tomlPath(section)).append(']');
            if (child.getInlineComment() != null && !child.getInlineComment().isEmpty()) {
                builder.append(" # ").append(child.getInlineComment());
            }
            builder.append('\n');
            for (Map.Entry<String, ConfigNode> valueEntry : child.children().entrySet()) {
                ConfigNode value = valueEntry.getValue();
                if (value.getValue() != null || value.children().isEmpty()) {
                    writeKey(builder, valueEntry.getKey(), value);
                }
            }
            writeSections(builder, section, child);
        }
    }

    private void writeKey(StringBuilder builder, String key, ConfigNode node) {
        for (String comment : node.comments()) {
            builder.append("# ").append(comment).append('\n');
        }
        builder.append(ConfigPath.tomlKey(key)).append(" = ").append(ConfigValues.toml(node.getValue()));
        if (node.getInlineComment() != null && !node.getInlineComment().isEmpty()) {
            builder.append(" # ").append(node.getInlineComment());
        }
        builder.append('\n');
    }

    private void writeArrayTables(StringBuilder builder, String section, List<Object> tables) {
        for (Object table : tables) {
            if (!(table instanceof Map)) {
                continue;
            }
            if (builder.length() > 0) {
                if (builder.charAt(builder.length() - 1) != '\n') {
                    builder.append('\n');
                }
                builder.append('\n');
            }
            builder.append("[[").append(ConfigPath.tomlPath(section)).append("]]\n");
            for (Entry<String, Object> entry : castMap(table).entrySet()) {
                if (isListOfMaps(entry.getValue())) {
                    continue;
                }
                builder.append(ConfigPath.tomlKey(entry.getKey())).append(" = ").append(ConfigValues.toml(entry.getValue())).append('\n');
            }
            for (Entry<String, Object> entry : castMap(table).entrySet()) {
                if (isListOfMaps(entry.getValue())) {
                    writeArrayTables(builder, ConfigPath.joinLiteral(section, entry.getKey()), castList(entry.getValue()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object value) {
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static boolean isListOfMaps(Object value) {
        if (!(value instanceof List)) {
            return false;
        }
        List<?> list = (List<?>) value;
        return !list.isEmpty() && list.get(0) instanceof Map;
    }

    private static String parseTomlKey(String key) {
        String[] parts = ConfigPath.parts(key);
        return parts.length == 1 ? parts[0] : key;
    }

    @SuppressWarnings("unchecked")
    private static void putMapPath(Map<String, Object> map, String path, Object value, int lineNumber) {
        String[] parts = ConfigPath.parts(path);
        if (parts.length == 0) {
            return;
        }
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = current.get(parts[i]);
            if (!(child instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                current.put(parts[i], child);
            }
            current = (Map<String, Object>) child;
        }
        String key = parts[parts.length - 1];
        if (current.containsKey(key)) {
            throw error(lineNumber, 1, "Duplicate key '" + key + "'");
        }
        current.put(key, value);
    }

    private static String parentPath(String path) {
        String[] parts = ConfigPath.parts(path);
        if (parts.length <= 1) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append(ConfigPath.literal(parts[i]));
        }
        return builder.toString();
    }

    private static String leafName(String path) {
        String[] parts = ConfigPath.parts(path);
        return parts.length == 0 ? path : parts[parts.length - 1];
    }

    private static void thisComments(ConfigNode node, List<String> comments, String inline) {
        if (!comments.isEmpty()) {
            node.comments().clear();
            node.comments().addAll(comments);
        }
        node.setInlineComment(inline);
    }

    private String collapseMultilineStrings(String text) {
        if (text == null || text.indexOf("\"\"\"") < 0 && text.indexOf("'''") < 0) {
            return text;
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int doubleStart = line.indexOf("\"\"\"");
            int singleStart = line.indexOf("'''");
            int start;
            String quote;
            if (doubleStart >= 0 && (singleStart < 0 || doubleStart < singleStart)) {
                start = doubleStart;
                quote = "\"\"\"";
            } else if (singleStart >= 0) {
                start = singleStart;
                quote = "'''";
            } else {
                output.append(line).append('\n');
                continue;
            }
            StringBuilder combined = new StringBuilder(line);
            while (combined.indexOf(quote, start + 3) < 0) {
                i++;
                if (i >= lines.length) {
                    throw error(lines.length, start + 1, "Unclosed multiline string");
                }
                combined.append("\\n").append(lines[i]);
            }
            output.append(combined).append('\n');
        }
        return output.toString();
    }

    private static ConfigParseException error(int line, int column, String message) {
        return new ConfigParseException(ConfigFormat.TOML, line, column, message);
    }
}
