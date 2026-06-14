package net.blueva.api.config;

final class ConfigCodecs {
    private static final ConfigCodec YAML = new YamlConfigCodec();
    private static final ConfigCodec TOML = new TomlConfigCodec();

    private ConfigCodecs() {
    }

    static ConfigCodec of(ConfigFormat format) {
        if (format == ConfigFormat.TOML) {
            return TOML;
        }
        return YAML;
    }
}
