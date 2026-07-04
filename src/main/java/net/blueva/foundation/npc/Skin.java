package net.blueva.foundation.npc;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.mojang.authlib.properties.Property;
import net.blueva.foundation.scheduler.Scheduler;
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

    /**
     * Applies the skin to the given GameProfile.
     *
     * <p>On modern authlib versions (1.21.9+) GameProfile is an immutable record,
     * so this method returns a new GameProfile with the skin properties. On older
     * versions it mutates the profile in place and returns it.</p>
     *
     * @param profile the GameProfile to apply the skin to
     * @param skin    the skin to apply
     * @return the GameProfile with the skin applied (may be a new instance)
     */
    static Object apply(Object profile, Skin skin) {
        if (profile == null || skin == null) {
            return profile;
        }
        try {
            Object propertyMap = createPropertyMap(skin);
            Object newProfile = tryCreateProfileWithProperties(profile, propertyMap);
            if (newProfile != null) {
                log("Applied skin via GameProfile constructor (value length: " + skin.value.length() + ")");
                return newProfile;
            }

            // Legacy mutable GameProfile: modify in place.
            Object properties = getProfileProperties(profile);
            if (properties == null) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Profile has no properties");
                return profile;
            }
            removeTexture(properties);
            putTexture(properties, skin);
            log("Applied skin to mutable profile (value length: " + skin.value.length() + ")");
            return profile;
        } catch (Throwable e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
            Bukkit.getLogger().log(java.util.logging.Level.WARNING,
                    "[BlueFoundation-NPC] Failed to apply skin to profile: " + e.getClass().getName()
                            + " -> " + (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : "null"), cause);
            return profile;
        }
    }

    private static Object createPropertyMap(Skin skin) throws Exception {
        Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");
        Property property = new Property("textures", skin.value, skin.signature);

        // Modern PropertyMap has a constructor that accepts a Multimap.
        Class<?> multimapClass = Class.forName("com.google.common.collect.Multimap");
        try {
            Constructor<?> constructor = propertyMapClass.getDeclaredConstructor(multimapClass);
            Object multimap = createSingleEntryMultimap("textures", property);
            return constructor.newInstance(multimap);
        } catch (NoSuchMethodException ignored) {
        }

        // Fallback for older authlib: create empty PropertyMap and put the property.
        Object propertyMap = propertyMapClass.getDeclaredConstructor().newInstance();
        putTexture(propertyMap, skin);
        return propertyMap;
    }

    private static Object createSingleEntryMultimap(String key, Property property) throws Exception {
        Class<?> multimapsClass = Class.forName("com.google.common.collect.Multimaps");
        Method forMap = multimapsClass.getMethod("forMap", java.util.Map.class);
        java.util.Map<String, Property> map = java.util.Collections.singletonMap(key, property);
        return forMap.invoke(null, map);
    }

    private static Object tryCreateProfileWithProperties(Object profile, Object propertyMap) {
        try {
            Class<?> profileClass = profile.getClass();
            Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");
            UUID id = getProfileId(profile);
            String name = getProfileName(profile);
            if (id == null || name == null) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Could not read GameProfile id/name");
                return null;
            }

            for (Constructor<?> constructor : profileClass.getDeclaredConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length != 3) {
                    continue;
                }
                if (!params[0].isAssignableFrom(UUID.class)
                        || !params[1].isAssignableFrom(String.class)
                        || !params[2].isAssignableFrom(propertyMapClass)) {
                    continue;
                }
                constructor.setAccessible(true);
                return constructor.newInstance(id, name, propertyMap);
            }

            Bukkit.getLogger().warning("[BlueFoundation-NPC] No GameProfile constructor found with (UUID, String, PropertyMap). Available constructors:");
            for (Constructor<?> constructor : profileClass.getDeclaredConstructors()) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC]   " + constructor);
            }
            return null;
        } catch (Throwable e) {
            Bukkit.getLogger().log(java.util.logging.Level.WARNING,
                    "[BlueFoundation-NPC] GameProfile constructor with PropertyMap failed: " + e.getClass().getName() + " " + e.getMessage(), e);
            return null;
        }
    }

    private static UUID getProfileId(Object profile) {
        try {
            return (UUID) profile.getClass().getMethod("getId").invoke(profile);
        } catch (Throwable ignored) {
        }
        try {
            return (UUID) profile.getClass().getMethod("id").invoke(profile);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String getProfileName(Object profile) {
        try {
            return (String) profile.getClass().getMethod("getName").invoke(profile);
        } catch (Throwable ignored) {
        }
        try {
            return (String) profile.getClass().getMethod("name").invoke(profile);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getProfileProperties(Object profile) throws Exception {
        try {
            Method method = profile.getClass().getMethod("getProperties");
            return method.invoke(profile);
        } catch (Throwable ignored) {
        }
        try {
            Method method = profile.getClass().getMethod("properties");
            return method.invoke(profile);
        } catch (Throwable ignored) {
        }
        Field field = findField(profile.getClass(), "properties");
        if (field != null) {
            field.setAccessible(true);
            return field.get(profile);
        }
        return null;
    }

    private static void removeTexture(Object properties) {
        try {
            Method method = properties.getClass().getMethod("removeAll", Object.class);
            method.invoke(properties, "textures");
            return;
        } catch (Throwable ignored) {
        }
        try {
            Method method = properties.getClass().getMethod("remove", Object.class);
            method.invoke(properties, "textures");
        } catch (Throwable ignored) {
        }
    }

    private static void putTexture(Object properties, Skin skin) throws Exception {
        Method method = properties.getClass().getMethod("put", Object.class, Object.class);
        method.invoke(properties, "textures", new Property("textures", skin.value, skin.signature));
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
            connection.setRequestProperty("User-Agent", "BlueFoundation-NPCs");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            if (code != 200) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Skin session endpoint returned " + code + " for UUID: " + uuid);
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
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Skin session response missing 'value' for UUID: " + uuid);
                return null;
            }
            Skin skin = of(value, signature);
            CACHE.put(uuid, skin);
            log("Fetched skin for UUID: " + uuid + " (signature present: " + (signature != null) + ")");
            return skin;
        } catch (Throwable e) {
            Bukkit.getLogger().log(java.util.logging.Level.WARNING,
                    "[BlueFoundation-NPC] Failed to fetch skin for UUID: " + uuid + " - " + e.getMessage(), e);
            return null;
        }
    }

    private static UUID resolveUuidSync(String name) {
        UUID uuid = resolveUuidFromEndpoint(name,
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name);
        if (uuid == null) {
            uuid = resolveUuidFromEndpoint(name,
                    "https://api.mojang.com/users/profiles/minecraft/" + name);
        }
        if (uuid == null) {
            Bukkit.getLogger().warning("[BlueFoundation-NPC] Could not resolve UUID for skin name: " + name);
        }
        return uuid;
    }

    private static UUID resolveUuidFromEndpoint(String name, String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "BlueFoundation-NPCs");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            if (code != 200) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Skin UUID endpoint returned " + code + " for name: " + name);
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
            String id = extractJsonField(json, "id");
            if (id == null) {
                Bukkit.getLogger().warning("[BlueFoundation-NPC] Skin UUID response missing 'id' for name: " + name + " -> " + json);
                return null;
            }
            return UUID.fromString(id.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        } catch (Throwable e) {
            Bukkit.getLogger().log(java.util.logging.Level.WARNING,
                    "[BlueFoundation-NPC] Failed to resolve UUID for skin name: " + name + " - " + e.getMessage(), e);
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

    private static void log(String message) {
        try {
            Bukkit.getLogger().log(java.util.logging.Level.FINE, "[BlueFoundation-NPC] " + message);
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
