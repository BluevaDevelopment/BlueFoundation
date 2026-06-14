package net.blueva.api.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
            if (!java.nio.file.Files.exists(file)) {
                ConfigIO.writeAtomic(file, codec.write(defaults));
            }
            ConfigDocument document = codec.read(ConfigIO.read(file));
            ConfigFile config = new ConfigFile(file, cache, codec, defaults, document);
            config.update();
            return config;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load config " + resourceName, exception);
        }
    }

    public static ConfigDocument parse(String text, ConfigFormat format) {
        return ConfigCodecs.of(format).read(text);
    }

    public static String write(ConfigDocument document, ConfigFormat format) {
        return ConfigCodecs.of(format).write(document);
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
