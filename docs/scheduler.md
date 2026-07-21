# Scheduler

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
