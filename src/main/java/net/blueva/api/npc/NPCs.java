package net.blueva.api.npc;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Public entry point for the BlueAPI NPC module.
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
        ensureInitialized();
        String internalName = "NPC-" + uuid.toString().substring(0, 8);
        NpcImpl npc = new NpcImpl(location, uuid, internalName);
        npc.name(name);
        NpcRegistry.register(npc);
        return npc;
    }

    private static void ensureInitialized() {
        if (manager == null) {
            throw new IllegalStateException("BlueAPI.NPCs is not initialized. Call BlueAPI.NPCs.init(plugin) first.");
        }
    }
}
