package net.blueva.api.npc;

import net.blueva.api.npc.event.NpcClickEvent;
import net.blueva.api.npc.event.NpcDespawnEvent;
import net.blueva.api.npc.event.NpcSpawnEvent;
import net.blueva.api.npc.util.NpcAnimation;
import net.blueva.api.npc.util.NpcPose;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Internal implementation of {@link Npc}.
 */
final class NpcImpl implements Npc {

    private final UUID uuid;
    private final int entityId;
    private final NpcPlayer player;
    private final Set<UUID> viewers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private Location location;
    private String displayName = "";
    private Skin skin;
    private ItemStack[] equipment = new ItemStack[0];
    private NpcPose pose = NpcPose.STANDING;
    private Consumer<NpcClickEvent> clickAction;

    NpcImpl(Location location, UUID uuid, String internalName) {
        this.uuid = uuid;
        this.location = location.clone();
        this.player = NpcPlayer.create(uuid, internalName, location, NpcConnection.create());
        if (this.player == null || this.player.getHandle() == null) {
            throw new IllegalStateException("Could not create the fake player entity for the NPC.");
        }
        this.entityId = resolveEntityId();
        applySkin();
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    private int resolveEntityId() {
        Object handle = player.getHandle();
        if (handle == null) {
            return -1;
        }
        try {
            Method method = handle.getClass().getMethod("getId");
            method.setAccessible(true);
            return (Integer) method.invoke(handle);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public void setLocation(Location location) {
        this.location = location.clone();
    }

    @Override
    public void teleport(Location location) {
        setLocation(location);
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendTeleport(viewer, player.getHandle(), location);
                NpcPackets.sendHeadRotation(viewer, player.getHandle(), location.getYaw());
            }
        }
    }

    @Override
    public String getName() {
        return displayName;
    }

    @Override
    public Npc name(String name) {
        this.displayName = name == null ? "" : name;
        return this;
    }

    @Override
    public Npc skin(Skin skin) {
        this.skin = skin;
        applySkin();
        refreshAll();
        return this;
    }

    @Override
    public Npc equipment(ItemStack... equipment) {
        this.equipment = equipment == null ? new ItemStack[0] : equipment.clone();
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendEquipment(viewer, entityId, this.equipment);
            }
        }
        return this;
    }

    @Override
    public Npc pose(NpcPose pose) {
        this.pose = pose == null ? NpcPose.STANDING : pose;
        return this;
    }

    @Override
    public Npc lookAt(Player target) {
        if (target == null || !target.isOnline()) {
            return this;
        }
        Location eye = target.getEyeLocation();
        Location from = location.clone().setDirection(eye.toVector().subtract(location.toVector()));
        location.setYaw(from.getYaw());
        location.setPitch(from.getPitch());
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendHeadRotation(viewer, player.getHandle(), from.getYaw());
            }
        }
        return this;
    }

    @Override
    public Npc animate(NpcAnimation animation) {
        if (animation == null) {
            return this;
        }
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendAnimation(viewer, player.getHandle(), animation.getId());
            }
        }
        return this;
    }

    @Override
    public Npc onClick(Consumer<NpcClickEvent> action) {
        this.clickAction = action;
        return this;
    }

    @Override
    public void showTo(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (viewers.add(id)) {
            spawnTo(player);
            Bukkit.getPluginManager().callEvent(new NpcSpawnEvent(this, player));
        }
    }

    @Override
    public void hideFrom(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (viewers.remove(id)) {
            despawnFrom(player);
            Bukkit.getPluginManager().callEvent(new NpcDespawnEvent(this, player));
        }
    }

    @Override
    public boolean isShownTo(Player player) {
        return player != null && viewers.contains(player.getUniqueId());
    }

    @Override
    public void refresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (viewers.contains(player.getUniqueId())) {
            despawnFrom(player);
            spawnTo(player);
        }
    }

    @Override
    public void destroy() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                despawnFrom(viewer);
            }
        }
        viewers.clear();
        NpcRegistry.unregister(this);
    }

    void handleClick(NpcClickEvent event) {
        if (clickAction != null) {
            clickAction.accept(event);
        }
    }

    private void spawnTo(Player viewer) {
        Object handle = player.getHandle();
        if (handle == null) {
            return;
        }
        NpcPackets.sendInfoAdd(viewer, handle);
        NpcPackets.sendSpawn(viewer, handle);
        NpcPackets.sendMetadata(viewer, handle);
        NpcPackets.sendEquipment(viewer, getEntityId(), equipment);
        NpcPackets.sendHeadRotation(viewer, handle, location.getYaw());
    }

    private void despawnFrom(Player viewer) {
        NpcPackets.sendDestroy(viewer, getEntityId());
        NpcPackets.sendInfoRemove(viewer, uuid, player.getHandle());
    }

    private void applySkin() {
        Object handle = player.getHandle();
        if (handle == null) {
            return;
        }
        try {
            Method getProfile = handle.getClass().getMethod("getProfile");
            getProfile.setAccessible(true);
            Object profile = getProfile.invoke(handle);
            Skin.apply(profile, skin);
        } catch (Throwable ignored) {
        }
    }

    private void refreshAll() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                refresh(viewer);
            }
        }
    }
}
