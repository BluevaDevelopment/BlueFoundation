package net.blueva.foundation.gamerules;

import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Multi-version game rule helpers.
 *
 * <p>1.13 replaced {@code World#setGameRuleValue(String, String)} with the typed
 * {@code World#setGameRule(GameRule, T)} API and deprecated the string form. Neither
 * {@code GameRule} nor the typed methods exist on 1.8, so everything here is resolved
 * reflectively and falls back to the string API on legacy servers.</p>
 */
public class GameRules {

    private static volatile Method setGameRule;      // World#setGameRule(GameRule, Object) (1.13+)
    private static volatile Method getGameRuleValue; // World#getGameRuleValue(GameRule) (1.13+)
    private static volatile Method getByName;        // GameRule#getByName(String) (1.13+)
    private static volatile Method setLegacyValue;   // World#setGameRuleValue(String, String) (all)
    private static volatile Method getLegacyValue;   // World#getGameRuleValue(String) (all)
    private static volatile boolean resolved;

    protected GameRules() {
    }

    /**
     * Sets a boolean game rule on any server version.
     *
     * @param world the world (may be {@code null})
     * @param name  the rule name in camelCase, e.g. {@code doDaylightCycle}
     * @param value the value to apply
     * @return {@code true} if the rule was applied
     */
    public static boolean set(World world, String name, boolean value) {
        if (world == null || isBlank(name)) {
            return false;
        }
        if (!resolved) {
            resolve();
        }

        Object rule = modernRule(name);
        if (rule != null && setGameRule != null) {
            try {
                Object result = setGameRule.invoke(world, rule, value);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true;
            } catch (Throwable ignored) {
                // fall through to the legacy string API
            }
        }

        if (setLegacyValue != null) {
            try {
                Object result = setLegacyValue.invoke(world, name, Boolean.toString(value));
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * Reads a boolean game rule on any server version.
     *
     * @param world    the world (may be {@code null})
     * @param name     the rule name in camelCase, e.g. {@code doDaylightCycle}
     * @param fallback returned when the rule is missing or unreadable
     * @return the rule value, or {@code fallback}
     */
    public static boolean getBoolean(World world, String name, boolean fallback) {
        if (world == null || isBlank(name)) {
            return fallback;
        }
        if (!resolved) {
            resolve();
        }

        Object rule = modernRule(name);
        if (rule != null && getGameRuleValue != null) {
            try {
                Object value = getGameRuleValue.invoke(world, rule);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (Throwable ignored) {
            }
        }

        if (getLegacyValue != null) {
            try {
                Object value = getLegacyValue.invoke(world, name);
                if (value instanceof String && !((String) value).isEmpty()) {
                    return Boolean.parseBoolean((String) value);
                }
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    /**
     * Returns whether the running server exposes the given rule at all.
     *
     * @param world the world to probe (may be {@code null})
     * @param name  the rule name in camelCase
     * @return {@code true} when the rule is known to this server
     */
    public static boolean isSupported(World world, String name) {
        if (isBlank(name)) {
            return false;
        }
        if (!resolved) {
            resolve();
        }
        if (modernRule(name) != null) {
            return true;
        }
        if (world == null || getLegacyValue == null) {
            return false;
        }
        try {
            Object value = getLegacyValue.invoke(world, name);
            return value instanceof String && !((String) value).isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolves a {@code GameRule} constant by name, accepting both the camelCase
     * Minecraft spelling ({@code doDaylightCycle}) and the Bukkit field spelling
     * ({@code DO_DAYLIGHT_CYCLE}).
     */
    private static Object modernRule(String name) {
        if (getByName == null) {
            return null;
        }
        for (String candidate : nameCandidates(name)) {
            try {
                Object rule = getByName.invoke(null, candidate);
                if (rule != null) {
                    return rule;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Builds the lookup spellings for a rule name, legacy ones first. Before 1.21.11
     * {@code GameRule#getByName} takes the camelCase id; from 1.21.11 it only takes the
     * lowercase registry key, and several rules were renamed.
     */
    static String[] nameCandidates(String name) {
        String trimmed = name.trim();
        Set<String> candidates = new LinkedHashSet<String>();
        candidates.add(trimmed);

        String camel = trimmed.indexOf('_') < 0 ? trimmed : toCamelCase(trimmed);
        candidates.add(camel);

        String modern = LegacyGameRuleNames.modernKey(camel);
        if (modern != null) {
            candidates.add(modern);
        }
        if (trimmed.indexOf('_') >= 0) {
            candidates.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return candidates.toArray(new String[0]);
    }

    private static String toCamelCase(String upperSnake) {
        String[] parts = upperSnake.toLowerCase(Locale.ROOT).split("_");
        StringBuilder camel = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (camel.length() == 0) {
                camel.append(parts[i]);
            } else {
                camel.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
        }
        return camel.toString();
    }

    private static void resolve() {
        synchronized (GameRules.class) {
            if (resolved) {
                return;
            }
            try {
                Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
                getByName = gameRuleClass.getMethod("getByName", String.class);
                setGameRule = World.class.getMethod("setGameRule", gameRuleClass, Object.class);
                getGameRuleValue = World.class.getMethod("getGameRuleValue", gameRuleClass);
            } catch (Throwable ignored) {
                // legacy server, the string API below is the only path
            }
            try {
                setLegacyValue = World.class.getMethod("setGameRuleValue", String.class, String.class);
            } catch (Throwable ignored) {
            }
            try {
                getLegacyValue = World.class.getMethod("getGameRuleValue", String.class);
            } catch (Throwable ignored) {
            }
            resolved = true;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
