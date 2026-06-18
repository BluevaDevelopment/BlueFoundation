package net.blueva.api.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.blueva.api.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Skin texture data for an NPC.
 *
 * <p>Contains the base64 texture value and its signature. Skins can be built
 * from raw values, copied from an online player, or fetched asynchronously from
 * the Mojang session API.</p>
 */
public final class Skin {

    private static final ConcurrentHashMap<UUID, Skin> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();
    private static final String MOJANG_SESSION_URL =
            "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private final String value;
    private final String signature;

    private Skin(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    /**
     * Creates a skin from raw texture value and signature.
     *
     * @param value     base64 encoded texture value
     * @param signature texture signature
     * @return a new Skin instance
     */
    public static Skin of(String value, String signature) {
        return new Skin(value, signature);
    }

    /**
     * Extracts the skin from an online player.
     *
     * @param player the player to copy the skin from
     * @return the player's skin, or {@code null} if none is found
     */
    public static Skin fromPlayer(Player player) {
        if (player == null) {
            return null;
        }
        try {
            Object profile = player.getClass().getMethod("getProfile").invoke(player);
            if (profile == null) {
                return null;
            }
            Object textures = profile.getClass().getMethod("getProperties").invoke(profile);
            if (textures == null) {
                return null;
            }
            Object property = ((Iterable<?>) textures.getClass().getMethod("get", Object.class).invoke(textures, "textures")).iterator().next();
            String value = (String) property.getClass().getMethod("getValue").invoke(property);
            String signature = (String) property.getClass().getMethod("getSignature").invoke(property);
            return of(value, signature);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Fetches a skin asynchronously from the Mojang session server.
     *
     * @param plugin   the plugin requesting the skin
     * @param uuid     the UUID of the player whose skin is requested
     * @param callback invoked on the main thread with the skin, or {@code null} on failure
     */
    public static void fetch(Plugin plugin, UUID uuid, Consumer<Skin> callback) {
        if (plugin == null || uuid == null || callback == null) {
            return;
        }
        Skin cached = CACHE.get(uuid);
        if (cached != null) {
            Scheduler.sync(plugin, () -> callback.accept(cached));
            return;
        }
        Scheduler.async(plugin, () -> {
            Skin skin = fetchSync(uuid);
            Scheduler.sync(plugin, () -> callback.accept(skin));
        });
    }

    /**
     * Fetches a skin asynchronously by player name. The name is first resolved to
     * a UUID via the Mojang API, then the skin is fetched.
     *
     * @param plugin   the plugin requesting the skin
     * @param name     the player name
     * @param callback invoked on the main thread with the skin, or {@code null} on failure
     */
    public static void fetchByName(Plugin plugin, String name, Consumer<Skin> callback) {
        if (plugin == null || name == null || callback == null) {
            return;
        }
        UUID cachedUuid = NAME_TO_UUID_CACHE.get(name);
        if (cachedUuid != null) {
            fetch(plugin, cachedUuid, callback);
            return;
        }
        Scheduler.async(plugin, () -> {
            UUID uuid = resolveUuidSync(name);
            if (uuid != null) {
                NAME_TO_UUID_CACHE.put(name, uuid);
                Skin skin = fetchSync(uuid);
                Scheduler.sync(plugin, () -> callback.accept(skin));
            } else {
                Scheduler.sync(plugin, () -> callback.accept(null));
            }
        });
    }

    static void apply(Object profile, Skin skin) {
        if (profile == null || skin == null) {
            return;
        }
        try {
            Object properties = profile.getClass().getMethod("getProperties").invoke(profile);
            properties.getClass().getMethod("removeAll", Object.class).invoke(properties, "textures");
            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", new Property("textures", skin.value, skin.signature));
        } catch (Throwable ignored) {
        }
    }

    private static Skin fetchSync(UUID uuid) {
        Skin cached = CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }
        try {
            URL url = new URL(String.format(MOJANG_SESSION_URL, uuid.toString().replace("-", "")));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "BlueAPI-NPCs");
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String json = response.toString();
            String value = extractJsonField(json, "value");
            String signature = extractJsonField(json, "signature");
            if (value == null) {
                return null;
            }
            Skin skin = of(value, signature);
            CACHE.put(uuid, skin);
            return skin;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static UUID resolveUuidSync(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "BlueAPI-NPCs");
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String id = extractJsonField(response.toString(), "id");
            if (id == null) {
                return null;
            }
            return UUID.fromString(id.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extractJsonField(String json, String key) {
        String search = "\"" + key + "\"";
        int index = json.indexOf(search);
        if (index == -1) {
            return null;
        }
        int colon = json.indexOf(':', index + search.length());
        if (colon == -1) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start == -1) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end == -1) {
            return null;
        }
        return json.substring(start + 1, end);
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }
}
