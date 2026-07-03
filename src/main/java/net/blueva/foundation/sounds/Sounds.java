package net.blueva.foundation.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Multi-version sound helpers. */
public class Sounds {

    protected Sounds() {
    }

    public static Sound match(String... names) {
        if (names == null) {
            return null;
        }
        for (String name : names) {
            Sound sound = matchOne(name);
            if (sound != null) {
                return sound;
            }
        }
        return null;
    }

    public static Sound require(String... names) {
        Sound sound = match(names);
        if (sound == null) {
            throw new IllegalArgumentException("Unsupported sound names: " + Arrays.toString(names));
        }
        return sound;
    }

    public static boolean isSupported(String... names) {
        return match(names) != null;
    }

    public static boolean play(Player player, float volume, float pitch, String... names) {
        Sound sound = match(names);
        if (player == null || sound == null) {
            return false;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
        return true;
    }

    private static Sound matchOne(String name) {
        if (isBlank(name)) {
            return null;
        }

        Sound registrySound = matchRegistry(name);
        if (registrySound != null) {
            return registrySound;
        }

        String normalized = legacyName(name);
        try {
            return Sound.valueOf(normalized);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Sound matchRegistry(String name) {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Field soundsField = registryClass.getField("SOUNDS");
            Object soundsRegistry = soundsField.get(null);
            Method get = soundsRegistry.getClass().getMethod("get", namespacedKeyClass);

            for (KeyCandidate candidate : registryCandidates(name)) {
                Object key = namespacedKey(namespacedKeyClass, candidate.namespace, candidate.key);
                if (key == null) {
                    continue;
                }

                Object sound = get.invoke(soundsRegistry, key);
                if (sound instanceof Sound) {
                    return (Sound) sound;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Set<KeyCandidate> registryCandidates(String name) {
        String normalized = name.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        Set<KeyCandidate> candidates = new LinkedHashSet<>();

        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            String namespace = normalized.substring(0, namespaceSeparator);
            String key = normalized.substring(namespaceSeparator + 1);
            addRegistryKeyCandidates(candidates, namespace, key);
            return candidates;
        }

        addRegistryKeyCandidates(candidates, "minecraft", normalized);
        return candidates;
    }

    private static void addRegistryKeyCandidates(Set<KeyCandidate> candidates, String namespace, String key) {
        if (isBlank(namespace) || isBlank(key)) {
            return;
        }

        candidates.add(new KeyCandidate(namespace, key));
        if (key.indexOf('_') >= 0) {
            for (String variant : underscoreDotVariants(key)) {
                candidates.add(new KeyCandidate(namespace, variant));
            }
        }
    }

    private static Set<String> underscoreDotVariants(String key) {
        Set<String> variants = new LinkedHashSet<>();
        int underscores = 0;
        for (int i = 0; i < key.length(); i++) {
            if (key.charAt(i) == '_') {
                underscores++;
            }
        }

        if (underscores == 0) {
            variants.add(key);
            return variants;
        }
        if (underscores > 8) {
            variants.add(key.replace('_', '.'));
            return variants;
        }

        char[] chars = key.toCharArray();
        int combinations = 1 << underscores;
        for (int mask = 1; mask < combinations; mask++) {
            char[] copy = chars.clone();
            int underscoreIndex = 0;
            for (int i = 0; i < copy.length; i++) {
                if (copy[i] == '_') {
                    if ((mask & (1 << underscoreIndex)) != 0) {
                        copy[i] = '.';
                    }
                    underscoreIndex++;
                }
            }
            variants.add(new String(copy));
        }
        return variants;
    }

    private static Object namespacedKey(Class<?> namespacedKeyClass, String namespace, String key) {
        try {
            if ("minecraft".equals(namespace)) {
                try {
                    Method minecraft = namespacedKeyClass.getMethod("minecraft", String.class);
                    return minecraft.invoke(null, key);
                } catch (Throwable ignored) {
                }
            }

            Constructor<?> constructor = namespacedKeyClass.getConstructor(String.class, String.class);
            return constructor.newInstance(namespace, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String legacyName(String name) {
        String normalized = name.trim();
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('.', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class KeyCandidate {
        private final String namespace;
        private final String key;

        private KeyCandidate(String namespace, String key) {
            this.namespace = namespace;
            this.key = key;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof KeyCandidate)) {
                return false;
            }
            KeyCandidate that = (KeyCandidate) other;
            return namespace.equals(that.namespace) && key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return 31 * namespace.hashCode() + key.hashCode();
        }
    }
}
