# BlueFoundation

[![](https://jitpack.io/v/BluevaDevelopment/BlueFoundation.svg)](https://jitpack.io/#BluevaDevelopment/BlueFoundation)

BlueFoundation is a lightweight API foundation for Minecraft plugins. It focuses on keeping reusable plugin infrastructure small, explicit, and easy to access from a single namespace:

```java
import net.blueva.foundation.BlueFoundation;
```

## Installation with JitPack

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.BluevaDevelopment</groupId>
        <artifactId>BlueFoundation</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```


## API structure

`BlueFoundation` is a small facade. Implementations live in dedicated packages, while public aliases keep usage centralized under `BlueFoundation.*`. Event wrappers are also split into dedicated interfaces/adapters internally.

```java
BlueFoundation.Dependencies
BlueFoundation.Version
BlueFoundation.Reflection
BlueFoundation.Materials
BlueFoundation.Items
BlueFoundation.Sounds
BlueFoundation.Scheduler
BlueFoundation.Commands
BlueFoundation.Messages
BlueFoundation.Text
BlueFoundation.Events
BlueFoundation.Configs
BlueFoundation.NPCs
```

## Runtime dependencies

Runtime dependency loading is managed through `BlueFoundation.Dependencies`.

It downloads Maven artifacts into the plugin's `libraries` folder and injects them into the plugin classloader at startup.

```java
BlueFoundation.Dependencies.load(this, BlueFoundation.Dependencies.list(
        BlueFoundation.Dependencies.mavenCentral("com.zaxxer", "HikariCP", "4.0.3")
));
```

You can also create dependency descriptors directly:

```java
BlueFoundation.Dependencies.RuntimeDependency dependency = new BlueFoundation.Dependencies.RuntimeDependency(
        "com.zaxxer",
        "HikariCP",
        "4.0.3",
        BlueFoundation.Dependencies.Repositories.MAVEN_CENTRAL
);

BlueFoundation.Dependencies.loader(this).load(dependency);
```

### Adventure runtime

BlueFoundation parses MiniMessage and serializes to legacy strings internally, so `BlueFoundation.Text` and `BlueFoundation.Messages` work without installing Adventure at runtime.

`BlueFoundation.Dependencies.loadAdventure(plugin)` is kept for backwards compatibility but is now a no-op. You do not need to load Adventure libraries manually.

Rules:

- Author user-facing text as MiniMessage.
- Use Adventure `Component` internally for text-aware APIs when you already have one.
- Serialize to legacy strings only at Bukkit boundaries that still require strings.
- Keep legacy `&` color support only as compatibility input.

## Version utilities

`BlueFoundation.Version` exposes Minecraft/Bukkit version helpers.

```java
BlueFoundation.Version.MinecraftVersion version = BlueFoundation.Version.current();

if (BlueFoundation.Version.isAtLeast(1, 20)) {
    // modern server logic
}

String bukkitVersion = BlueFoundation.Version.bukkitVersion();
String craftBukkitRevision = BlueFoundation.Version.craftBukkitRevision();
```

The parser supports classic versions like `1.8.8`, modern versions like `1.21.11`, and future-style versions like `26.1`.

## Reflection helpers

`BlueFoundation.Reflection` provides small helpers for CraftBukkit/NMS lookups and packet sending.

```java
Class<?> craftPlayer = BlueFoundation.Reflection.craftBukkitClass("entity.CraftPlayer");
Class<?> packetClass = BlueFoundation.Reflection.nmsClass("PacketPlayOutChat");
Object handle = BlueFoundation.Reflection.getHandle(player);
```

## Materials and sounds

`BlueFoundation.Materials` and `BlueFoundation.Sounds` resolve renamed constants safely by trying multiple names. Sounds also accept modern namespaced keys on servers that expose the Bukkit sound registry.

```java
Material oakSign = BlueFoundation.Materials.require("OAK_SIGN", "SIGN");
Sound levelUp = BlueFoundation.Sounds.require("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
Sound pling = BlueFoundation.Sounds.require("minecraft:block.note_block.pling", "NOTE_PLING");

BlueFoundation.Sounds.play(player, 1.0F, 1.0F, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
```

## Items

`BlueFoundation.Items` creates and edits Bukkit `ItemStack` instances. Display names and lore are MiniMessage-first, then serialized to legacy strings only because older Bukkit item metadata APIs require strings.

```java
ItemStack item = BlueFoundation.Items.builder("OAK_SIGN", "SIGN")
        .amount(1)
        .name("<gold>Game Selector</gold>")
        .lore("<gray>Right click to open")
        .glow()
        .hideAllFlags()
        .build();
```

Useful helpers:

```java
BlueFoundation.Items.name(item, "<green>Ready");
BlueFoundation.Items.lore(item, "<gray>Line one", "<yellow>Line two");
ItemStack copy = BlueFoundation.Items.builder(existingItem).name("<green>Copy").build();
BlueFoundation.Items.enchant(item, "sharpness", 1);
BlueFoundation.Items.unbreakable(item, true);
BlueFoundation.Items.customModelData(item, 1001);
BlueFoundation.Items.pdcString(item, "myplugin", "item_id", "selector");
String itemId = BlueFoundation.Items.pdcString(item, "myplugin", "item_id");
BlueFoundation.Items.skullTexture(item, "http://textures.minecraft.net/texture/...");
BlueFoundation.Items.skullValue(item, "{player}", player);
BlueFoundation.Items.editMeta(item, meta -> {
    // Add plugin-specific metadata without leaving the BlueFoundation item flow.
});
```

If your plugin already has Adventure components after placeholder processing,
use the component overloads to avoid serializing and parsing text twice:

```java
BlueFoundation.Items.name(item, titleComponent);
BlueFoundation.Items.loreComponents(item, loreComponents);
BlueFoundation.Items.loreSplit(item, "<gray>Line one\n<yellow>Line two");
```

## Text and messages

`BlueFoundation.Text` parses MiniMessage into Adventure `Component` values and serializes components back to legacy strings only for old Bukkit APIs that still require strings.

```java
Component title = BlueFoundation.Text.component("<gold>Victory!</gold>");
String inventoryTitle = BlueFoundation.Text.legacySection(title);
```

`BlueFoundation.Messages` sends Adventure components to players, command senders, action bars, and titles. Message strings are parsed as MiniMessage by default. Legacy `&` colors are accepted only as a compatibility input path.

```java
BlueFoundation.Messages.send(player, "<green>Hello!");
BlueFoundation.Messages.actionBar(player, "<yellow>Action bar text");
BlueFoundation.Messages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 70, 20);
```

## Scheduler

`BlueFoundation.Scheduler` is a Folia-aware scheduler facade with small stable task handles and tick conversion helpers. It compiles against legacy Spigot, then detects Folia at runtime and uses Paper/Folia schedulers reflectively, so consumers do not need to bundle any external scheduler library.

Global/server work:

```java
BlueFoundation.Scheduler.Task task = BlueFoundation.Scheduler.runLater(
        this,
        () -> BlueFoundation.Messages.send(player, "<green>Ready!"),
        BlueFoundation.Scheduler.seconds(3)
);

BlueFoundation.Scheduler.runAsync(this, () -> {
    // Async work
});

task.cancel();
```

Region/entity-safe Folia work:

```java
BlueFoundation.Scheduler.runAtLocation(this, location, task -> {
    // Safe for blocks/chunks at this location on Folia.
});

BlueFoundation.Scheduler.runAtEntityTimer(
        this,
        player,
        task -> player.setVelocity(vector),
        () -> getLogger().warning("Entity scheduler retired"),
        1L,
        1L
);

BlueFoundation.Scheduler.teleportAsync(player, destination).thenAccept(success -> {
    if (success) {
        BlueFoundation.Scheduler.runAtEntity(this, player, task -> BlueFoundation.Messages.send(player, "<green>Teleported!"));
    }
});
```

Supported helpers:

```java
BlueFoundation.Scheduler.runNextTick(plugin, task);
BlueFoundation.Scheduler.runLater(plugin, task, delayTicks);
BlueFoundation.Scheduler.runTimer(plugin, task, delayTicks, periodTicks);
BlueFoundation.Scheduler.runAsync(plugin, task);
BlueFoundation.Scheduler.runLaterAsync(plugin, task, delayTicks);
BlueFoundation.Scheduler.runTimerAsync(plugin, task, delayTicks, periodTicks);
BlueFoundation.Scheduler.runAtLocation(plugin, location, task);
BlueFoundation.Scheduler.runAtLocationLater(plugin, location, task, delayTicks);
BlueFoundation.Scheduler.runAtLocationTimer(plugin, location, task, delayTicks, periodTicks);
BlueFoundation.Scheduler.runAtEntity(plugin, entity, task);
BlueFoundation.Scheduler.runAtEntityWithFallback(plugin, entity, task, fallback);
BlueFoundation.Scheduler.runAtEntityLater(plugin, entity, task, fallback, delayTicks);
BlueFoundation.Scheduler.runAtEntityTimer(plugin, entity, task, fallback, delayTicks, periodTicks);
BlueFoundation.Scheduler.isFolia();
BlueFoundation.Scheduler.isOwnedByCurrentRegion(entityOrLocation);
BlueFoundation.Scheduler.teleportAsync(entity, location);
BlueFoundation.Scheduler.cancelTasks(plugin);
```

The old `sync*`/`async*` methods remain as aliases for Bukkit-style code. On Folia, global methods run through the global region scheduler; use the location/entity methods for world, block and entity mutations.

## Commands

`BlueFoundation.Commands` registers command handlers declared in `plugin.yml` and provides small sender/tab-completion helpers.

```java
BlueFoundation.Commands.register(
        this,
        "arena",
        (sender, command, label, args) -> {
            if (!BlueFoundation.Commands.hasPermission(sender, "blueva.arena", "<red>No permission.")) {
                return true;
            }
            BlueFoundation.Messages.send(sender, "<green>Arena command executed.");
            return true;
        },
        (sender, command, label, args) -> BlueFoundation.Commands.suggest(
                BlueFoundation.Commands.arg(args, 0),
                Arrays.asList("create", "delete", "edit")
        )
);
```

## Configs

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

## NPCs

`BlueFoundation.NPCs` creates packet-based player NPCs that work across Bukkit/Spigot/Paper versions without depending on versioned NMS package names. It builds a fake `ServerPlayer`/`EntityPlayer` internally and sends spawn, equipment, teleport, animation and destroy packets only to selected viewers.

This approach avoids the `ArrayIndexOutOfBoundsException` that affects NpcApi on modern Paper versions, because version detection is handled by `BlueFoundation.Version` instead of parsing `Bukkit.getServer().getClass().getPackage().getName()`.

> **Status:** usable MVP. Core features are implemented and the module compiles, but it has not yet been battle-tested on every modern Paper build. Feedback from real servers is welcome.

Initialize and shut down the module:

```java
@Override
public void onEnable() {
    BlueFoundation.NPCs.init(this);
}

@Override
public void onDisable() {
    BlueFoundation.NPCs.close();
}
```

Create and show an NPC:

```java
Npc npc = BlueFoundation.NPCs.create(location, "Shop Keeper");
npc.skin(Skin.fromPlayer(somePlayer));
npc.equipment(helmet, chestplate, leggings, boots, mainHand, offHand);
npc.showTo(player);
```

The `equipment` array follows the server's `EquipmentSlot` enum order. On modern versions that order is typically: main hand, off hand, feet, legs, chest, head.

Handle interactions:

```java
npc.onClick(event -> {
    if (event.getClickType() == NpcClickEvent.ClickType.RIGHT) {
        BlueFoundation.Messages.send(event.getPlayer(), "<green>Hello!");
    }
});
```

Movement, animations and cleanup:

```java
npc.teleport(newLocation);
npc.lookAt(targetPlayer);
npc.animate(NpcAnimation.SWING_MAIN_ARM);
npc.hideFrom(player);
npc.destroy();
```

Skins can also be fetched asynchronously from Mojang:

```java
Skin.fetch(this, playerUuid, skin -> {
    if (skin != null) {
        npc.skin(skin);
    }
});
```

Events fired by the module:

- `NpcSpawnEvent` — when an NPC is shown to a player.
- `NpcDespawnEvent` — when an NPC is hidden from a player.
- `NpcClickEvent` — when a player left/right-clicks an NPC.

## Multi-version events

`BlueFoundation.Events` provides wrapped events that register the correct Bukkit event implementation at runtime.
```java
public final class PickupListener implements BlueFoundation.Events.EntityPickup {

    @Override
    public boolean onEntityPickUpItemStack(Entity entity, Item item, int remaining, boolean cancelled) {
        return false; // return true to cancel
    }
}
```

Register wrapped listeners from your plugin:

```java
if (BlueFoundation.Events.supports(BlueFoundation.Events.Type.ENTITY_PICKUP)) {
    BlueFoundation.Events.register(
            this,
            new PickupListener(),
            EventPriority.NORMAL
    );
}
```

Current wrapped events:

- `BlueFoundation.Events.EntityPickup`
  - Uses `PlayerPickupItemEvent` on legacy servers.
  - Uses `EntityPickupItemEvent` on newer servers.
- `BlueFoundation.Events.PlayerSwapHandItems`
  - Available on servers with `PlayerSwapHandItemsEvent`.
- `BlueFoundation.Events.PlayerItemMend`
  - Available on servers with `PlayerItemMendEvent`.
- `BlueFoundation.Events.EntityToggleGlide`
  - Available on servers with `EntityToggleGlideEvent`.
- `BlueFoundation.Events.EntityAirChange`
  - Available on servers with `EntityAirChangeEvent`.
- `BlueFoundation.Events.PlayerInteractAtEntity`
  - Available on servers with `PlayerInteractAtEntityEvent`. Provides `handName` when available.
- `BlueFoundation.Events.PlayerArmorStandManipulate`
  - Available on servers with `PlayerArmorStandManipulateEvent`. Provides `slotName` and `handName` when available.

Use `BlueFoundation.Events.supports(BlueFoundation.Events.Type...)` to check capabilities before registering. Unsupported wrapped events also return `false` from `BlueFoundation.Events.register(...)`.

## Dependency API

- `BlueFoundation.Dependencies`: runtime dependency manager.
- `BlueFoundation.Dependencies.Loader`: downloads and injects runtime dependencies.
- `BlueFoundation.Dependencies.RuntimeDependency`: Maven dependency descriptor.
- `BlueFoundation.Dependencies.Repositories`: common Maven repository URLs.

Included repositories:

```java
BlueFoundation.Dependencies.Repositories.MAVEN_CENTRAL
BlueFoundation.Dependencies.Repositories.CODEMC
BlueFoundation.Dependencies.Repositories.JITPACK
```

## Goal

BlueFoundation aims to be a common foundation for Blueva plugins: small, clear, and ready to grow with multiversion tools and reusable systems for Bukkit/Spigot/Paper development.
