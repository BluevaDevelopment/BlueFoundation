<p align="center">
  <img src="docs/assets/bluefoundation-logo.png" alt="BlueFoundation" width="760">
</p>

<p align="center">
  <strong>A lightweight API foundation for Minecraft (Bukkit/Spigot/Paper/Folia, Velocity, BungeeCord) and Hytale servers.</strong>
</p>

<p align="center">
  <img alt="Bukkit" src="https://img.shields.io/badge/Bukkit-supported-F7A100">
  <img alt="Spigot" src="https://img.shields.io/badge/Spigot-supported-ED8106">
  <img alt="Paper" src="https://img.shields.io/badge/Paper-supported-00C7B7">
  <img alt="Folia" src="https://img.shields.io/badge/Folia-supported-8E44AD">
  <img alt="Velocity" src="https://img.shields.io/badge/Velocity-supported-6E56CF">
  <img alt="BungeeCord" src="https://img.shields.io/badge/BungeeCord-supported-5C5C5C">
  <img alt="Hytale" src="https://img.shields.io/badge/Hytale-supported-2ECC71">
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-26.38-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-8+-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Build" src="https://img.shields.io/badge/build-Gradle-02303A?logo=gradle&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-blue">
</p>

BlueFoundation focuses on keeping reusable plugin infrastructure small, explicit, and easy to access from a single namespace:

```java
import net.blueva.foundation.BlueFoundation;
```

Multi-module Gradle project: one jar per platform, plus a shared `common` core pulled in transitively.

```
common/       -> BlueFoundation-Common
bukkit/       -> BlueFoundation            (Bukkit/Spigot/Paper/Folia)
velocity/     -> BlueFoundation-Velocity
bungeecord/   -> BlueFoundation-BungeeCord
hytale/       -> BlueFoundation-Hytale
```

## Feature matrix

✅ full · ⚠️ partial · ❌ not applicable / not attempted. See each platform's docs page for the details behind a ⚠️.

| Facade | Bukkit | Velocity | BungeeCord | Hytale |
|---|:---:|:---:|:---:|:---:|
| `Dependencies` | ✅ | ✅ | ✅ | ✅ |
| `Reflection` | ✅ | ⚠️ generic only | ⚠️ generic only | ⚠️ generic only |
| `Configs` | ✅ | ✅ | ✅ | ⚠️ standalone |
| `Version` | ✅ | ⚠️ proxy/protocol | ⚠️ proxy/protocol | ⚠️ manifest semver |
| `Text` / `Messages` | ✅ | ✅ | ✅ | ✅ |
| `Commands` | ✅ | ⚠️ basic | ⚠️ basic | ⚠️ basic |
| `Scheduler` | ✅ incl. Folia | ✅ | ✅ | ✅ |
| `BossBars` | ✅ | ✅ | ❌ | ❌ |
| `Scoreboards` | ✅ | ❌ | ❌ | ⚠️ experimental |
| `Sounds` | ✅ | ❌ | ❌ | ✅ |
| `Music` | ✅ | ❌ | ❌ | ⚠️ MIDI only |
| `Players` | ✅ | ⚠️ partial | ⚠️ partial | ❌ |
| `Menus` / `Forms` | ✅ | ❌ | ❌ | ❌ |
| `Dialogs` | ⚠️ 1.21.6+ | ❌ | ❌ | ❌ |
| `Materials`, `Items`, `Entities`, `NPCs`, `Particles`, `Attributes`, `GameRules`, `Inventories`, `Hologram`, `Blocks`, `Worlds` | ✅ | ❌ | ❌ | ❌ |

## Installation

`repo.blueva.net` is a public Maven repository, no authentication needed.

| Platform | Artifact |
|---|---|
| Bukkit / Spigot / Paper | `net.blueva.foundation:BlueFoundation:26.38` |
| Velocity | `net.blueva.foundation:BlueFoundation-Velocity:26.38` |
| BungeeCord | `net.blueva.foundation:BlueFoundation-BungeeCord:26.38` |
| Hytale | `net.blueva.foundation:BlueFoundation-Hytale:26.38` |

```kotlin
repositories { maven("https://repo.blueva.net/releases") }
dependencies { compileOnly("net.blueva.foundation:BlueFoundation:26.38") } // swap the artifact id above for other platforms
```

<details>
<summary>Maven</summary>

```xml
<repositories>
    <repository>
        <id>blueva</id>
        <url>https://repo.blueva.net/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>net.blueva.foundation</groupId>
        <artifactId>BlueFoundation</artifactId>
        <version>26.38</version>
    </dependency>
</dependencies>
```

</details>

## Documentation

- [Common module](docs/common.md) - shared core, pulled in transitively

**Bukkit / Spigot / Paper**

[Dependencies](docs/dependencies.md) · [Version](docs/version.md) · [Reflection](docs/reflection.md) · [Materials and sounds](docs/materials-sounds.md) · [Music](docs/music.md) · [Entities](docs/entities.md) · [Items](docs/items.md) · [Text and messages](docs/text-messages.md) · [Scheduler](docs/scheduler.md) · [Commands](docs/commands.md) · [Configs](docs/configs.md) · [NPCs](docs/npcs.md) · [Scoreboard](docs/scoreboards.md) · [Multi-version events](docs/events.md) · [Particles](docs/particles.md) · [Attributes](docs/attributes.md) · [Game rules](docs/gamerules.md) · [Boss bars](docs/bossbars.md) · [Worlds, blocks, players and inventories](docs/world-players.md) · [Menus, forms and dialogs](docs/menus.md)

**Proxies and Hytale**

- [Velocity](docs/velocity.md)
- [BungeeCord](docs/bungeecord.md)
- [Hytale](docs/hytale.md)

## Goal

BlueFoundation aims to be a common foundation for Blueva plugins: small, clear, and ready to grow with multiversion tools and reusable systems - across Bukkit/Spigot/Paper, Velocity, BungeeCord, and Hytale.

## License

GPL-3.0. See [LICENSE](LICENSE).
