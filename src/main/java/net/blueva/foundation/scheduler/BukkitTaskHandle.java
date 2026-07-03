package net.blueva.foundation.scheduler;

import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;

final class BukkitTaskHandle implements Scheduler.Task {
    private final BukkitTask task;

    BukkitTaskHandle(BukkitTask task) {
        this.task = task;
    }

    @Override
    public int id() {
        return task == null ? -1 : task.getTaskId();
    }

    @Override
    public boolean sync() {
        return task != null && task.isSync();
    }

    @Override
    public boolean cancelled() {
        if (task == null) {
            return true;
        }
        try {
            Method method = task.getClass().getMethod("isCancelled");
            Object result = method.invoke(task);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public Object raw() {
        return task;
    }
}
