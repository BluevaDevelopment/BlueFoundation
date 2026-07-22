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
        <version>v26.16</version>
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
BlueFoundation.Entities
BlueFoundation.Scheduler
BlueFoundation.Commands
BlueFoundation.Messages
BlueFoundation.Text
BlueFoundation.Events
BlueFoundation.Configs
BlueFoundation.NPCs
BlueFoundation.Scoreboards
```

## Documentation

- [Runtime dependencies](docs/dependencies.md) — `BlueFoundation.Dependencies`
- [Version utilities](docs/version.md) — `BlueFoundation.Version`
- [Reflection helpers](docs/reflection.md) — `BlueFoundation.Reflection`
- [Materials and sounds](docs/materials-sounds.md) — `BlueFoundation.Materials`, `BlueFoundation.Sounds`
- [Entities](docs/entities.md) — `BlueFoundation.Entities`
- [Items](docs/items.md) — `BlueFoundation.Items`
- [Text and messages](docs/text-messages.md) — `BlueFoundation.Text`, `BlueFoundation.Messages`
- [Scheduler](docs/scheduler.md) — `BlueFoundation.Scheduler`
- [Commands](docs/commands.md) — `BlueFoundation.Commands`
- [Configs](docs/configs.md) — `BlueFoundation.Configs`
- [NPCs](docs/npcs.md) — `BlueFoundation.NPCs`
- [Scoreboard](docs/scoreboards.md) — `BlueFoundation.Scoreboards`
- [Multi-version events](docs/events.md) — `BlueFoundation.Events`

## Goal

BlueFoundation aims to be a common foundation for Blueva plugins: small, clear, and ready to grow with multiversion tools and reusable systems for Bukkit/Spigot/Paper development.
