package net.blueva.api.config;

import java.util.ArrayList;
import java.util.List;

final class ConfigPath {
    private ConfigPath() {
    }

    static String[] parts(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new String[0];
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        char quote = 0;
        String trimmed = path.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (quoted && c == '\\') {
                escaped = true;
                continue;
            }
            if ((c == '"' || c == '\'') && (!quoted || quote == c)) {
                quoted = !quoted;
                quote = quoted ? c : 0;
                continue;
            }
            if (c == '.' && !quoted) {
                addPart(parts, current);
                continue;
            }
            current.append(c);
        }
        addPart(parts, current);
        return parts.toArray(new String[parts.size()]);
    }

    static String join(String parent, String child) {
        return parent == null || parent.isEmpty() ? child : parent + "." + child;
    }

    static String joinLiteral(String parent, String child) {
        String key = literal(child);
        return parent == null || parent.isEmpty() ? key : parent + "." + key;
    }

    static String literal(String key) {
        if (key != null && key.indexOf('.') < 0 && key.indexOf('"') < 0 && key.indexOf('\'') < 0) {
            return key;
        }
        return "\"" + escape(key == null ? "" : key) + "\"";
    }

    static String tomlKey(String key) {
        if (key != null && key.matches("[A-Za-z0-9_-]+")) {
            return key;
        }
        return "\"" + escape(key == null ? "" : key) + "\"";
    }

    static String tomlPath(String path) {
        String[] parts = parts(path);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append(tomlKey(parts[i]));
        }
        return builder.toString();
    }

    static String yamlKey(String key) {
        if (key != null && key.matches("[A-Za-z0-9_-]+")) {
            return key;
        }
        return "\"" + escape(key == null ? "" : key) + "\"";
    }

    private static void addPart(List<String> parts, StringBuilder current) {
        String part = current.toString().trim();
        if (!part.isEmpty()) {
            parts.add(part);
        }
        current.setLength(0);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
