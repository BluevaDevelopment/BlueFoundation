package net.blueva.api.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loaded BlueAPI configuration file. */
public class ConfigFile {

    private final Path file;
    private final Path cacheFile;
    private final ConfigCodec codec;
    private final ConfigDocument defaults;
    private ConfigDocument document;

    ConfigFile(Path file, Path cacheFile, ConfigCodec codec, ConfigDocument defaults, ConfigDocument document) {
        this.file = file;
        this.cacheFile = cacheFile;
        this.codec = codec;
        this.defaults = defaults;
        this.document = document;
    }

    public Path file() {
        return file;
    }

    public Path cacheFile() {
        return cacheFile;
    }

    public ConfigDocument document() {
        return document;
    }

    public boolean contains(String path) {
        return document.contains(path);
    }

    public Object get(String path) {
        return document.get(path);
    }

    public ConfigSection rootSection() {
        return document.rootSection();
    }

    public ConfigSection section(String path) {
        return document.section(path);
    }

    public ConfigSection sectionOrCreate(String path) {
        return document.sectionOrCreate(path);
    }

    public String getString(String path) {
        Object value = get(path);
        return value == null ? "" : String.valueOf(value);
    }

    public String getString(String path, String fallback) {
        Object value = get(path);
        return value == null ? fallback : String.valueOf(value);
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int fallback) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long fallback) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public double getDouble(String path) {
        return getDouble(path, 0D);
    }

    public double getDouble(String path, double fallback) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean fallback) {
        Object value = get(path);
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

    public ConfigDateTime getDateTime(String path) {
        Object value = get(path);
        return value instanceof ConfigDateTime ? (ConfigDateTime) value : null;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            return new ArrayList<Object>((List<Object>) value);
        }
        return Collections.emptyList();
    }

    public List<String> getStringList(String path) {
        List<Object> values = getList(path);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) value);
        }
        ConfigNode node = document.node(path);
        if (node == null || node.children().isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ConfigNode> entry : node.children().entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }
        return result;
    }

    public List<String> keys(String path) {
        ConfigNode node = document.node(path);
        if (node == null || node.children().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(node.children().keySet());
    }

    public ConfigFile set(String path, Object value) {
        document.set(path, value);
        return this;
    }

    public ConfigFile setIfAbsent(String path, Object value) {
        if (!contains(path)) {
            set(path, value);
        }
        return this;
    }

    public ConfigFile addDefault(String path, Object value) {
        if (defaults.node(path) == null) {
            defaults.set(path, value);
        }
        if (!contains(path)) {
            set(path, value);
        }
        return this;
    }

    public ConfigFile comment(String path, String... comments) {
        ConfigNode node = document.nodeOrCreate(path);
        node.comments().clear();
        if (comments != null) {
            for (String comment : comments) {
                if (comment != null) {
                    node.comments().add(comment);
                }
            }
        }
        return this;
    }

    public ConfigFile inlineComment(String path, String comment) {
        document.nodeOrCreate(path).setInlineComment(comment);
        return this;
    }

    public void reload() throws IOException {
        document = codec.read(ConfigIO.read(file));
        update();
    }

    public void save() throws IOException {
        save(true);
        saveCache();
    }

    public void update() throws IOException {
        String before = Files.exists(file) ? ConfigIO.read(file) : "";
        ConfigCache cache = Files.exists(cacheFile) ? ConfigCache.read(ConfigIO.read(cacheFile)) : new ConfigCache();
        for (Map.Entry<String, ConfigNode> entry : defaults.nodes().entrySet()) {
            ConfigNode defaultNode = entry.getValue();
            if (defaultNode.getValue() == null && !defaultNode.children().isEmpty() && document.node(entry.getKey()) == null) {
                document.putNode(entry.getKey(), defaultNode.copy());
            }
        }
        for (Map.Entry<String, ConfigNode> entry : defaults.leaves().entrySet()) {
            String path = entry.getKey();
            ConfigNode defaultNode = entry.getValue();
            Object newDefault = defaultNode.getValue();
            ConfigNode localNode = document.node(path);
            if (localNode == null) {
                document.putNode(path, defaultNode.copy());
                continue;
            }

            ConfigCache.Entry cached = cache.get(path);
            if (cached != null) {
                String localHash = ConfigCache.hash(localNode.getValue());
                if (localHash.equals(cached.defaultHash)) {
                    copyValueAndComments(localNode, defaultNode);
                } else if (ConfigCache.commentHash(localNode).equals(cached.defaultCommentHash)) {
                    copyComments(localNode, defaultNode);
                } else {
                    refreshMissingComments(localNode, defaultNode);
                }
            } else {
                refreshMissingComments(localNode, defaultNode);
            }
        }
        String after = codec.write(document);
        if (!before.equals(after)) {
            ConfigIO.backup(file, cacheFile);
        }
        ConfigIO.writeAtomic(file, after);
        saveCache();
    }

    private void saveCache() throws IOException {
        ConfigCache cache = new ConfigCache();
        for (Map.Entry<String, ConfigNode> entry : defaults.leaves().entrySet()) {
            String path = entry.getKey();
            ConfigNode local = document.node(path);
            if (local != null) {
                cache.put(path, entry.getValue(), local);
            }
        }
        ConfigIO.writeAtomic(cacheFile, cache.write());
    }

    private static void copyValueAndComments(ConfigNode target, ConfigNode source) {
        target.setValue(source.getValue());
        target.comments().clear();
        target.comments().addAll(source.comments());
        target.setInlineComment(source.getInlineComment());
    }

    private static void copyComments(ConfigNode target, ConfigNode source) {
        target.comments().clear();
        target.comments().addAll(source.comments());
        target.setInlineComment(source.getInlineComment());
    }

    private static void refreshMissingComments(ConfigNode target, ConfigNode source) {
        if (target.comments().isEmpty() && !source.comments().isEmpty()) {
            target.comments().addAll(source.comments());
        }
        if ((target.getInlineComment() == null || target.getInlineComment().isEmpty()) && source.getInlineComment() != null) {
            target.setInlineComment(source.getInlineComment());
        }
    }

    private void save(boolean backup) throws IOException {
        String next = codec.write(document);
        if (backup && Files.exists(file)) {
            String current = ConfigIO.read(file);
            if (!current.equals(next)) {
                ConfigIO.backup(file, cacheFile);
            }
        }
        ConfigIO.writeAtomic(file, next);
    }
}
