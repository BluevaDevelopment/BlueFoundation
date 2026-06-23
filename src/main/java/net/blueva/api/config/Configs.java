package net.blueva.api.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** BlueAPI configuration entry point. */
public class Configs {

    protected Configs() {
    }

    public static ConfigFile yaml(JavaPlugin plugin, String resourceName) {
        return load(plugin, resourceName, ConfigFormat.YAML);
    }

    public static ConfigFile toml(JavaPlugin plugin, String resourceName) {
        return load(plugin, resourceName, ConfigFormat.TOML);
    }

    public static ConfigFile load(JavaPlugin plugin, String resourceName, ConfigFormat format) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        return load(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), resourceName, format);
    }

    public static ConfigFile load(Path dataFolder, ClassLoader classLoader, String resourceName, ConfigFormat format) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder cannot be null");
        }
        if (classLoader == null) {
            classLoader = Configs.class.getClassLoader();
        }
        if (isBlank(resourceName)) {
            throw new IllegalArgumentException("resourceName cannot be blank");
        }
        ConfigCodec codec = ConfigCodecs.of(format);
        Path file = dataFolder.resolve(resourceName);
        Path cache = dataFolder.resolve(".blueapi").resolve("config-cache").resolve(safeCacheName(resourceName));

        try {
            String defaultText = resource(classLoader, resourceName);
            ConfigDocument defaults = codec.read(defaultText);
            boolean existedBeforeLoad = java.nio.file.Files.exists(file);
            boolean cacheExistedBeforeLoad = java.nio.file.Files.exists(cache);
            if (!existedBeforeLoad) {
                ConfigIO.writeAtomic(file, codec.write(defaults));
            }
            ConfigDocument document = codec.read(ConfigIO.read(file));
            Set<String> adoptedCustomPaths = existedBeforeLoad && !cacheExistedBeforeLoad
                    ? changedExistingPaths(document, defaults)
                    : new HashSet<String>();
            ConfigFile config = new ConfigFile(file, cache, codec, defaults, document, adoptedCustomPaths);
            config.update();
            return config;
        } catch (ConfigParseException exception) {
            throw withSource(exception, file);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config " + resourceName, exception);
        }
    }


    public static ConfigFile load(Path file, InputStream defaultInput, ConfigFormat format) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        if (defaultInput == null) {
            throw new IllegalArgumentException("defaultInput cannot be null");
        }
        try {
            String defaultText;
            try {
                defaultText = new String(readAll(defaultInput), StandardCharsets.UTF_8);
            } finally {
                defaultInput.close();
            }
            return loadFile(file, defaultText, format);
        } catch (ConfigParseException exception) {
            throw withSource(exception, file);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config " + file, exception);
        }
    }

    public static ConfigFile load(Path file, ConfigFormat format) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        try {
            String text = Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
            return loadFile(file, text, format);
        } catch (ConfigParseException exception) {
            throw withSource(exception, file);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config " + file, exception);
        }
    }

    private static ConfigFile loadFile(Path file, String defaultText, ConfigFormat format) throws IOException {
        ConfigCodec codec = ConfigCodecs.of(format);
        Path dataFolder = file.getParent() == null ? java.nio.file.Paths.get("") : file.getParent();
        Path cache = dataFolder.resolve(".blueapi").resolve("config-cache").resolve(safeCacheName(file.getFileName().toString()));
        ConfigDocument defaults = codec.read(defaultText);
        boolean existedBeforeLoad = Files.exists(file);
        boolean cacheExistedBeforeLoad = Files.exists(cache);
        if (!existedBeforeLoad) {
            ConfigIO.writeAtomic(file, codec.write(defaults));
        }
        ConfigDocument document = codec.read(ConfigIO.read(file));
        Set<String> adoptedCustomPaths = existedBeforeLoad && !cacheExistedBeforeLoad
                ? changedExistingPaths(document, defaults)
                : new HashSet<String>();
        ConfigFile config = new ConfigFile(file, cache, codec, defaults, document, adoptedCustomPaths);
        config.update();
        return config;
    }

    public static ConfigRegistry registry(Path dataFolder, ClassLoader classLoader, String resourcePrefix, ConfigFormat format) {
        return new ConfigRegistry(dataFolder, classLoader, resourcePrefix, format);
    }

    public static ConfigRegistry yamlRegistry(Path dataFolder, ClassLoader classLoader, String resourcePrefix) {
        return registry(dataFolder, classLoader, resourcePrefix, ConfigFormat.YAML);
    }

    public static ConfigRegistry tomlRegistry(Path dataFolder, ClassLoader classLoader, String resourcePrefix) {
        return registry(dataFolder, classLoader, resourcePrefix, ConfigFormat.TOML);
    }

    public static ConfigDocument parse(String text, ConfigFormat format) {
        return ConfigCodecs.of(format).read(text);
    }

    public static ConfigDocument read(Path file, ConfigFormat format) throws IOException {
        return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), format);
    }

    public static String write(ConfigDocument document, ConfigFormat format) {
        return ConfigCodecs.of(format).write(document);
    }


    private static ConfigParseException withSource(ConfigParseException exception, Path file) {
        return new ConfigParseException(
                exception.format(),
                exception.line(),
                exception.column(),
                exception.getMessage().substring(exception.getMessage().indexOf(": ") + 2),
                file == null ? null : file.toString()
        );
    }

    private static Set<String> changedExistingPaths(ConfigDocument document, ConfigDocument defaults) {
        Set<String> changed = new HashSet<String>();
        for (String path : document.leaves().keySet()) {
            ConfigNode localNode = document.node(path);
            ConfigNode defaultNode = defaults.node(path);
            if (defaultNode == null || !ConfigCache.hash(localNode.getValue()).equals(ConfigCache.hash(defaultNode.getValue()))) {
                changed.add(path);
            }
        }
        return changed;
    }

    private static String resource(ClassLoader classLoader, String resourceName) throws IOException {
        InputStream input = classLoader.getResourceAsStream(resourceName);
        if (input == null && resourceName.startsWith("/")) {
            input = classLoader.getResourceAsStream(resourceName.substring(1));
        }
        if (input == null) {
            throw new IOException("Missing bundled config resource: " + resourceName);
        }
        try {
            byte[] bytes = readAll(input);
            return new String(bytes, StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }

    private static byte[] readAll(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String safeCacheName(String resourceName) {
        return resourceName.replace('\\', '_').replace('/', '_') + ".cache";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
