package net.blueva.foundation.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class FoliaRuntime {
    private final boolean folia;
    private final boolean schedulers;
    private final Methods methods;

    private FoliaRuntime(boolean folia, boolean schedulers, Methods methods) {
        this.folia = folia;
        this.schedulers = schedulers;
        this.methods = methods;
    }

    static FoliaRuntime create() {
        boolean folia = SchedulerReflection.classExists("io.papermc.paper.threadedregions.RegionizedServer");
        try {
            Class<?> serverClass = Bukkit.getServer().getClass();
            Method getGlobal = serverClass.getMethod("getGlobalRegionScheduler");
            Method getRegion = serverClass.getMethod("getRegionScheduler");
            Method getAsync = serverClass.getMethod("getAsyncScheduler");

            if (SchedulerReflection.invokeRaw(Bukkit.getServer(), getGlobal) == null
                    || SchedulerReflection.invokeRaw(Bukkit.getServer(), getRegion) == null
                    || SchedulerReflection.invokeRaw(Bukkit.getServer(), getAsync) == null) {
                return empty(folia);
            }

            Class<?> globalClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class<?> regionClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> asyncClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");

            Methods methods = new Methods(
                    getGlobal,
                    getRegion,
                    getAsync,
                    globalClass.getMethod("run", Plugin.class, Consumer.class),
                    globalClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class),
                    globalClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class),
                    SchedulerReflection.find(globalClass, "cancelTasks", Plugin.class),
                    regionClass.getMethod("run", Plugin.class, Location.class, Consumer.class),
                    regionClass.getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class),
                    regionClass.getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class),
                    asyncClass.getMethod("runNow", Plugin.class, Consumer.class),
                    asyncClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class),
                    asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class),
                    SchedulerReflection.find(asyncClass, "cancelTasks", Plugin.class),
                    Entity.class.getMethod("getScheduler"),
                    entitySchedulerClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class),
                    entitySchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class),
                    entitySchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class),
                    SchedulerReflection.find(serverClass, "isOwnedByCurrentRegion", Location.class),
                    SchedulerReflection.find(serverClass, "isOwnedByCurrentRegion", Entity.class),
                    SchedulerReflection.find(serverClass, "isOwnedByCurrentRegion", World.class, int.class, int.class),
                    SchedulerReflection.find(serverClass, "isGlobalTickThread")
            );
            return new FoliaRuntime(folia, true, methods);
        } catch (Throwable ignored) {
            return empty(folia);
        }
    }

    private static FoliaRuntime empty(boolean folia) {
        return new FoliaRuntime(folia, false, Methods.empty());
    }

    boolean available() {
        return folia && schedulers;
    }

    boolean schedulersAvailable() {
        return schedulers;
    }

    Scheduler.Task global(Plugin plugin, Runnable runnable) {
        return invokeTask(true, global(), methods.globalRun, plugin, taskConsumer(task -> runnable.run()));
    }

    Scheduler.Task global(Plugin plugin, Consumer<Scheduler.Task> consumer) {
        return invokeTask(true, global(), methods.globalRun, plugin, taskConsumer(consumer, true));
    }

    CompletableFuture<Void> globalFuture(Plugin plugin, Consumer<Scheduler.Task> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        invokeTask(true, global(), methods.globalRun, plugin, taskConsumer(task -> {
            consumer.accept(task);
            future.complete(null);
        }, true));
        return future;
    }

    Scheduler.Task globalLater(Plugin plugin, Runnable runnable, long delay) {
        return invokeTask(true, global(), methods.globalDelayed, plugin, taskConsumer(task -> runnable.run()), delay);
    }

    Scheduler.Task globalLater(Plugin plugin, Consumer<Scheduler.Task> consumer, long delay) {
        return invokeTask(true, global(), methods.globalDelayed, plugin, taskConsumer(consumer, true), delay);
    }

    Scheduler.Task globalTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return invokeTask(true, global(), methods.globalTimer, plugin, taskConsumer(task -> runnable.run()), delay, period);
    }

    Scheduler.Task globalTimer(Plugin plugin, Consumer<Scheduler.Task> consumer, long delay, long period) {
        return invokeTask(true, global(), methods.globalTimer, plugin, taskConsumer(consumer, true), delay, period);
    }

    Scheduler.Task async(Plugin plugin, Runnable runnable) {
        return invokeTask(false, async(), methods.asyncRun, plugin, taskConsumer(task -> runnable.run()));
    }

    Scheduler.Task async(Plugin plugin, Consumer<Scheduler.Task> consumer) {
        return invokeTask(false, async(), methods.asyncRun, plugin, taskConsumer(consumer, false));
    }

    Scheduler.Task asyncLater(Plugin plugin, Runnable runnable, long delayMillis) {
        return invokeTask(false, async(), methods.asyncDelayed, plugin, taskConsumer(task -> runnable.run()), delayMillis, TimeUnit.MILLISECONDS);
    }

    Scheduler.Task asyncLater(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayMillis) {
        return invokeTask(false, async(), methods.asyncDelayed, plugin, taskConsumer(consumer, false), delayMillis, TimeUnit.MILLISECONDS);
    }

    Scheduler.Task asyncTimer(Plugin plugin, Runnable runnable, long delayMillis, long periodMillis) {
        return invokeTask(false, async(), methods.asyncTimer, plugin, taskConsumer(task -> runnable.run()), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    Scheduler.Task asyncTimer(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayMillis, long periodMillis) {
        return invokeTask(false, async(), methods.asyncTimer, plugin, taskConsumer(consumer, false), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
    }

    CompletableFuture<Void> locationFuture(Plugin plugin, Location location, Consumer<Scheduler.Task> consumer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        invokeTask(true, region(), methods.regionRun, plugin, location, taskConsumer(task -> {
            consumer.accept(task);
            future.complete(null);
        }, true));
        return future;
    }

    Scheduler.Task locationLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        return invokeTask(true, region(), methods.regionDelayed, plugin, location, taskConsumer(task -> runnable.run()), delay);
    }

    Scheduler.Task locationLater(Plugin plugin, Location location, Consumer<Scheduler.Task> consumer, long delay) {
        return invokeTask(true, region(), methods.regionDelayed, plugin, location, taskConsumer(consumer, true), delay);
    }

    Scheduler.Task locationTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        return invokeTask(true, region(), methods.regionTimer, plugin, location, taskConsumer(task -> runnable.run()), delay, period);
    }

    Scheduler.Task locationTimer(Plugin plugin, Location location, Consumer<Scheduler.Task> consumer, long delay, long period) {
        return invokeTask(true, region(), methods.regionTimer, plugin, location, taskConsumer(consumer, true), delay, period);
    }

    CompletableFuture<Boolean> entityFuture(Plugin plugin, Entity entity, Consumer<Scheduler.Task> consumer, Runnable fallback) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Object scheduler = entityScheduler(entity);
        if (scheduler == null) {
            runFallback(fallback);
            future.complete(Boolean.FALSE);
            return future;
        }
        Object result = SchedulerReflection.invokeRaw(scheduler, methods.entityRun, plugin, taskConsumer(task -> {
            consumer.accept(task);
            future.complete(Boolean.TRUE);
        }, true), fallbackRunnable(fallback, future));
        if (result == null) {
            runFallback(fallback);
            future.complete(Boolean.FALSE);
        }
        return future;
    }

    Scheduler.Task entityLater(Plugin plugin, Entity entity, Consumer<Scheduler.Task> consumer, Runnable fallback, long delay) {
        Object scheduler = entityScheduler(entity);
        if (scheduler == null) {
            runFallback(fallback);
            return new FoliaTaskHandle(null, true);
        }
        Object result = SchedulerReflection.invokeRaw(scheduler, methods.entityDelayed, plugin, taskConsumer(consumer, true), fallback, delay);
        if (result == null) {
            runFallback(fallback);
        }
        return new FoliaTaskHandle(result, true);
    }

    Scheduler.Task entityTimer(Plugin plugin, Entity entity, Consumer<Scheduler.Task> consumer, Runnable fallback, long delay, long period) {
        Object scheduler = entityScheduler(entity);
        if (scheduler == null) {
            runFallback(fallback);
            return new FoliaTaskHandle(null, true);
        }
        Object result = SchedulerReflection.invokeRaw(scheduler, methods.entityTimer, plugin, taskConsumer(consumer, true), fallback, delay, period);
        if (result == null) {
            runFallback(fallback);
        }
        return new FoliaTaskHandle(result, true);
    }

    void cancelTasks(Plugin plugin) {
        SchedulerReflection.invokeRaw(global(), methods.globalCancel, plugin);
        SchedulerReflection.invokeRaw(async(), methods.asyncCancel, plugin);
    }

    boolean isOwnedByCurrentRegion(Location location) {
        return SchedulerReflection.invokeBoolean(Bukkit.getServer(), methods.ownedLocation, location, Scheduler.isPrimaryThread());
    }

    boolean isOwnedByCurrentRegion(Entity entity) {
        return SchedulerReflection.invokeBoolean(Bukkit.getServer(), methods.ownedEntity, entity, Scheduler.isPrimaryThread());
    }

    boolean isOwnedByCurrentRegion(World world, int chunkX, int chunkZ) {
        return SchedulerReflection.invokeBoolean(Bukkit.getServer(), methods.ownedWorldChunk, world, chunkX, chunkZ, Scheduler.isPrimaryThread());
    }

    boolean isGlobalTickThread() {
        return SchedulerReflection.invokeBoolean(Bukkit.getServer(), methods.globalTickThread, Scheduler.isPrimaryThread());
    }

    private Object global() {
        return SchedulerReflection.invokeRaw(Bukkit.getServer(), methods.getGlobalRegionScheduler);
    }

    private Object region() {
        return SchedulerReflection.invokeRaw(Bukkit.getServer(), methods.getRegionScheduler);
    }

    private Object async() {
        return SchedulerReflection.invokeRaw(Bukkit.getServer(), methods.getAsyncScheduler);
    }

    private Object entityScheduler(Entity entity) {
        return SchedulerReflection.invokeRaw(entity, methods.entityGetScheduler);
    }

    private Scheduler.Task invokeTask(boolean sync, Object target, Method method, Object... args) {
        return new FoliaTaskHandle(SchedulerReflection.invokeRaw(target, method, args), sync);
    }

    private Consumer<Object> taskConsumer(Consumer<Scheduler.Task> consumer, boolean sync) {
        return raw -> consumer.accept(new FoliaTaskHandle(raw, sync));
    }

    private Consumer<Object> taskConsumer(Consumer<Scheduler.Task> consumer) {
        return taskConsumer(consumer, true);
    }

    private Runnable fallbackRunnable(Runnable fallback, CompletableFuture<Boolean> future) {
        if (fallback == null) {
            return null;
        }
        return () -> {
            fallback.run();
            future.complete(Boolean.FALSE);
        };
    }

    private static void runFallback(Runnable fallback) {
        if (fallback != null) {
            fallback.run();
        }
    }

    private static final class Methods {
        private final Method getGlobalRegionScheduler;
        private final Method getRegionScheduler;
        private final Method getAsyncScheduler;
        private final Method globalRun;
        private final Method globalDelayed;
        private final Method globalTimer;
        private final Method globalCancel;
        private final Method regionRun;
        private final Method regionDelayed;
        private final Method regionTimer;
        private final Method asyncRun;
        private final Method asyncDelayed;
        private final Method asyncTimer;
        private final Method asyncCancel;
        private final Method entityGetScheduler;
        private final Method entityRun;
        private final Method entityDelayed;
        private final Method entityTimer;
        private final Method ownedLocation;
        private final Method ownedEntity;
        private final Method ownedWorldChunk;
        private final Method globalTickThread;

        private Methods(Method getGlobalRegionScheduler, Method getRegionScheduler, Method getAsyncScheduler,
                        Method globalRun, Method globalDelayed, Method globalTimer, Method globalCancel,
                        Method regionRun, Method regionDelayed, Method regionTimer,
                        Method asyncRun, Method asyncDelayed, Method asyncTimer, Method asyncCancel,
                        Method entityGetScheduler, Method entityRun, Method entityDelayed, Method entityTimer,
                        Method ownedLocation, Method ownedEntity, Method ownedWorldChunk, Method globalTickThread) {
            this.getGlobalRegionScheduler = getGlobalRegionScheduler;
            this.getRegionScheduler = getRegionScheduler;
            this.getAsyncScheduler = getAsyncScheduler;
            this.globalRun = globalRun;
            this.globalDelayed = globalDelayed;
            this.globalTimer = globalTimer;
            this.globalCancel = globalCancel;
            this.regionRun = regionRun;
            this.regionDelayed = regionDelayed;
            this.regionTimer = regionTimer;
            this.asyncRun = asyncRun;
            this.asyncDelayed = asyncDelayed;
            this.asyncTimer = asyncTimer;
            this.asyncCancel = asyncCancel;
            this.entityGetScheduler = entityGetScheduler;
            this.entityRun = entityRun;
            this.entityDelayed = entityDelayed;
            this.entityTimer = entityTimer;
            this.ownedLocation = ownedLocation;
            this.ownedEntity = ownedEntity;
            this.ownedWorldChunk = ownedWorldChunk;
            this.globalTickThread = globalTickThread;
        }

        private static Methods empty() {
            return new Methods(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);
        }
    }
}
