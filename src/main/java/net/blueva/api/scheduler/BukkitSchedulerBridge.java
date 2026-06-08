package net.blueva.api.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

final class BukkitSchedulerBridge {
    private BukkitSchedulerBridge() {
    }

    static Scheduler.Task sync(Plugin plugin, Runnable runnable) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    static Scheduler.Task sync(Plugin plugin, Consumer<Scheduler.Task> consumer) {
        return submit(plugin, consumer, true, 0L, 0L, Mode.NOW);
    }

    static Scheduler.Task syncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks));
    }

    static Scheduler.Task syncLater(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayTicks) {
        return submit(plugin, consumer, true, delayTicks, 0L, Mode.LATER);
    }

    static Scheduler.Task syncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks));
    }

    static Scheduler.Task syncTimer(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayTicks, long periodTicks) {
        return submit(plugin, consumer, true, delayTicks, periodTicks, Mode.TIMER);
    }

    static Scheduler.Task async(Plugin plugin, Runnable runnable) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    static Scheduler.Task async(Plugin plugin, Consumer<Scheduler.Task> consumer) {
        return submit(plugin, consumer, false, 0L, 0L, Mode.NOW);
    }

    static Scheduler.Task asyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks));
    }

    static Scheduler.Task asyncLater(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayTicks) {
        return submit(plugin, consumer, false, delayTicks, 0L, Mode.LATER);
    }

    static Scheduler.Task asyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        return new BukkitTaskHandle(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks));
    }

    static Scheduler.Task asyncTimer(Plugin plugin, Consumer<Scheduler.Task> consumer, long delayTicks, long periodTicks) {
        return submit(plugin, consumer, false, delayTicks, periodTicks, Mode.TIMER);
    }

    private static Scheduler.Task submit(Plugin plugin, Consumer<Scheduler.Task> consumer, boolean sync, long delay, long period, Mode mode) {
        final BukkitTask[] ref = new BukkitTask[1];
        Runnable runnable = () -> consumer.accept(new BukkitTaskHandle(ref[0]));
        switch (mode) {
            case NOW:
                ref[0] = sync ? Bukkit.getScheduler().runTask(plugin, runnable) : Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
                break;
            case LATER:
                ref[0] = sync ? Bukkit.getScheduler().runTaskLater(plugin, runnable, delay) : Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
                break;
            case TIMER:
                ref[0] = sync ? Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period) : Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
                break;
        }
        return new BukkitTaskHandle(ref[0]);
    }

    private enum Mode {
        NOW,
        LATER,
        TIMER
    }
}
