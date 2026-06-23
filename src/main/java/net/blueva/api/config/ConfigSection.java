package net.blueva.api.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Read/write view over a config section. */
public final class ConfigSection {

    private final ConfigDocument document;
    private final String path;

    ConfigSection(ConfigDocument document, String path) {
        if (document == null) {
            throw new IllegalArgumentException("document cannot be null");
        }
        this.document = document;
        this.path = path == null ? "" : path;
    }

    public String path() {
        return path;
    }

    public boolean exists() {
        return node() != null;
    }

    public boolean contains(String childPath) {
        return document.node(resolve(childPath)) != null;
    }

    public boolean containsKey(String childPath) {
        return contains(childPath);
    }

    public Object get(String childPath) {
        ConfigNode node = document.node(resolve(childPath));
        return node == null ? null : node.getValue();
    }

    public ConfigSection set(String childPath, Object value) {
        document.set(resolve(childPath), value);
        return this;
    }

    public ConfigSection remove(String childPath) {
        document.remove(resolve(childPath));
        return this;
    }

    public String getString(String childPath) {
        Object value = get(childPath);
        return value == null ? null : String.valueOf(value);
    }

    public String getString(String childPath, String fallback) {
        String value = getString(childPath);
        return value == null ? fallback : value;
    }

    public int getInt(String childPath) {
        return getInt(childPath, 0);
    }

    public int getInt(String childPath, int fallback) {
        Object value = get(childPath);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public long getLong(String childPath) {
        return getLong(childPath, 0L);
    }

    public long getLong(String childPath, long fallback) {
        Object value = get(childPath);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public double getDouble(String childPath) {
        return getDouble(childPath, 0.0D);
    }

    public double getDouble(String childPath, double fallback) {
        Object value = get(childPath);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public boolean getBoolean(String childPath) {
        return getBoolean(childPath, false);
    }

    public boolean getBoolean(String childPath, boolean fallback) {
        Object value = get(childPath);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if ("true".equalsIgnoreCase(String.valueOf(value))) {
            return true;
        }
        if ("false".equalsIgnoreCase(String.valueOf(value))) {
            return false;
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String childPath) {
        Object value = get(childPath);
        if (value instanceof List) {
            return new ArrayList<Object>((List<Object>) value);
        }
        return Collections.emptyList();
    }

    public List<String> getStringList(String childPath) {
        List<Object> values = getList(childPath);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        return result;
    }

    public List<String> getStringList(String childPath, List<String> fallback) {
        List<String> values = getStringList(childPath);
        return values.isEmpty() ? fallback : values;
    }

    public List<Integer> getIntList(String childPath) {
        List<Object> values = getList(childPath);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<Integer>();
        for (Object value : values) {
            if (value instanceof Number) {
                result.add(((Number) value).intValue());
                continue;
            }
            try {
                result.add(Integer.parseInt(String.valueOf(value)));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public ConfigSection section(String childPath) {
        String resolved = resolve(childPath);
        return document.node(resolved) == null ? null : new ConfigSection(document, resolved);
    }

    public ConfigSection getSection(String childPath) {
        return section(childPath);
    }

    public ConfigSection sectionOrCreate(String childPath) {
        String resolved = resolve(childPath);
        document.nodeOrCreate(resolved);
        return new ConfigSection(document, resolved);
    }

    public List<String> keys() {
        ConfigNode node = node();
        if (node == null || node.children().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(node.children().keySet());
    }

    public List<String> getKeys() {
        return keys();
    }

    public List<ConfigSection> sections() {
        ConfigNode node = node();
        if (node == null || node.children().isEmpty()) {
            return Collections.emptyList();
        }
        List<ConfigSection> result = new ArrayList<ConfigSection>();
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            if (entry.getValue().hasChildren()) {
                result.add(new ConfigSection(document, ConfigPath.joinLiteral(path, entry.getKey())));
            }
        }
        return result;
    }

    private ConfigNode node() {
        return path.isEmpty() ? document.root() : document.node(path);
    }

    private String resolve(String childPath) {
        if (childPath == null || childPath.trim().isEmpty()) {
            return path;
        }
        if (path.isEmpty()) {
            return childPath;
        }
        return ConfigPath.joinLiteral(path, childPath);
    }
}
