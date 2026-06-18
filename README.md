# BlueAPI

[![](https://jitpack.io/v/BluevaDevelopment/BlueAPI.svg)](https://jitpack.io/#BluevaDevelopment/BlueAPI)

BlueAPI is a lightweight API foundation for Minecraft plugins. It focuses on keeping reusable plugin infrastructure small, explicit, and easy to access from a single namespace:

```java
import net.blueva.api.BlueAPI;
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
        <artifactId>BlueAPI</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```


## API structure

`BlueAPI` is a small facade. Implementations live in dedicated packages, while public aliases keep usage centralized under `BlueAPI.*`. Event wrappers are also split into dedicated interfaces/adapters internally.

```java
BlueAPI.Dependencies
BlueAPI.Version
BlueAPI.Reflection
BlueAPI.Materials
BlueAPI.Items
BlueAPI.Sounds
BlueAPI.Scheduler
BlueAPI.Commands
BlueAPI.Messages
BlueAPI.Text
BlueAPI.Events
BlueAPI.Configs
BlueAPI.NPCs
```

## Runtime dependencies

Runtime dependency loading is managed through `BlueAPI.Dependencies`.

It downloads Maven artifacts into the plugin's `libraries` folder and injects them into the plugin classloader at startup.

```java
BlueAPI.Dependencies.load(this, BlueAPI.Dependencies.list(
        BlueAPI.Dependencies.jitPack("com.github.Eisi05", "NpcApi", "2.2.2")
));
```

You can also create dependency descriptors directly:

```java
BlueAPI.Dependencies.RuntimeDependency dependency = new BlueAPI.Dependencies.RuntimeDependency(
        "com.github.Eisi05",
        "NpcApi",
        "2.2.2",
        BlueAPI.Dependencies.Repositories.JITPACK
);

BlueAPI.Dependencies.loader(this).load(dependency);
```

### Adventure runtime

BlueAPI and Blueva plugins use Adventure + MiniMessage for text, not `ChatColor`.

Rules:

- Author user-facing text as MiniMessage.
- Use Adventure `Component` internally for text-aware APIs.
- Serialize to legacy strings only at Bukkit boundaries that still require strings.
- Keep legacy `&` color support only as compatibility input.

Call the Adventure runtime profile before using `BlueAPI.Text` or `BlueAPI.Messages` on servers that may not provide Adventure natively:

```java
@Override
public void onLoad() {
    BlueAPI.Dependencies.loadAdventure(this);
}

@Override
public void onEnable() {
    BlueAPI.Messages.init(this);
}

@Override
public void onDisable() {
    BlueAPI.Messages.close();
}
```

Paper servers with native Adventure audiences do not need `adventure-platform-bukkit`; Spigot/Bukkit servers get the required Adventure libraries through the runtime dependency loader.

The Bukkit Adventure platform targets Paper, Spigot, and Bukkit across old and modern Minecraft versions. BlueAPI still compiles against Java 8 / Spigot API 1.8.8, so it compiles against the latest Java-8-compatible Adventure 4.x line. Runtime loading is not hard-capped to that line: modern Java 21 + Minecraft 1.21+ Spigot/Bukkit profiles can inject Adventure 5.x core artifacts while the Bukkit platform bridge stays on its own 4.x line.

## Version utilities

`BlueAPI.Version` exposes Minecraft/Bukkit version helpers.

```java
BlueAPI.Version.MinecraftVersion version = BlueAPI.Version.current();

if (BlueAPI.Version.isAtLeast(1, 20)) {
    // modern server logic
}

String bukkitVersion = BlueAPI.Version.bukkitVersion();
String craftBukkitRevision = BlueAPI.Version.craftBukkitRevision();
```

The parser supports classic versions like `1.8.8`, modern versions like `1.21.11`, and future-style versions like `26.1`.

## Reflection helpers

`BlueAPI.Reflection` provides small helpers for CraftBukkit/NMS lookups and packet sending.

```java
Class<?> craftPlayer = BlueAPI.Reflection.craftBukkitClass("entity.CraftPlayer");
Class<?> packetClass = BlueAPI.Reflection.nmsClass("PacketPlayOutChat");
Object handle = BlueAPI.Reflection.getHandle(player);
```

## Materials and sounds

`BlueAPI.Materials` and `BlueAPI.Sounds` resolve renamed enum constants safely by trying multiple names.

```java
Material oakSign = BlueAPI.Materials.require("OAK_SIGN", "SIGN");
Sound levelUp = BlueAPI.Sounds.require("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");

BlueAPI.Sounds.play(player, 1.0F, 1.0F, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
```

## Items

`BlueAPI.Items` creates and edits Bukkit `ItemStack` instances. Display names and lore are MiniMessage-first, then serialized to legacy strings only because older Bukkit item metadata APIs require strings.

```java
ItemStack item = BlueAPI.Items.builder("OAK_SIGN", "SIGN")
        .amount(1)
        .name("<gold>Game Selector</gold>")
        .lore("<gray>Right click to open")
        .glow()
        .hideAllFlags()
        .build();
```

Useful helpers:

```java
BlueAPI.Items.name(item, "<green>Ready");
BlueAPI.Items.lore(item, "<gray>Line one", "<yellow>Line two");
BlueAPI.Items.enchant(item, "sharpness", 1);
BlueAPI.Items.unbreakable(item, true);
```

## Text and messages

`BlueAPI.Text` parses MiniMessage into Adventure `Component` values and serializes components back to legacy strings only for old Bukkit APIs that still require strings.

```java
Component title = BlueAPI.Text.component("<gold>Victory!</gold>");
String inventoryTitle = BlueAPI.Text.legacySection(title);
```

`BlueAPI.Messages` sends Adventure components to players, command senders, action bars, and titles. Message strings are parsed as MiniMessage by default. Legacy `&` colors are accepted only as a compatibility input path.

```java
BlueAPI.Messages.send(player, "<green>Hello!");
BlueAPI.Messages.actionBar(player, "<yellow>Action bar text");
BlueAPI.Messages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 70, 20);
```

## Scheduler

`BlueAPI.Scheduler` is a Folia-aware scheduler facade with small stable task handles and tick conversion helpers. It compiles against legacy Spigot, then detects Folia at runtime and uses Paper/Folia schedulers reflectively, so consumers do not need to bundle any external scheduler library.

Global/server work:

```java
BlueAPI.Scheduler.Task task = BlueAPI.Scheduler.runLater(
        this,
        () -> BlueAPI.Messages.send(player, "<green>Ready!"),
        BlueAPI.Scheduler.seconds(3)
);

BlueAPI.Scheduler.runAsync(this, () -> {
    // Async work
});

task.cancel();
```

Region/entity-safe Folia work:

```java
BlueAPI.Scheduler.runAtLocation(this, location, task -> {
    // Safe for blocks/chunks at this location on Folia.
});

BlueAPI.Scheduler.runAtEntityTimer(
        this,
        player,
        task -> player.setVelocity(vector),
        () -> getLogger().warning("Entity scheduler retired"),
        1L,
        1L
);

BlueAPI.Scheduler.teleportAsync(player, destination).thenAccept(success -> {
    if (success) {
        BlueAPI.Scheduler.runAtEntity(this, player, task -> BlueAPI.Messages.send(player, "<green>Teleported!"));
    }
});
```

Supported helpers:

```java
BlueAPI.Scheduler.runNextTick(plugin, task);
BlueAPI.Scheduler.runLater(plugin, task, delayTicks);
BlueAPI.Scheduler.runTimer(plugin, task, delayTicks, periodTicks);
BlueAPI.Scheduler.runAsync(plugin, task);
BlueAPI.Scheduler.runLaterAsync(plugin, task, delayTicks);
BlueAPI.Scheduler.runTimerAsync(plugin, task, delayTicks, periodTicks);
BlueAPI.Scheduler.runAtLocation(plugin, location, task);
BlueAPI.Scheduler.runAtLocationLater(plugin, location, task, delayTicks);
BlueAPI.Scheduler.runAtLocationTimer(plugin, location, task, delayTicks, periodTicks);
BlueAPI.Scheduler.runAtEntity(plugin, entity, task);
BlueAPI.Scheduler.runAtEntityWithFallback(plugin, entity, task, fallback);
BlueAPI.Scheduler.runAtEntityLater(plugin, entity, task, fallback, delayTicks);
BlueAPI.Scheduler.runAtEntityTimer(plugin, entity, task, fallback, delayTicks, periodTicks);
BlueAPI.Scheduler.isFolia();
BlueAPI.Scheduler.isOwnedByCurrentRegion(entityOrLocation);
BlueAPI.Scheduler.teleportAsync(entity, location);
BlueAPI.Scheduler.cancelTasks(plugin);
```

The old `sync*`/`async*` methods remain as aliases for Bukkit-style code. On Folia, global methods run through the global region scheduler; use the location/entity methods for world, block and entity mutations.

## Commands

`BlueAPI.Commands` registers command handlers declared in `plugin.yml` and provides small sender/tab-completion helpers.

```java
BlueAPI.Commands.register(
        this,
        "arena",
        (sender, command, label, args) -> {
            if (!BlueAPI.Commands.hasPermission(sender, "blueva.arena", "<red>No permission.")) {
                return true;
            }
            BlueAPI.Messages.send(sender, "<green>Arena command executed.");
            return true;
        },
        (sender, command, label, args) -> BlueAPI.Commands.suggest(
                BlueAPI.Commands.arg(args, 0),
                Arrays.asList("create", "delete", "edit")
        )
);
```

## Configs

`BlueAPI.Configs` loads YAML and TOML configs without wrapping another config API. Both formats use the same internal tree, getters, comments and auto-update logic, so plugins can start on YAML and migrate to TOML later without changing the config-facing code style.

YAML usage:

```java
ConfigFile config = BlueAPI.Configs.yaml(this, "config.yml");

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
ConfigFile config = BlueAPI.Configs.toml(this, "config.toml");

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

Bundled defaults are copied from the plugin jar and updated on startup. BlueAPI stores technical update metadata in a hidden `.blueapi/config-cache` file. When bundled defaults change, values and lists are updated only if the user still had the previous default. User-edited values are preserved. Comments are refreshed when they still match the previous bundled comments, while custom comments are kept. File writes are atomic where the filesystem supports it, and changed files are backed up under `.blueapi/config-backups`.

YAML uses conservative YAML 1.2-style booleans, so only `true` and `false` are parsed as booleans. TOML includes date/time and array-of-table support. Parse failures include format, line and column details.

## NPCs

`BlueAPI.NPCs` creates packet-based player NPCs that work across Bukkit/Spigot/Paper versions without depending on versioned NMS package names. It builds a fake `ServerPlayer`/`EntityPlayer` internally and sends spawn, equipment, teleport, animation and destroy packets only to selected viewers.

This approach avoids the `ArrayIndexOutOfBoundsException` that affects NpcApi on modern Paper versions, because version detection is handled by `BlueAPI.Version` instead of parsing `Bukkit.getServer().getClass().getPackage().getName()`.

> **Status:** usable MVP. Core features are implemented and the module compiles, but it has not yet been battle-tested on every modern Paper build. Feedback from real servers is welcome.

Initialize and shut down the module:

```java
@Override
public void onEnable() {
    BlueAPI.NPCs.init(this);
}

@Override
public void onDisable() {
    BlueAPI.NPCs.close();
}
```

Create and show an NPC:

```java
Npc npc = BlueAPI.NPCs.create(location, "Shop Keeper");
npc.skin(Skin.fromPlayer(somePlayer));
npc.equipment(helmet, chestplate, leggings, boots, mainHand, offHand);
npc.showTo(player);
```

The `equipment` array follows the server's `EquipmentSlot` enum order. On modern versions that order is typically: main hand, off hand, feet, legs, chest, head.

Handle interactions:

```java
npc.onClick(event -> {
    if (event.getClickType() == NpcClickEvent.ClickType.RIGHT) {
        BlueAPI.Messages.send(event.getPlayer(), "<green>Hello!");
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

`BlueAPI.Events` provides wrapped events that register the correct Bukkit event implementation at runtime.
```java
public final class PickupListener implements BlueAPI.Events.EntityPickup {

    @Override
    public boolean onEntityPickUpItemStack(Entity entity, Item item, int remaining, boolean cancelled) {
        return false; // return true to cancel
    }
}
```

Register wrapped listeners from your plugin:

```java
if (BlueAPI.Events.supports(BlueAPI.Events.Type.ENTITY_PICKUP)) {
    BlueAPI.Events.register(
            this,
            new PickupListener(),
            EventPriority.NORMAL
    );
}
```

Current wrapped events:

- `BlueAPI.Events.EntityPickup`
  - Uses `PlayerPickupItemEvent` on legacy servers.
  - Uses `EntityPickupItemEvent` on newer servers.
- `BlueAPI.Events.PlayerSwapHandItems`
  - Available on servers with `PlayerSwapHandItemsEvent`.
- `BlueAPI.Events.PlayerItemMend`
  - Available on servers with `PlayerItemMendEvent`.
- `BlueAPI.Events.EntityToggleGlide`
  - Available on servers with `EntityToggleGlideEvent`.
- `BlueAPI.Events.EntityAirChange`
  - Available on servers with `EntityAirChangeEvent`.
- `BlueAPI.Events.PlayerInteractAtEntity`
  - Available on servers with `PlayerInteractAtEntityEvent`. Provides `handName` when available.
- `BlueAPI.Events.PlayerArmorStandManipulate`
  - Available on servers with `PlayerArmorStandManipulateEvent`. Provides `slotName` and `handName` when available.

Use `BlueAPI.Events.supports(BlueAPI.Events.Type...)` to check capabilities before registering. Unsupported wrapped events also return `false` from `BlueAPI.Events.register(...)`.

## Dependency API

- `BlueAPI.Dependencies`: runtime dependency manager.
- `BlueAPI.Dependencies.Loader`: downloads and injects runtime dependencies.
- `BlueAPI.Dependencies.RuntimeDependency`: Maven dependency descriptor.
- `BlueAPI.Dependencies.Repositories`: common Maven repository URLs.

Included repositories:

```java
BlueAPI.Dependencies.Repositories.MAVEN_CENTRAL
BlueAPI.Dependencies.Repositories.CODEMC
BlueAPI.Dependencies.Repositories.JITPACK
```

## Goal

BlueAPI aims to be a common foundation for Blueva plugins: small, clear, and ready to grow with multiversion tools and reusable systems for Bukkit/Spigot/Paper development.
