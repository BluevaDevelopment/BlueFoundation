# Configs

`BlueFoundation.Configs` loads YAML and TOML configs without wrapping another config API. Both formats use the same internal tree, getters, comments and auto-update logic, so plugins can start on YAML and migrate to TOML later without changing the config-facing code style.

YAML usage:

```java
ConfigFile config = BlueFoundation.Configs.yaml(this, "config.yml");

boolean enabled = config.getBoolean("features.enabled", true);
int limit = config.getInt("limits.players", 100);
double multiplier = config.getDouble("economy.multiplier", 1.0D);
List<String> worlds = config.getStringList("worlds");
Map<String, Object> database = config.getMap("database");
List<String> featureKeys = config.keys("features");

config.set("features.enabled", false);
config.setIfAbsent("features.mode", "default");
config.comment("features.enabled", "Master toggle for this feature.");
config.inlineComment("features.mode", "Used only when no arena overrides it.");
config.save();
```

Multiple file registry:

```java
ConfigRegistry configs = BlueFoundation.Configs.yamlRegistry(
        getDataFolder().toPath().resolve("modules/my-module"),
        moduleClassLoader,
        "files/"
);

configs.register("settings.yml");
configs.registerCopyOnly("kits.yml");

boolean enabled = configs.getBoolean("settings.yml", "features.enabled", true);
List<String> kits = configs.getStringList("kits.yml", "kits.default.items");
configs.set("settings.yml", "features.enabled", false);
configs.save("settings.yml");
```

Example YAML default:

```yaml
features:
  enabled: true
  mode: default

worlds:
  - world
  - nether

database:
  host: localhost
  port: 3306

npcs:
  - name: Bob
    type: villager
```

TOML usage:

```java
ConfigFile config = BlueFoundation.Configs.toml(this, "config.toml");

boolean enabled = config.getBoolean("features.enabled", true);
List<Object> npcs = config.getList("npcs");
ConfigDateTime createdAt = config.getDateTime("metadata.created_at");
```

Example TOML default:

```toml
[features]
enabled = true
mode = "default"

worlds = ["world", "nether"]

[database]
host = "localhost"
port = 3306

[[npcs]]
name = "Bob"
type = "villager"
```

Bundled defaults are copied from the plugin jar and updated on startup. BlueFoundation stores technical update metadata in a hidden `.bluefoundation/config-cache` file. When bundled defaults change, values and lists are updated only if the user still had the previous default. User-edited values are preserved. Comments are refreshed when they still match the previous bundled comments, while custom comments are kept. File writes are atomic where the filesystem supports it, and changed files are backed up under `.bluefoundation/config-backups`.

YAML uses conservative YAML 1.2-style booleans, so only `true` and `false` are parsed as booleans. TOML includes date/time and array-of-table support. Parse failures include format, line and column details.
