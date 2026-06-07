package net.blueva.api.events.adapter;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/** Internal reflection utilities for registering runtime-only Bukkit events. */
public final class DynamicEventSupport {

    private DynamicEventSupport() {
    }

    public static boolean eventExists(String eventClassName) {
        return eventClass(eventClassName) != null;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Event> eventClass(String eventClassName) {
        try {
            Class<?> rawEventClass = Class.forName(eventClassName);
            return (Class<? extends Event>) rawEventClass.asSubclass(Event.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean register(Plugin plugin, String eventClassName, EventPriority priority, EventExecutor executor) {
        Class<? extends Event> eventClass = eventClass(eventClassName);
        if (eventClass == null) {
            return false;
        }

        Listener listener = new Listener() {
        };
        Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, plugin, false);
        return true;
    }

    public static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    public static Object invokeOptional(Object target, String methodName) {
        try {
            return invoke(target, methodName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isCancelled(Event event) {
        return event instanceof Cancellable && ((Cancellable) event).isCancelled();
    }

    public static void setCancelled(Event event, boolean cancelled) {
        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(cancelled);
        }
    }

    public static int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static boolean booleanValue(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }
}
