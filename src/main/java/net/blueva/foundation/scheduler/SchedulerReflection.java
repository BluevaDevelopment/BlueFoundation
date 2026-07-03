package net.blueva.foundation.scheduler;

import java.lang.reflect.Method;

final class SchedulerReflection {
    private SchedulerReflection() {
    }

    static Object invokeRaw(Object target, Method method, Object... args) {
        if (target == null || method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean invokeBoolean(Object target, Method method, Object arg, boolean fallback) {
        Object value = invokeRaw(target, method, arg);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    static boolean invokeBoolean(Object target, Method method, Object arg1, Object arg2, Object arg3, boolean fallback) {
        Object value = invokeRaw(target, method, arg1, arg2, arg3);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    static boolean invokeBoolean(Object target, Method method, boolean fallback) {
        Object value = invokeRaw(target, method);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    static Method find(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
