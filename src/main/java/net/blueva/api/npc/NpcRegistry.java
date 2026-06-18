package net.blueva.api.npc;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal registry of active NPCs.
 */
final class NpcRegistry {

    private static final ConcurrentHashMap<UUID, NpcImpl> BY_UUID = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, NpcImpl> BY_ENTITY_ID = new ConcurrentHashMap<>();

    private NpcRegistry() {
    }

    static void register(NpcImpl npc) {
        BY_UUID.put(npc.getUuid(), npc);
        BY_ENTITY_ID.put(npc.getEntityId(), npc);
    }

    static void unregister(NpcImpl npc) {
        BY_UUID.remove(npc.getUuid());
        BY_ENTITY_ID.remove(npc.getEntityId());
    }

    static NpcImpl byUuid(UUID uuid) {
        return BY_UUID.get(uuid);
    }

    static NpcImpl byEntityId(int entityId) {
        return BY_ENTITY_ID.get(entityId);
    }

    static Collection<NpcImpl> all() {
        return Collections.unmodifiableCollection(BY_UUID.values());
    }

    static void clear() {
        BY_UUID.clear();
        BY_ENTITY_ID.clear();
    }
}
