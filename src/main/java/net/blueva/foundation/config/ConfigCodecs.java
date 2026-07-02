package net.blueva.foundation.config;

final class ConfigCodecs {
    private static final ConfigCodec YAML = new YamlConfigCodec();
    private static final ConfigCodec TOML = new TomlConfigCodec();
    private static final ConfigCodec JSON = new JsonConfigCodec();

    private ConfigCodecs() {
    }

    static ConfigCodec of(ConfigFormat format) {
        if (format == ConfigFormat.TOML) {
            return TOML;
        }
        if (format == ConfigFormat.JSON) {
            return JSON;
        }
        return YAML;
    }
}
