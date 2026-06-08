package net.blueva.api.scheduler;

final class FoliaTaskHandle implements Scheduler.Task {
    private final Object task;
    private final boolean sync;

    FoliaTaskHandle(Object task, boolean sync) {
        this.task = task;
        this.sync = sync;
    }

    @Override
    public int id() {
        return -1;
    }

    @Override
    public boolean sync() {
        return sync;
    }

    @Override
    public boolean cancelled() {
        if (task == null) {
            return false;
        }
        try {
            Object value = task.getClass().getMethod("isCancelled").invoke(task);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void cancel() {
        if (task == null) {
            return;
        }
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (Throwable ignored) {
            // Ignore cancellation failures from retired entity schedulers.
        }
    }

    @Override
    public Object raw() {
        return task;
    }
}
