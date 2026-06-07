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

`BlueAPI` is a small facade. Implementations live in dedicated packages, while the public aliases keep usage centralized under `BlueAPI.*`.

```java
BlueAPI.Dependencies
BlueAPI.Version
BlueAPI.Reflection
BlueAPI.Materials
BlueAPI.Sounds
BlueAPI.Messages
BlueAPI.Events
```

## Runtime dependencies

Runtime dependency loading is managed through `BlueAPI.Dependencies`.

It downloads Maven artifacts into the plugin's `libraries` folder and injects them into the plugin classloader at startup.

```java
BlueAPI.Dependencies.load(this, BlueAPI.Dependencies.list(
        BlueAPI.Dependencies.mavenCentral("dev.dejvokep", "boosted-yaml", "1.3.7"),
        BlueAPI.Dependencies.jitPack("com.github.MrMicky-FR", "FastBoard", "2.1.5")
));
```

You can also create dependency descriptors directly:

```java
BlueAPI.Dependencies.RuntimeDependency dependency = new BlueAPI.Dependencies.RuntimeDependency(
        "dev.dejvokep",
        "boosted-yaml",
        "1.3.7",
        BlueAPI.Dependencies.Repositories.MAVEN_CENTRAL
);

BlueAPI.Dependencies.loader(this).load(dependency);
```

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

## Messages

`BlueAPI.Messages` provides basic multi-version player messaging helpers.

```java
BlueAPI.Messages.send(player, "&aHello!");
BlueAPI.Messages.actionBar(player, "&eAction bar text");
BlueAPI.Messages.title(player, "&bTitle", "&7Subtitle", 10, 70, 20);
```

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
boolean registered = BlueAPI.Events.register(
        this,
        new PickupListener(),
        EventPriority.NORMAL
);
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

Unsupported wrapped events simply return `false` from `BlueAPI.Events.register(...)`.

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
