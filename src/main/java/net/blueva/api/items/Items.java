package net.blueva.api.items;

import net.blueva.api.materials.Materials;
import net.blueva.api.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Base64;

/** Multi-version item helpers. Text input is MiniMessage-first. */
public class Items {

    protected Items() {
    }

    public static Builder builder(Material material) {
        return new Builder(material);
    }

    public static Builder builder(ItemStack item) {
        return new Builder(item);
    }

    public static Builder builder(String... materialNames) {
        return new Builder(Materials.require(materialNames));
    }

    public static ItemStack of(Material material) {
        return builder(material).build();
    }

    public static ItemStack of(Material material, int amount) {
        return builder(material).amount(amount).build();
    }

    public static ItemStack of(String... materialNames) {
        return builder(materialNames).build();
    }

    public static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static ItemStack amount(ItemStack item, int amount) {
        if (item != null) {
            item.setAmount(safeAmount(amount));
        }
        return item;
    }

    public static ItemStack durability(ItemStack item, short durability) {
        if (item != null) {
            item.setDurability(durability);
        }
        return item;
    }

    public static ItemStack name(ItemStack item, String name) {
        return name(item, isBlank(name) ? null : Text.component(name));
    }

    public static ItemStack name(ItemStack item, Component name) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setDisplayName(name == null ? null : Text.legacySection(name));
            }
        });
    }

    public static ItemStack lore(ItemStack item, String... lore) {
        return lore(item, lore == null ? null : Arrays.asList(lore));
    }

    public static ItemStack lore(ItemStack item, Collection<String> lore) {
        return setLegacyLore(item, legacyLines(lore));
    }

    public static ItemStack loreSplit(ItemStack item, String... lore) {
        return loreSplit(item, lore == null ? null : Arrays.asList(lore));
    }

    public static ItemStack loreSplit(ItemStack item, Collection<String> lore) {
        return loreSplit(item, lore, false);
    }

    public static ItemStack loreSplit(ItemStack item, Collection<String> lore, boolean keepEmptyLines) {
        return setLegacyLore(item, legacyLines(splitLines(lore, keepEmptyLines)));
    }

    public static ItemStack loreComponents(ItemStack item, Component... lore) {
        return loreComponents(item, lore == null ? null : Arrays.asList(lore));
    }

    public static ItemStack loreComponents(ItemStack item, Collection<? extends Component> lore) {
        return setLegacyLore(item, legacyComponentLines(lore));
    }

    private static ItemStack setLegacyLore(ItemStack item, final List<String> lore) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.setLore(lore);
            }
        });
    }

    public static ItemStack addLore(ItemStack item, String... lines) {
        return addLegacyLore(item, legacyLines(lines == null ? null : Arrays.asList(lines)));
    }

    public static ItemStack addLoreSplit(ItemStack item, String... lines) {
        return addLoreSplit(item, lines == null ? null : Arrays.asList(lines));
    }

    public static ItemStack addLoreSplit(ItemStack item, Collection<String> lines) {
        return addLoreSplit(item, lines, false);
    }

    public static ItemStack addLoreSplit(ItemStack item, Collection<String> lines, boolean keepEmptyLines) {
        return addLegacyLore(item, legacyLines(splitLines(lines, keepEmptyLines)));
    }

    public static ItemStack addLoreComponents(ItemStack item, Component... lines) {
        return addLoreComponents(item, lines == null ? null : Arrays.asList(lines));
    }

    public static ItemStack addLoreComponents(ItemStack item, Collection<? extends Component> lines) {
        return addLegacyLore(item, legacyComponentLines(lines));
    }

    private static ItemStack addLegacyLore(ItemStack item, final List<String> lines) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
                if (lines != null) {
                    lore.addAll(lines);
                }
                meta.setLore(lore.isEmpty() ? null : lore);
            }
        });
    }

    public static ItemStack enchant(ItemStack item, Enchantment enchantment, int level) {
        return enchant(item, enchantment, level, true);
    }

    public static ItemStack enchant(ItemStack item, Enchantment enchantment, int level, boolean unsafe) {
        if (item == null || enchantment == null || level <= 0) {
            return item;
        }
        if (unsafe) {
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            item.addEnchantment(enchantment, level);
        }
        return item;
    }

    public static ItemStack enchant(ItemStack item, String enchantmentName, int level) {
        return enchant(item, resolveEnchantment(enchantmentName), level, true);
    }

    public static ItemStack glow(ItemStack item) {
        enchant(item, Enchantment.DURABILITY, 1, true);
        return flags(item, "HIDE_ENCHANTS");
    }

    public static ItemStack flags(ItemStack item, String... flagNames) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (flagNames == null) {
                    return;
                }
                for (String flagName : flagNames) {
                    ItemFlag flag = resolveFlag(flagName);
                    if (flag != null) {
                        meta.addItemFlags(flag);
                    }
                }
            }
        });
    }

    public static ItemStack hideAllFlags(ItemStack item) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                meta.addItemFlags(ItemFlag.values());
            }
        });
    }

    public static ItemStack unbreakable(ItemStack item, boolean unbreakable) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                setUnbreakable(meta, unbreakable);
            }
        });
    }

    public static ItemStack customModelData(ItemStack item, final int data) {
        return customModelData(item, Integer.valueOf(data));
    }

    public static ItemStack clearCustomModelData(ItemStack item) {
        return customModelData(item, null);
    }

    public static ItemStack customModelData(ItemStack item, final Integer data) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                setCustomModelData(meta, data);
            }
        });
    }

    public static ItemStack skullOwner(ItemStack item, String owner) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (meta instanceof SkullMeta && !isBlank(owner)) {
                    ((SkullMeta) meta).setOwner(owner);
                }
            }
        });
    }

    public static ItemStack skullTexture(ItemStack item, final String texture) {
        return editMeta(item, new TypedMetaEditor<SkullMeta>() {
            @Override
            public Class<SkullMeta> type() {
                return SkullMeta.class;
            }

            @Override
            public void edit(SkullMeta meta) {
                applySkullTexture(meta, texture);
            }
        });
    }

    public static ItemStack skullValue(ItemStack item, final String value) {
        return skullValue(item, value, null);
    }

    public static ItemStack skullValue(ItemStack item, final String value, final OfflinePlayer currentPlayer) {
        return editMeta(item, new TypedMetaEditor<SkullMeta>() {
            @Override
            public Class<SkullMeta> type() {
                return SkullMeta.class;
            }

            @Override
            public void edit(SkullMeta meta) {
                applySkullValue(meta, value, currentPlayer);
            }
        });
    }

    public static ItemStack leatherColor(ItemStack item, Color color) {
        return editMeta(item, new MetaEditor() {
            @Override
            public void edit(ItemMeta meta) {
                if (meta instanceof LeatherArmorMeta && color != null) {
                    ((LeatherArmorMeta) meta).setColor(color);
                }
            }
        });
    }

    public static Enchantment resolveEnchantment(String name) {
        if (isBlank(name)) {
            return null;
        }

        String normalized = normalize(name);
        Enchantment enchantment = Enchantment.getByName(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        String legacy = modernToLegacyEnchantment(normalized);
        if (legacy != null) {
            enchantment = Enchantment.getByName(legacy);
            if (enchantment != null) {
                return enchantment;
            }
        }

        try {
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey");
            Object key = namespacedKey.getMethod("minecraft", String.class).invoke(null, normalized.toLowerCase());
            Method getByKey = Enchantment.class.getMethod("getByKey", namespacedKey);
            Object result = getByKey.invoke(null, key);
            return result instanceof Enchantment ? (Enchantment) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static ItemStack editMeta(ItemStack item, MetaEditor editor) {
        if (item == null || editor == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        editor.edit(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack editMeta(ItemStack item, TypedMetaEditor<? extends ItemMeta> editor) {
        if (item == null || editor == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !editor.type().isInstance(meta)) {
            return item;
        }
        editTypedMeta(meta, editor);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack pdcString(ItemStack item, String namespace, String key, String value) {
        return setPdc(item, namespace, key, "STRING", value);
    }

    public static String pdcString(ItemStack item, String namespace, String key) {
        Object value = getPdc(item, namespace, key, "STRING");
        return value instanceof String ? (String) value : null;
    }

    public static ItemStack pdcInt(ItemStack item, String namespace, String key, int value) {
        return setPdc(item, namespace, key, "INTEGER", Integer.valueOf(value));
    }

    public static Integer pdcInt(ItemStack item, String namespace, String key) {
        Object value = getPdc(item, namespace, key, "INTEGER");
        return value instanceof Integer ? (Integer) value : null;
    }

    public static ItemStack pdcBoolean(ItemStack item, String namespace, String key, boolean value) {
        return setPdc(item, namespace, key, "BYTE", Byte.valueOf((byte) (value ? 1 : 0)));
    }

    public static Boolean pdcBoolean(ItemStack item, String namespace, String key) {
        Object value = getPdc(item, namespace, key, "BYTE");
        return value instanceof Byte ? Boolean.valueOf(((Byte) value).byteValue() != 0) : null;
    }

    public static boolean pdcHas(ItemStack item, String namespace, String key, String typeName) {
        ItemMeta meta = item == null ? null : safeMeta(item);
        if (meta == null) {
            return false;
        }
        try {
            PdcContext context = pdcContext(meta, namespace, key, typeName);
            if (context == null) {
                return false;
            }
            try {
                Method has = context.container.getClass().getMethod("has", context.key.getClass(), context.type.getClass());
                Object result = has.invoke(context.container, context.key, context.type);
                return result instanceof Boolean && (Boolean) result;
            } catch (NoSuchMethodException ignored) {
                Method has = context.container.getClass().getMethod("has", context.key.getClass());
                Object result = has.invoke(context.container, context.key);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ItemStack pdcRemove(ItemStack item, String namespace, String key) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = safeMeta(item);
        if (meta == null) {
            return item;
        }
        try {
            PdcContext context = pdcContext(meta, namespace, key, "STRING");
            if (context == null) {
                return item;
            }
            Method remove = context.container.getClass().getMethod("remove", context.key.getClass());
            remove.invoke(context.container, context.key);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
        }
        return item;
    }

    private static List<String> legacyLines(Collection<String> lines) {
        if (lines == null) {
            return null;
        }
        List<String> legacy = new ArrayList<>();
        for (String line : lines) {
            legacy.add(Text.legacySection(line));
        }
        return legacy;
    }

    private static List<String> splitLines(Collection<String> lines, boolean keepEmptyLines) {
        if (lines == null) {
            return null;
        }
        List<String> split = new ArrayList<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String[] parts = line.split("\\n", -1);
            for (String part : parts) {
                if (keepEmptyLines || !part.isEmpty()) {
                    split.add(part);
                }
            }
        }
        return split;
    }

    private static List<String> legacyComponentLines(Collection<? extends Component> lines) {
        if (lines == null) {
            return null;
        }
        List<String> legacy = new ArrayList<>();
        for (Component line : lines) {
            legacy.add(Text.legacySection(line));
        }
        return legacy;
    }

    private static ItemFlag resolveFlag(String name) {
        if (isBlank(name)) {
            return null;
        }
        try {
            return ItemFlag.valueOf(normalize(name));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setUnbreakable(ItemMeta meta, boolean unbreakable) {
        try {
            Method method = meta.getClass().getMethod("setUnbreakable", boolean.class);
            method.invoke(meta, unbreakable);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Object spigot = meta.getClass().getMethod("spigot").invoke(meta);
            Method method = spigot.getClass().getMethod("setUnbreakable", boolean.class);
            method.invoke(spigot, unbreakable);
        } catch (Throwable ignored) {
        }
    }

    private static void setCustomModelData(ItemMeta meta, Integer data) {
        try {
            Method method = meta.getClass().getMethod("setCustomModelData", Integer.class);
            method.invoke(meta, data);
        } catch (Throwable ignored) {
        }
    }

    private static ItemStack setPdc(ItemStack item, String namespace, String key, String typeName, Object value) {
        if (item == null) {
            return null;
        }
        if (value == null) {
            return pdcRemove(item, namespace, key);
        }
        ItemMeta meta = safeMeta(item);
        if (meta == null) {
            return item;
        }
        try {
            PdcContext context = pdcContext(meta, namespace, key, typeName);
            if (context == null) {
                return item;
            }
            Method set = context.container.getClass().getMethod("set", context.key.getClass(), context.type.getClass(), Object.class);
            set.invoke(context.container, context.key, context.type, value);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {
        }
        return item;
    }

    private static Object getPdc(ItemStack item, String namespace, String key, String typeName) {
        ItemMeta meta = item == null ? null : safeMeta(item);
        if (meta == null) {
            return null;
        }
        try {
            PdcContext context = pdcContext(meta, namespace, key, typeName);
            if (context == null) {
                return null;
            }
            Method get = context.container.getClass().getMethod("get", context.key.getClass(), context.type.getClass());
            return get.invoke(context.container, context.key, context.type);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PdcContext pdcContext(ItemMeta meta, String namespace, String key, String typeName) {
        if (meta == null || isBlank(namespace) || isBlank(key) || isBlank(typeName)) {
            return null;
        }
        try {
            Method containerGetter = meta.getClass().getMethod("getPersistentDataContainer");
            Object container = containerGetter.invoke(meta);
            if (container == null) {
                return null;
            }
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Object namespacedKey = namespacedKey(namespacedKeyClass, namespace, key);
            if (namespacedKey == null) {
                return null;
            }
            Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object type = typeClass.getField(typeName.trim().toUpperCase()).get(null);
            return new PdcContext(container, namespacedKey, type);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object namespacedKey(Class<?> namespacedKeyClass, String namespace, String key) {
        try {
            if ("minecraft".equalsIgnoreCase(namespace)) {
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

    private static ItemMeta safeMeta(ItemStack item) {
        try {
            return item == null ? null : item.getItemMeta();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean applySkullValue(SkullMeta meta, String value, OfflinePlayer currentPlayer) {
        if (meta == null || isBlank(value)) {
            return false;
        }

        String trimmed = value.trim();
        OfflinePlayer resolvedCurrent = resolveCurrentPlayer(trimmed, currentPlayer);
        if (resolvedCurrent != null && applyOfflinePlayerProfile(meta, resolvedCurrent)) {
            return true;
        }

        UUID uuid = parseUuid(trimmed);
        String playerName = resolvePlayerName(uuid, trimmed);
        if (playerName != null && applyPlayerNameProfile(meta, playerName)) {
            return true;
        }

        if (isTextureValue(trimmed) && applySkullTexture(meta, trimmed)) {
            return true;
        }

        if (uuid == null && !isMinecraftPlayerName(trimmed)) {
            return false;
        }

        Object profile = resolvePlayerProfile(uuid, trimmed);
        if (hasSkinTexture(profile) && applyProfile(meta, profile)) {
            return true;
        }

        OfflinePlayer offline = resolveOfflinePlayer(uuid, trimmed);
        return offline != null && applyOfflinePlayerProfile(meta, offline);
    }

    private static OfflinePlayer resolveCurrentPlayer(String value, OfflinePlayer currentPlayer) {
        if (currentPlayer == null || isBlank(value)) {
            return null;
        }
        if ("{player}".equalsIgnoreCase(value)) {
            return currentPlayer;
        }
        String name = currentPlayer.getName();
        if (name != null && value.equalsIgnoreCase(name)) {
            return currentPlayer;
        }
        UUID uuid = parseUuid(value);
        return uuid != null && uuid.equals(currentPlayer.getUniqueId()) ? currentPlayer : null;
    }

    private static boolean applyPlayerNameProfile(SkullMeta meta, String playerName) {
        if (meta == null || !isMinecraftPlayerName(playerName)) {
            return false;
        }
        try {
            Method creator = Bukkit.class.getMethod("createPlayerProfile", String.class);
            Object profile = creator.invoke(null, playerName);
            if (applyProfile(meta, profile)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            meta.setOwner(playerName);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean applyOfflinePlayerProfile(SkullMeta meta, OfflinePlayer player) {
        if (meta == null || player == null) {
            return false;
        }

        Object profile = getPlayerProfile(player);
        if (hasSkinTexture(profile) && applyProfile(meta, profile)) {
            return true;
        }

        try {
            Method setter = meta.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
            setter.invoke(meta, player);
            return true;
        } catch (Throwable ignored) {
        }

        String name = player.getName();
        return !isBlank(name) && applyPlayerNameProfile(meta, name);
    }

    private static String resolvePlayerName(UUID uuid, String value) {
        if (uuid == null) {
            return isMinecraftPlayerName(value) ? value : null;
        }

        Player online = getOnlinePlayer(uuid);
        if (online != null && isMinecraftPlayerName(online.getName())) {
            return online.getName();
        }

        OfflinePlayer offline = getOfflinePlayer(uuid);
        String name = offline == null ? null : offline.getName();
        return isMinecraftPlayerName(name) ? name : null;
    }

    private static Object resolvePlayerProfile(UUID uuid, String value) {
        if (uuid != null) {
            Player online = getOnlinePlayer(uuid);
            Object onlineProfile = getPlayerProfile(online);
            if (hasSkinTexture(onlineProfile)) {
                return onlineProfile;
            }
            return getPlayerProfile(getOfflinePlayer(uuid));
        }

        if (!isMinecraftPlayerName(value)) {
            return null;
        }

        Player online = Bukkit.getPlayerExact(value);
        Object onlineProfile = getPlayerProfile(online);
        if (hasSkinTexture(onlineProfile)) {
            return onlineProfile;
        }
        return getPlayerProfile(Bukkit.getOfflinePlayer(value));
    }

    private static OfflinePlayer resolveOfflinePlayer(UUID uuid, String value) {
        if (uuid != null) {
            return getOfflinePlayer(uuid);
        }
        return isBlank(value) ? null : Bukkit.getOfflinePlayer(value);
    }

    private static Player getOnlinePlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            Method getter = Bukkit.class.getMethod("getPlayer", UUID.class);
            Object player = getter.invoke(null, uuid);
            return player instanceof Player ? (Player) player : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static OfflinePlayer getOfflinePlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            Method getter = Bukkit.class.getMethod("getOfflinePlayer", UUID.class);
            Object player = getter.invoke(null, uuid);
            return player instanceof OfflinePlayer ? (OfflinePlayer) player : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getPlayerProfile(OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Method getter = player.getClass().getMethod("getPlayerProfile");
            return getter.invoke(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasSkinTexture(Object profile) {
        if (profile == null) {
            return false;
        }
        try {
            Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            Object skin = textures.getClass().getMethod("getSkin").invoke(textures);
            return skin != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isTextureValue(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        if (!isBlank(textureUrl(trimmed))) {
            return true;
        }
        if (!isBlank(extractTextureUrl(trimmed))) {
            return true;
        }
        return trimmed.length() > 64 && trimmed.matches("^[A-Za-z0-9+/=]+$");
    }

    private static boolean applySkullTexture(SkullMeta meta, String value) {
        if (meta == null || isBlank(value)) {
            return false;
        }
        return applySkullTextureProfile(meta, value) || applySkullTextureProperty(meta, value);
    }

    private static boolean applySkullTextureProfile(SkullMeta meta, String value) {
        String textureUrl = textureUrl(value);
        if (isBlank(textureUrl)) {
            return false;
        }
        try {
            Method creator = Bukkit.class.getMethod("createPlayerProfile", UUID.class);
            Object profile = creator.invoke(null, UUID.randomUUID());
            Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            Method setSkin = textures.getClass().getMethod("setSkin", URL.class);
            setSkin.invoke(textures, new URL(textureUrl));
            profile.getClass().getMethod("setTextures", textures.getClass()).invoke(profile, textures);
            return applyProfile(meta, profile);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean applySkullTextureProperty(SkullMeta meta, String value) {
        String texture = textureProperty(value);
        if (isBlank(texture)) {
            return false;
        }
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "");
            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", texture);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);
            if (applyProfile(meta, profile)) {
                return true;
            }
            Field field = findField(meta.getClass(), "profile");
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            field.set(meta, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean applyProfile(SkullMeta meta, Object profile) {
        if (meta == null || profile == null) {
            return false;
        }
        try {
            Method setter = findCompatibleMethod(meta.getClass(), "setOwnerProfile", profile.getClass());
            if (setter == null) {
                setter = findCompatibleMethod(meta.getClass(), "setPlayerProfile", profile.getClass());
            }
            if (setter == null) {
                return false;
            }
            setter.setAccessible(true);
            setter.invoke(meta, profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findCompatibleMethod(Class<?> type, String name, Class<?> argumentType) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isAssignableFrom(argumentType)) {
                return method;
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String textureUrl(String value) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String decoded = extractTextureUrl(trimmed);
        if (!isBlank(decoded)) {
            return decoded;
        }
        if (trimmed.length() >= 32 && trimmed.matches("^[a-fA-F0-9]+$")) {
            return "http://textures.minecraft.net/texture/" + trimmed;
        }
        return null;
    }

    private static String textureProperty(String value) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        String decoded = extractTextureUrl(trimmed);
        if (!isBlank(decoded)) {
            return trimmed;
        }
        String url = textureUrl(trimmed);
        if (isBlank(url)) {
            return trimmed;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String extractTextureUrl(String value) {
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            String marker = "\"url\"";
            int markerIndex = json.indexOf(marker);
            if (markerIndex < 0) {
                return null;
            }
            int colonIndex = json.indexOf(':', markerIndex + marker.length());
            int firstQuote = json.indexOf('"', colonIndex + 1);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (colonIndex < 0 || firstQuote < 0 || secondQuote < 0) {
                return null;
            }
            return json.substring(firstQuote + 1, secondQuote);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isMinecraftPlayerName(String value) {
        return value != null && value.length() >= 3 && value.length() <= 16 && value.matches("^[a-zA-Z0-9_]+$");
    }

    private static UUID parseUuid(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String modernToLegacyEnchantment(String normalized) {
        if ("SHARPNESS".equals(normalized)) return "DAMAGE_ALL";
        if ("SMITE".equals(normalized)) return "DAMAGE_UNDEAD";
        if ("BANE_OF_ARTHROPODS".equals(normalized)) return "DAMAGE_ARTHROPODS";
        if ("EFFICIENCY".equals(normalized)) return "DIG_SPEED";
        if ("UNBREAKING".equals(normalized)) return "DURABILITY";
        if ("FORTUNE".equals(normalized)) return "LOOT_BONUS_BLOCKS";
        if ("POWER".equals(normalized)) return "ARROW_DAMAGE";
        if ("PUNCH".equals(normalized)) return "ARROW_KNOCKBACK";
        if ("FLAME".equals(normalized)) return "ARROW_FIRE";
        if ("INFINITY".equals(normalized)) return "ARROW_INFINITE";
        return null;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private static int safeAmount(int amount) {
        return Math.max(1, amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void editTypedMeta(ItemMeta meta, TypedMetaEditor editor) {
        editor.edit(meta);
    }

    public interface MetaEditor {
        void edit(ItemMeta meta);
    }

    public interface TypedMetaEditor<T extends ItemMeta> {
        Class<T> type();

        void edit(T meta);
    }

    private static final class PdcContext {
        private final Object container;
        private final Object key;
        private final Object type;

        private PdcContext(Object container, Object key, Object type) {
            this.container = container;
            this.key = key;
            this.type = type;
        }
    }

    public static class Builder {
        private final ItemStack item;

        protected Builder(Material material) {
            if (material == null) {
                throw new IllegalArgumentException("material cannot be null");
            }
            this.item = new ItemStack(material);
        }

        protected Builder(ItemStack item) {
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null");
            }
            this.item = item.clone();
        }

        public Builder amount(int amount) {
            Items.amount(item, amount);
            return this;
        }

        public Builder durability(short durability) {
            Items.durability(item, durability);
            return this;
        }

        public Builder data(int data) {
            Items.durability(item, (short) data);
            return this;
        }

        public Builder name(String name) {
            Items.name(item, name);
            return this;
        }

        public Builder name(Component name) {
            Items.name(item, name);
            return this;
        }

        public Builder lore(String... lore) {
            Items.lore(item, lore);
            return this;
        }

        public Builder lore(Collection<String> lore) {
            Items.lore(item, lore);
            return this;
        }

        public Builder loreSplit(String... lore) {
            Items.loreSplit(item, lore);
            return this;
        }

        public Builder loreSplit(Collection<String> lore) {
            Items.loreSplit(item, lore);
            return this;
        }

        public Builder loreSplit(Collection<String> lore, boolean keepEmptyLines) {
            Items.loreSplit(item, lore, keepEmptyLines);
            return this;
        }

        public Builder loreComponents(Component... lore) {
            Items.loreComponents(item, lore);
            return this;
        }

        public Builder loreComponents(Collection<? extends Component> lore) {
            Items.loreComponents(item, lore);
            return this;
        }

        public Builder addLore(String... lines) {
            Items.addLore(item, lines);
            return this;
        }

        public Builder addLoreSplit(String... lines) {
            Items.addLoreSplit(item, lines);
            return this;
        }

        public Builder addLoreSplit(Collection<String> lines) {
            Items.addLoreSplit(item, lines);
            return this;
        }

        public Builder addLoreSplit(Collection<String> lines, boolean keepEmptyLines) {
            Items.addLoreSplit(item, lines, keepEmptyLines);
            return this;
        }

        public Builder addLoreComponents(Component... lines) {
            Items.addLoreComponents(item, lines);
            return this;
        }

        public Builder addLoreComponents(Collection<? extends Component> lines) {
            Items.addLoreComponents(item, lines);
            return this;
        }

        public Builder enchant(Enchantment enchantment, int level) {
            Items.enchant(item, enchantment, level, true);
            return this;
        }

        public Builder enchant(String enchantment, int level) {
            Items.enchant(item, enchantment, level);
            return this;
        }

        public Builder glow() {
            Items.glow(item);
            return this;
        }

        public Builder flags(String... flagNames) {
            Items.flags(item, flagNames);
            return this;
        }

        public Builder hideAllFlags() {
            Items.hideAllFlags(item);
            return this;
        }

        public Builder unbreakable() {
            return unbreakable(true);
        }

        public Builder unbreakable(boolean unbreakable) {
            Items.unbreakable(item, unbreakable);
            return this;
        }

        public Builder customModelData(int data) {
            Items.customModelData(item, data);
            return this;
        }

        public Builder clearCustomModelData() {
            Items.clearCustomModelData(item);
            return this;
        }

        public Builder skullOwner(String owner) {
            Items.skullOwner(item, owner);
            return this;
        }

        public Builder skullTexture(String texture) {
            Items.skullTexture(item, texture);
            return this;
        }

        public Builder skullValue(String value) {
            Items.skullValue(item, value);
            return this;
        }

        public Builder leatherColor(Color color) {
            Items.leatherColor(item, color);
            return this;
        }

        public Builder pdcString(String namespace, String key, String value) {
            Items.pdcString(item, namespace, key, value);
            return this;
        }

        public Builder pdcInt(String namespace, String key, int value) {
            Items.pdcInt(item, namespace, key, value);
            return this;
        }

        public Builder pdcBoolean(String namespace, String key, boolean value) {
            Items.pdcBoolean(item, namespace, key, value);
            return this;
        }

        public Builder pdcRemove(String namespace, String key) {
            Items.pdcRemove(item, namespace, key);
            return this;
        }

        public Builder meta(MetaEditor editor) {
            Items.editMeta(item, editor);
            return this;
        }

        public Builder typedMeta(TypedMetaEditor<? extends ItemMeta> editor) {
            Items.editMeta(item, editor);
            return this;
        }

        public ItemStack build() {
            return item.clone();
        }
    }
}
