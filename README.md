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

## Runtime dependencies

Runtime dependency loading is managed through `BlueAPI.Dependencies`.

It downloads Maven artifacts into the plugin's `libraries` folder and injects them into the plugin classloader at startup.

```java
public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        BlueAPI.Dependencies.load(this, BlueAPI.Dependencies.list(
                BlueAPI.Dependencies.mavenCentral("dev.dejvokep", "boosted-yaml", "1.3.7"),
                BlueAPI.Dependencies.jitPack("com.github.MrMicky-FR", "FastBoard", "2.1.5")
        ));
    }
}
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
