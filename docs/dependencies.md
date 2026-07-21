# Runtime dependencies

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

## Adventure runtime

BlueFoundation parses MiniMessage and serializes to legacy strings internally, so `BlueFoundation.Text` and `BlueFoundation.Messages` work without installing Adventure at runtime.

`BlueFoundation.Dependencies.loadAdventure(plugin)` is kept for backwards compatibility but is now a no-op. You do not need to load Adventure libraries manually.

Rules:

- Author user-facing text as MiniMessage.
- Use Adventure `Component` internally for text-aware APIs when you already have one.
- Serialize to legacy strings only at Bukkit boundaries that still require strings.
- Keep legacy `&` color support only as compatibility input.

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
