package net.blueva.api.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler facade with Bukkit, Paper and Folia-aware dispatch.
 * <p>
 * The class intentionally compiles against old Spigot APIs and resolves Paper/Folia schedulers reflectively at runtime.
 */
public class Scheduler {

    private static final FoliaRuntime FOLIA = FoliaRuntime.create();

    protected Scheduler() {
    }

    public static Task sync(Plugin plugin, Runnable runnable) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.global(plugin, runnable)
                : BukkitSchedulerBridge.sync(plugin, runnable);
    }

    public static Task sync(Plugin plugin, Consumer<Task> consumer) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.global(plugin, consumer)
                : BukkitSchedulerBridge.sync(plugin, consumer);
    }

    public static CompletableFuture<Void> runNextTick(Plugin plugin, Runnable runnable) {
        sync(plugin, runnable);
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> runNextTick(Plugin plugin, Consumer<Task> consumer) {
        require(plugin, consumer);
        if (FOLIA.available()) {
            return FOLIA.globalFuture(plugin, consumer);
        }
        sync(plugin, consumer);
        return CompletableFuture.completedFuture(null);
    }

    public static Task syncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.globalLater(plugin, runnable, safeFoliaDelay(delayTicks))
                : BukkitSchedulerBridge.syncLater(plugin, runnable, safeTicks(delayTicks));
    }

    public static Task syncLater(Plugin plugin, Consumer<Task> consumer, long delayTicks) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.globalLater(plugin, consumer, safeFoliaDelay(delayTicks))
                : BukkitSchedulerBridge.syncLater(plugin, consumer, safeTicks(delayTicks));
    }

    public static Task syncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.globalTimer(plugin, runnable, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : BukkitSchedulerBridge.syncTimer(plugin, runnable, safeTicks(delayTicks), safePeriod(periodTicks));
    }

    public static Task syncTimer(Plugin plugin, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.globalTimer(plugin, consumer, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : BukkitSchedulerBridge.syncTimer(plugin, consumer, safeTicks(delayTicks), safePeriod(periodTicks));
    }

    public static Task async(Plugin plugin, Runnable runnable) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.async(plugin, runnable)
                : BukkitSchedulerBridge.async(plugin, runnable);
    }

    public static Task async(Plugin plugin, Consumer<Task> consumer) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.async(plugin, consumer)
                : BukkitSchedulerBridge.async(plugin, consumer);
    }

    public static Task asyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.asyncLater(plugin, runnable, ticksToMillis(safeTicks(delayTicks)))
                : BukkitSchedulerBridge.asyncLater(plugin, runnable, safeTicks(delayTicks));
    }

    public static Task asyncLater(Plugin plugin, Consumer<Task> consumer, long delayTicks) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.asyncLater(plugin, consumer, ticksToMillis(safeTicks(delayTicks)))
                : BukkitSchedulerBridge.asyncLater(plugin, consumer, safeTicks(delayTicks));
    }

    public static Task asyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        require(plugin, runnable);
        return FOLIA.available()
                ? FOLIA.asyncTimer(plugin, runnable, ticksToMillis(safeTicks(delayTicks)), ticksToMillis(safePeriod(periodTicks)))
                : BukkitSchedulerBridge.asyncTimer(plugin, runnable, safeTicks(delayTicks), safePeriod(periodTicks));
    }

    public static Task asyncTimer(Plugin plugin, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        require(plugin, consumer);
        return FOLIA.available()
                ? FOLIA.asyncTimer(plugin, consumer, ticksToMillis(safeTicks(delayTicks)), ticksToMillis(safePeriod(periodTicks)))
                : BukkitSchedulerBridge.asyncTimer(plugin, consumer, safeTicks(delayTicks), safePeriod(periodTicks));
    }

    public static Task runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return syncLater(plugin, runnable, delayTicks);
    }

    public static Task runLater(Plugin plugin, Consumer<Task> consumer, long delayTicks) {
        return syncLater(plugin, consumer, delayTicks);
    }

    public static Task runTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return syncTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static Task runTimer(Plugin plugin, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        return syncTimer(plugin, consumer, delayTicks, periodTicks);
    }

    public static Task runAsync(Plugin plugin, Runnable runnable) {
        return async(plugin, runnable);
    }

    public static Task runAsync(Plugin plugin, Consumer<Task> consumer) {
        return async(plugin, consumer);
    }

    public static Task runLaterAsync(Plugin plugin, Runnable runnable, long delayTicks) {
        return asyncLater(plugin, runnable, delayTicks);
    }

    public static Task runLaterAsync(Plugin plugin, Consumer<Task> consumer, long delayTicks) {
        return asyncLater(plugin, consumer, delayTicks);
    }

    public static Task runTimerAsync(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return asyncTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static Task runTimerAsync(Plugin plugin, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        return asyncTimer(plugin, consumer, delayTicks, periodTicks);
    }

    public static CompletableFuture<Void> runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        require(plugin, runnable);
        if (FOLIA.available() && location != null) {
            return FOLIA.locationFuture(plugin, location, task -> runnable.run());
        }
        sync(plugin, runnable);
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> runAtLocation(Plugin plugin, Location location, Consumer<Task> consumer) {
        require(plugin, consumer);
        if (FOLIA.available() && location != null) {
            return FOLIA.locationFuture(plugin, location, consumer);
        }
        sync(plugin, consumer);
        return CompletableFuture.completedFuture(null);
    }

    public static Task runAtLocationLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        require(plugin, runnable);
        return FOLIA.available() && location != null
                ? FOLIA.locationLater(plugin, location, runnable, safeFoliaDelay(delayTicks))
                : syncLater(plugin, runnable, delayTicks);
    }

    public static Task runAtLocationLater(Plugin plugin, Location location, Consumer<Task> consumer, long delayTicks) {
        require(plugin, consumer);
        return FOLIA.available() && location != null
                ? FOLIA.locationLater(plugin, location, consumer, safeFoliaDelay(delayTicks))
                : syncLater(plugin, consumer, delayTicks);
    }

    public static Task runAtLocationTimer(Plugin plugin, Location location, Runnable runnable, long delayTicks, long periodTicks) {
        require(plugin, runnable);
        return FOLIA.available() && location != null
                ? FOLIA.locationTimer(plugin, location, runnable, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : syncTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static Task runAtLocationTimer(Plugin plugin, Location location, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        require(plugin, consumer);
        return FOLIA.available() && location != null
                ? FOLIA.locationTimer(plugin, location, consumer, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : syncTimer(plugin, consumer, delayTicks, periodTicks);
    }

    public static CompletableFuture<Boolean> runAtEntity(Plugin plugin, Entity entity, Runnable runnable) {
        require(plugin, runnable);
        if (FOLIA.available() && entity != null) {
            return FOLIA.entityFuture(plugin, entity, task -> runnable.run(), null);
        }
        sync(plugin, runnable);
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    public static CompletableFuture<Boolean> runAtEntity(Plugin plugin, Entity entity, Consumer<Task> consumer) {
        require(plugin, consumer);
        if (FOLIA.available() && entity != null) {
            return FOLIA.entityFuture(plugin, entity, consumer, null);
        }
        sync(plugin, consumer);
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    public static CompletableFuture<Boolean> runAtEntityWithFallback(Plugin plugin, Entity entity, Consumer<Task> consumer, Runnable fallback) {
        require(plugin, consumer);
        if (FOLIA.available() && entity != null) {
            return FOLIA.entityFuture(plugin, entity, consumer, fallback);
        }
        sync(plugin, consumer);
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    public static Task runAtEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        return runAtEntityLater(plugin, entity, runnable, null, delayTicks);
    }

    public static Task runAtEntityLater(Plugin plugin, Entity entity, Runnable runnable, Runnable fallback, long delayTicks) {
        require(plugin, runnable);
        return FOLIA.available() && entity != null
                ? FOLIA.entityLater(plugin, entity, task -> runnable.run(), fallback, safeFoliaDelay(delayTicks))
                : syncLater(plugin, runnable, delayTicks);
    }

    public static Task runAtEntityLater(Plugin plugin, Entity entity, Consumer<Task> consumer, long delayTicks) {
        return runAtEntityLater(plugin, entity, consumer, null, delayTicks);
    }

    public static Task runAtEntityLater(Plugin plugin, Entity entity, Consumer<Task> consumer, Runnable fallback, long delayTicks) {
        require(plugin, consumer);
        return FOLIA.available() && entity != null
                ? FOLIA.entityLater(plugin, entity, consumer, fallback, safeFoliaDelay(delayTicks))
                : syncLater(plugin, consumer, delayTicks);
    }

    public static Task runAtEntityTimer(Plugin plugin, Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        return runAtEntityTimer(plugin, entity, runnable, null, delayTicks, periodTicks);
    }

    public static Task runAtEntityTimer(Plugin plugin, Entity entity, Runnable runnable, Runnable fallback, long delayTicks, long periodTicks) {
        require(plugin, runnable);
        return FOLIA.available() && entity != null
                ? FOLIA.entityTimer(plugin, entity, task -> runnable.run(), fallback, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : syncTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static Task runAtEntityTimer(Plugin plugin, Entity entity, Consumer<Task> consumer, long delayTicks, long periodTicks) {
        return runAtEntityTimer(plugin, entity, consumer, null, delayTicks, periodTicks);
    }

    public static Task runAtEntityTimer(Plugin plugin, Entity entity, Consumer<Task> consumer, Runnable fallback, long delayTicks, long periodTicks) {
        require(plugin, consumer);
        return FOLIA.available() && entity != null
                ? FOLIA.entityTimer(plugin, entity, consumer, fallback, safeFoliaDelay(delayTicks), safePeriod(periodTicks))
                : syncTimer(plugin, consumer, delayTicks, periodTicks);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return teleportAsync(entity, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        return SchedulerTeleport.teleportAsync(entity, location, cause);
    }

    public static void cancelTasks(Plugin plugin) {
        if (plugin == null) {
            return;
        }
        if (FOLIA.available()) {
            FOLIA.cancelTasks(plugin);
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    public static boolean isFolia() {
        return FOLIA.available();
    }

    public static boolean supportsFoliaSchedulers() {
        return FOLIA.schedulersAvailable();
    }

    public static String platform() {
        return FOLIA.available() ? "FOLIA" : "BUKKIT";
    }

    public static boolean isOwnedByCurrentRegion(Location location) {
        return FOLIA.available() && location != null ? FOLIA.isOwnedByCurrentRegion(location) : isPrimaryThread();
    }

    public static boolean isOwnedByCurrentRegion(Block block) {
        return block != null && isOwnedByCurrentRegion(block.getLocation());
    }

    public static boolean isOwnedByCurrentRegion(Entity entity) {
        return FOLIA.available() && entity != null ? FOLIA.isOwnedByCurrentRegion(entity) : isPrimaryThread();
    }

    public static boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return FOLIA.available() && world != null ? FOLIA.isOwnedByCurrentRegion(world, chunkX, chunkZ) : isPrimaryThread();
    }

    public static boolean isGlobalTickThread() {
        return FOLIA.available() ? FOLIA.isGlobalTickThread() : isPrimaryThread();
    }

    public static long seconds(double seconds) {
        if (seconds <= 0D) {
            return 0L;
        }
        return Math.round(seconds * 20D);
    }

    public static long milliseconds(long milliseconds) {
        if (milliseconds <= 0L) {
            return 0L;
        }
        return Math.max(1L, (TimeUnit.MILLISECONDS.toMillis(milliseconds) + 49L) / 50L);
    }

    public static long ticks(long ticks) {
        return safeTicks(ticks);
    }

    public static boolean isPrimaryThread() {
        try {
            return Bukkit.isPrimaryThread();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void require(Plugin plugin, Runnable runnable) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (runnable == null) {
            throw new IllegalArgumentException("runnable cannot be null");
        }
    }

    private static void require(Plugin plugin, Consumer<Task> consumer) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null");
        }
    }

    private static long safeTicks(long ticks) {
        return Math.max(0L, ticks);
    }

    private static long safeFoliaDelay(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long safePeriod(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * 50L;
    }

    /** Stable scheduled task handle. */
    public interface Task {
        int id();

        boolean sync();

        boolean cancelled();

        void cancel();

        Object raw();
    }
}
