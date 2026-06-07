package net.blueva.api.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Arrays;

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
        String normalized = name.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        try {
            return Sound.valueOf(normalized);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
