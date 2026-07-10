package net.blueva.foundation.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for multiple configuration files backed by BlueFoundation Configs.
 * Useful for modules or addons that store several YAML/TOML files under one data folder.
 */
public final class ConfigRegistry {

    private final Path dataFolder;
    private final ClassLoader classLoader;
    private final String resourcePrefix;
    private final ConfigFormat format;
    private final Map<String, Entry> entries = new ConcurrentHashMap<String, Entry>();

    ConfigRegistry(Path dataFolder, ClassLoader classLoader, String resourcePrefix, ConfigFormat format) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        this.dataFolder = dataFolder;
        this.classLoader = classLoader == null ? ConfigRegistry.class.getClassLoader() : classLoader;
        this.resourcePrefix = normalizePrefix(resourcePrefix);
        this.format = format == null ? ConfigFormat.YAML : format;
    }

    public Path dataFolder() {
        return dataFolder;
    }

    public boolean register(String fileName) {
        return register(fileName, ConfigUpdatePolicy.MERGE_DEFAULTS);
    }

    public boolean register(String fileName, ConfigUpdatePolicy updatePolicy) {
        if (updatePolicy == null) {
            updatePolicy = ConfigUpdatePolicy.MERGE_DEFAULTS;
        }
        if (updatePolicy == ConfigUpdatePolicy.COPY_DEFAULTS_ONLY) {
            try {
                return registerCopyOnly(fileName);
            } catch (IOException exception) {
                return false;
            }
        }
        if (entries.containsKey(fileName)) {
            return true;
        }
        if (resourceStream(fileName) == null) {
            return false;
        }
        ConfigFile config = Configs.load(dataFolder, mappedClassLoader(fileName), fileName, format, updatePolicy);
        entries.put(fileName, new UpdatingEntry(config));
        return true;
    }

    public boolean registerCopyOnly(String fileName) throws IOException {
        if (entries.containsKey(fileName)) {
            return true;
        }
        Path file = dataFolder.resolve(fileName);
        if (!Files.exists(file)) {
            try (InputStream input = resourceStream(fileName)) {
                if (input == null) {
                    return false;
                }
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
                Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        entries.put(fileName, new CopyOnlyEntry(file, format));
        return true;
    }

    public boolean isRegistered(String fileName) {
        return entries.containsKey(fileName);
    }

    public List<String> registeredFiles() {
        return new ArrayList<String>(entries.keySet());
    }

    public void reload(String fileName) throws IOException {
        Entry entry = entries.get(fileName);
        if (entry != null) {
            entry.reload();
        }
    }

    public void reloadAll() throws IOException {
        for (String fileName : entries.keySet()) {
            reload(fileName);
        }
    }

    public void save(String fileName) throws IOException {
        Entry entry = entries.get(fileName);
        if (entry != null) {
            entry.save();
        }
    }

    public void saveAll() throws IOException {
        for (String fileName : entries.keySet()) {
            save(fileName);
        }
    }

    public Object get(String fileName, String path) {
        Entry entry = entries.get(fileName);
        return entry == null ? null : entry.get(path);
    }

    public boolean contains(String fileName, String path) {
        Entry entry = entries.get(fileName);
        return entry != null && entry.contains(path);
    }

    public ConfigSection rootSection(String fileName) {
        Entry entry = entries.get(fileName);
        return entry == null ? null : entry.rootSection();
    }

    public ConfigSection section(String fileName, String path) {
        Entry entry = entries.get(fileName);
        return entry == null ? null : entry.section(path);
    }

    public ConfigSection sectionOrCreate(String fileName, String path) {
        Entry entry = entries.get(fileName);
        return entry == null ? null : entry.sectionOrCreate(path);
    }

    public ConfigRegistry set(String fileName, String path, Object value) {
        Entry entry = entries.get(fileName);
        if (entry != null) {
            entry.set(path, value);
        }
        return this;
    }

    public String getString(String fileName, String path) {
        Object value = get(fileName, path);
        return value == null ? null : ConfigValues.string(value);
    }

    public String getString(String fileName, String path, String fallback) {
        String value = getString(fileName, path);
        return value == null ? fallback : value;
    }

    public int getInt(String fileName, String path) {
        return getInt(fileName, path, 0);
    }

    public int getInt(String fileName, String path, int fallback) {
        Object value = get(fileName, path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public double getDouble(String fileName, String path) {
        return getDouble(fileName, path, 0.0D);
    }

    public double getDouble(String fileName, String path, double fallback) {
        Object value = get(fileName, path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public boolean getBoolean(String fileName, String path) {
        return getBoolean(fileName, path, false);
    }

    public boolean getBoolean(String fileName, String path, boolean fallback) {
        Object value = get(fileName, path);
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
    public List<String> getStringList(String fileName, String path) {
        Object value = get(fileName, path);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object entry : (List<Object>) value) {
            result.add(ConfigValues.string(entry));
        }
        return result;
    }

    private InputStream resourceStream(String fileName) {
        return classLoader.getResourceAsStream(resourcePrefix + fileName);
    }

    private ClassLoader mappedClassLoader(final String fileName) {
        return new ClassLoader(classLoader) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (fileName.equals(name)) {
                    InputStream stream = resourceStream(fileName);
                    if (stream != null) {
                        return stream;
                    }
                }
                return super.getResourceAsStream(name);
            }
        };
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private interface Entry {
        Object get(String path);

        boolean contains(String path);

        void set(String path, Object value);

        ConfigSection rootSection();

        ConfigSection section(String path);

        ConfigSection sectionOrCreate(String path);

        void reload() throws IOException;

        void save() throws IOException;
    }

    private static final class UpdatingEntry implements Entry {
        private final ConfigFile config;

        private UpdatingEntry(ConfigFile config) {
            this.config = config;
        }

        @Override
        public Object get(String path) {
            return config.get(path);
        }

        @Override
        public boolean contains(String path) {
            return config.contains(path);
        }

        @Override
        public void set(String path, Object value) {
            config.set(path, value);
        }

        @Override
        public ConfigSection rootSection() {
            return config.rootSection();
        }

        @Override
        public ConfigSection section(String path) {
            return config.section(path);
        }

        @Override
        public ConfigSection sectionOrCreate(String path) {
            return config.sectionOrCreate(path);
        }

        @Override
        public void reload() throws IOException {
            config.reload();
        }

        @Override
        public void save() throws IOException {
            config.save();
        }
    }

    private static final class CopyOnlyEntry implements Entry {
        private final Path file;
        private final ConfigFormat format;
        private ConfigDocument document;

        private CopyOnlyEntry(Path file, ConfigFormat format) throws IOException {
            this.file = file;
            this.format = format;
            reload();
        }

        @Override
        public Object get(String path) {
            return document.get(path);
        }

        @Override
        public boolean contains(String path) {
            return document.contains(path);
        }

        @Override
        public void set(String path, Object value) {
            document.set(path, value);
        }

        @Override
        public ConfigSection rootSection() {
            return document.rootSection();
        }

        @Override
        public ConfigSection section(String path) {
            return document.section(path);
        }

        @Override
        public ConfigSection sectionOrCreate(String path) {
            return document.sectionOrCreate(path);
        }

        @Override
        public void reload() throws IOException {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            document = Configs.parse(text, format);
        }

        @Override
        public void save() throws IOException {
            ConfigIO.writeAtomic(file, Configs.write(document, format));
        }
    }
}
