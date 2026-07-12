package net.blueva.foundation.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Public entry point for the BlueFoundation NPC module.
 */
public class NPCs {

    private static NpcManager manager;

    protected NPCs() {
    }

    /**
     * Initializes the NPC module.
     *
     * @param plugin the plugin using the module
     */
    public static synchronized void init(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (manager != null) {
            return;
        }
        manager = new NpcManager(plugin);
        manager.enable();
    }

    /**
     * Shuts down the NPC module, destroying every active NPC.
     */
    public static synchronized void close() {
        if (manager == null) {
            return;
        }
        manager.disable();
        manager = null;
    }

    /**
     * Creates a new NPC at the given location with a random UUID.
     *
     * @param location the spawn location
     * @return the created NPC
     */
    public static Npc create(Location location) {
        return create(location, UUID.randomUUID());
    }

    /**
     * Creates a new NPC at the given location with the specified UUID.
     *
     * @param location the spawn location
     * @param uuid     the UUID to use
     * @return the created NPC
     */
    public static Npc create(Location location, UUID uuid) {
        return create(location, uuid, "");
    }

    /**
     * Creates a new NPC at the given location with a random UUID and display name.
     *
     * @param location the spawn location
     * @param name     the display name
     * @return the created NPC
     */
    public static Npc create(Location location, String name) {
        return create(location, UUID.randomUUID(), name);
    }

    /**
     * Creates a new NPC at the given location.
     *
     * @param location the spawn location
     * @param uuid     the UUID to use
     * @param name     the display name
     * @return the created NPC
     */
    public static Npc create(Location location, UUID uuid, String name) {
        return create(location, uuid, name, EntityType.PLAYER);
    }

    /**
     * Creates a new NPC at the given location with a specific entity type.
     *
     * @param location   the spawn location
     * @param uuid       the UUID to use
     * @param name       the display name
     * @param entityType the Bukkit entity type to display (PLAYER for a human NPC)
     * @return the created NPC
     */
    public static Npc create(Location location, UUID uuid, String name, EntityType entityType) {
        ensureInitialized();
        String internalName = internalName(uuid, name);
        NpcImpl npc = new NpcImpl(location, uuid, internalName, entityType);
        npc.name(name);
        NpcRegistry.register(npc);
        return npc;
    }

    /**
     * Sends a vanilla entity animation packet (arm swing, damage, ...) to a
     * single viewer on any server version. The animation ids match the vanilla
     * protocol ({@code 0} = swing main arm, {@code 3} = swing off-hand).
     * <p>This is the low-level bridge used by {@code Players} to animate any
     * entity on legacy servers that lack the Bukkit swing methods.</p>
     *
     * @param viewer       the player that should see the animation
     * @param entityHandle the NMS handle of the entity to animate
     * @param animationId  the vanilla animation id
     */
    public static void sendEntityAnimation(Player viewer, Object entityHandle, int animationId) {
        NpcPackets.sendAnimation(viewer, entityHandle, animationId);
    }

    private static String internalName(UUID uuid, String name) {
        String normalized = name == null ? "" : name.replaceAll("[^A-Za-z0-9_]", "");
        if (normalized.isEmpty()) {
            return "NPC_" + uuid.toString().replace("-", "").substring(0, 8);
        }
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private static void ensureInitialized() {
        if (manager == null) {
            throw new IllegalStateException("BlueFoundation.NPCs is not initialized. Call BlueFoundation.NPCs.init(plugin) first.");
        }
    }
}
