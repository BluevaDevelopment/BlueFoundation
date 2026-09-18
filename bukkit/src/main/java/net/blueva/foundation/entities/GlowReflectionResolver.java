package net.blueva.foundation.entities;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves packet internals by type and signature instead of version-specific
 * member names.
 */
final class GlowReflectionResolver {

    private GlowReflectionResolver() {
    }

    static Class<?> firstClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(Arrays.toString(names));
    }

    static Class<?> innerClassWithConstructor(Class<?> owner, Class<?>... parameters)
            throws NoSuchMethodException {
        for (Class<?> inner : owner.getDeclaredClasses()) {
            if (constructorOrNull(inner, parameters) != null) {
                return inner;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " inner constructor "
                + Arrays.toString(parameters));
    }

    static Class<?> innerClassWithFactory(Class<?> owner, Class<?> firstParameter, Class<?> secondParameter)
            throws NoSuchMethodException {
        for (Class<?> inner : owner.getDeclaredClasses()) {
            for (Method method : allMethods(inner)) {
                Class<?>[] parameters = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers())
                        && method.getReturnType() == inner
                        && parameters.length == 2
                        && parameters[0] == firstParameter
                        && parameters[1] == secondParameter) {
                    return inner;
                }
            }
        }
        throw new NoSuchMethodException(owner.getName() + " data value factory");
    }

    static Constructor<?> constructor(Class<?> type, Class<?>... parameters)
            throws NoSuchMethodException {
        Constructor<?> constructor = constructorOrNull(type, parameters);
        if (constructor == null) {
            throw new NoSuchMethodException(type.getName() + Arrays.toString(parameters));
        }
        return constructor;
    }

    static Field fieldByType(Class<?> owner, Class<?> fieldType, boolean staticField)
            throws NoSuchFieldException {
        for (Field field : allFields(owner)) {
            if (Modifier.isStatic(field.getModifiers()) == staticField
                    && fieldType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException(owner.getName() + " field of type " + fieldType.getName());
    }

    static Field sharedFlagsField(Class<?> entityClass, Class<?> accessorClass)
            throws ReflectiveOperationException {
        List<Field> candidates = new ArrayList<Field>();
        for (Field field : allFields(entityClass)) {
            if (Modifier.isStatic(field.getModifiers()) && accessorClass.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                candidates.add(field);
            }
        }
        if (candidates.isEmpty()) {
            throw new NoSuchFieldException(entityClass.getName() + " shared flags accessor");
        }

        Field selected = null;
        int selectedId = Integer.MAX_VALUE;
        for (Field candidate : candidates) {
            Object accessor = candidate.get(null);
            int id = accessorId(accessor);
            if (id >= 0 && id < selectedId) {
                selected = candidate;
                selectedId = id;
            }
        }
        if (selected != null && selectedId == 0) {
            return selected;
        }
        candidates.sort(Comparator.comparing(Field::getName));
        return candidates.get(0);
    }

    static Field intField(Class<?> owner) throws NoSuchFieldException {
        for (Field field : allFields(owner)) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException(owner.getName() + " int field");
    }

    static Method noArgReturning(Class<?> owner, Class<?> returnType) throws NoSuchMethodException {
        for (Method method : allMethods(owner)) {
            if (method.getParameterTypes().length == 0 && method.getReturnType() == returnType) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " () -> " + returnType.getName());
    }

    static Method noArgAssignableReturn(Class<?> owner, Class<?> returnType)
            throws NoSuchMethodException {
        for (Method method : allMethods(owner)) {
            if (method.getParameterTypes().length == 0
                    && returnType.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " () -> " + returnType.getName());
    }

    static Method oneArgMethod(Class<?> owner, Class<?> parameterType, Class<?> preferredReturnType)
            throws NoSuchMethodException {
        Method fallback = null;
        for (Method method : allMethods(owner)) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != parameterType) {
                continue;
            }
            method.setAccessible(true);
            if (method.getReturnType() == preferredReturnType) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchMethodException(owner.getName() + "(" + parameterType.getName() + ")");
    }

    static Method staticFactory(Class<?> owner, Class<?> returnType, Class<?>... parameters)
            throws NoSuchMethodException {
        for (Method method : allMethods(owner)) {
            // An enum's valueOf(String) has the same shape as a lookup by serialized name.
            if (Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == returnType
                    && Arrays.equals(method.getParameterTypes(), parameters)
                    && !(owner.isEnum() && method.getName().equals("valueOf"))) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + " static factory "
                + Arrays.toString(parameters));
    }

    static Method packetSender(Class<?> listenerClass, Class<?> packetClass)
            throws NoSuchMethodException {
        for (Method method : allMethods(listenerClass)) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1
                    && parameters[0].isAssignableFrom(packetClass)
                    && method.getReturnType() == void.class) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(listenerClass.getName() + " packet sender");
    }

    static Method dataValueFactory(Class<?> valueClass, Class<?> accessorClass)
            throws NoSuchMethodException {
        return staticFactory(valueClass, valueClass, accessorClass, Object.class);
    }

    private static int accessorId(Object accessor) {
        if (accessor == null) {
            return -1;
        }
        try {
            return ((Integer) noArgReturning(accessor.getClass(), int.class).invoke(accessor)).intValue();
        } catch (Throwable ignored) {
        }
        try {
            Field field = intField(accessor.getClass());
            return field.getInt(accessor);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static Constructor<?> constructorOrNull(Class<?> type, Class<?>... parameters) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static List<Method> allMethods(Class<?> type) {
        List<Method> methods = new ArrayList<Method>();
        Class<?> current = type;
        while (current != null) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        return methods;
    }
}
