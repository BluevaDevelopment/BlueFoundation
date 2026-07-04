package net.blueva.foundation.npc;

import net.blueva.foundation.npc.event.NpcClickEvent;
import net.blueva.foundation.npc.event.NpcDespawnEvent;
import net.blueva.foundation.npc.event.NpcSpawnEvent;
import net.blueva.foundation.npc.util.NpcAnimation;
import net.blueva.foundation.npc.util.NpcPose;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Internal implementation of {@link Npc}.
 */
final class NpcImpl implements Npc {

    private final UUID uuid;
    private int entityId;
    private NpcPlayer player;
    private Object entityHandle;
    private EntityType entityType = EntityType.PLAYER;
    private final Set<UUID> viewers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private Location location;
    private String displayName = "";
    private Skin skin;
    private ItemStack[] equipment = new ItemStack[0];
    private NpcPose pose = NpcPose.STANDING;
    private Consumer<NpcClickEvent> clickAction;

    private ChatColor glowColor = null;
    private double scale = 1.0D;
    private boolean nameVisible = false;
    private boolean listed = false;
    private boolean lookAtClosestPlayer = false;
    private double lookAtRange = 10.0D;

    NpcImpl(Location location, UUID uuid, String internalName, EntityType entityType) {
        this.uuid = uuid;
        this.location = location.clone();
        this.entityType = entityType == null ? EntityType.PLAYER : entityType;
        this.player = NpcPlayer.create(uuid, internalName, location, NpcConnection.create(), null);
        if (this.player == null || this.player.getHandle() == null) {
            throw new IllegalStateException("Could not create the fake player entity for the NPC.");
        }
        if (this.entityType == EntityType.PLAYER) {
            this.entityHandle = this.player.getHandle();
        } else {
            this.entityHandle = NpcEntityFactory.create(location, this.entityType);
            if (this.entityHandle == null) {
                throw new IllegalStateException("Could not create the fake entity for the NPC of type " + this.entityType);
            }
        }
        this.entityId = resolveEntityId();
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
        return resolveEntityId(this.entityHandle);
    }

    private static int resolveEntityId(Object handle) {
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
                NpcPackets.sendTeleport(viewer, entityHandle, location);
                NpcPackets.sendHeadRotation(viewer, entityHandle, location.getYaw());
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
        if (entityType != EntityType.PLAYER) {
            return this;
        }
        int oldEntityId = this.entityId;
        Set<UUID> currentViewers = new HashSet<>(viewers);
        for (UUID viewerId : currentViewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                despawnFrom(viewer, oldEntityId);
            }
        }
        recreatePlayer();
        for (UUID viewerId : currentViewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && viewers.contains(viewerId)) {
                spawnTo(viewer);
            }
        }
        return this;
    }

    @Override
    public Npc equipment(ItemStack... equipment) {
        if (!canWearEquipment()) {
            return this;
        }
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
        if (!canUsePose()) {
            return this;
        }
        this.pose = pose == null ? NpcPose.STANDING : pose;
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendPose(viewer, entityHandle, this.pose);
            }
        }
        return this;
    }

    @Override
    public NpcPose getPose() {
        return pose;
    }

    @Override
    public Npc lookAt(Player target) {
        if (target == null || !target.isOnline()) {
            return this;
        }
        Location targetEye = target.getEyeLocation();
        Location npcEye = location.clone().add(0.0D, 1.6D * scale, 0.0D);
        Location facing = npcEye.clone().setDirection(targetEye.toVector().subtract(npcEye.toVector()));
        location.setYaw(facing.getYaw());
        location.setPitch(facing.getPitch());
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendTeleport(viewer, entityHandle, location);
                NpcPackets.sendHeadRotation(viewer, entityHandle, facing.getYaw());
            }
        }
        return this;
    }

    @Override
    public Npc lookAtClosestPlayer(boolean enabled) {
        return lookAtClosestPlayer(enabled, 10.0D);
    }

    @Override
    public Npc lookAtClosestPlayer(boolean enabled, double range) {
        this.lookAtClosestPlayer = enabled;
        this.lookAtRange = Math.max(1.0D, range);
        return this;
    }

    @Override
    public boolean isLookingAtClosestPlayer() {
        return lookAtClosestPlayer;
    }

    @Override
    public Npc glow(ChatColor color) {
        this.glowColor = color;
        updateTeamForAll();
        if (entityHandle != null) {
            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    NpcPackets.sendGlow(viewer, entityHandle, color != null);
                }
            }
        }
        return this;
    }

    @Override
    public Npc glow(boolean glow) {
        return glow(glow ? ChatColor.WHITE : null);
    }

    @Override
    public ChatColor getGlowColor() {
        return glowColor;
    }

    @Override
    public Npc scale(double scale) {
        this.scale = scale;
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendScale(viewer, entityHandle, this.scale);
            }
        }
        return this;
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public Npc nameVisible(boolean visible) {
        this.nameVisible = visible;
        updateTeamForAll();
        return this;
    }

    @Override
    public boolean isNameVisible() {
        return nameVisible;
    }

    @Override
    public Npc listed(boolean listed) {
        this.listed = listed;
        Object handle = player.getHandle();
        if (handle != null) {
            try {
                java.lang.reflect.Field field = handle.getClass().getDeclaredField("listed");
                field.setAccessible(true);
                field.set(handle, listed);
            } catch (Throwable ignored) {
            }
        }
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendInfoRemove(viewer, uuid, player.getHandle());
                NpcPackets.sendInfoAdd(viewer, player.getHandle());
            }
        }
        return this;
    }

    @Override
    public boolean isListed() {
        return listed;
    }

    @Override
    public Npc entityType(EntityType type) {
        this.entityType = type == null ? EntityType.PLAYER : type;
        return this;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }

    @Override
    public double getHeight() {
        if (entityHandle == null) {
            return 1.8D;
        }
        try {
            Method method = entityHandle.getClass().getMethod("getBbHeight");
            method.setAccessible(true);
            Object value = method.invoke(entityHandle);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (Throwable ignored) {
        }
        try {
            Method method = entityHandle.getClass().getMethod("getBoundingBox");
            method.setAccessible(true);
            Object aabb = method.invoke(entityHandle);
            if (aabb != null) {
                Method ySize = aabb.getClass().getMethod("getYsize");
                ySize.setAccessible(true);
                Object value = ySize.invoke(aabb);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }
        } catch (Throwable ignored) {
        }
        return 1.8D;
    }

    @Override
    public Npc animate(NpcAnimation animation) {
        if (animation == null) {
            return this;
        }
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                NpcPackets.sendAnimation(viewer, entityHandle, animation.getId());
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

    void updateLookAt() {
        if (!lookAtClosestPlayer) {
            return;
        }
        Player closest = null;
        double closestDistance = lookAtRange * lookAtRange;
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            double distSq = location.distanceSquared(viewer.getLocation());
            if (distSq < closestDistance) {
                closestDistance = distSq;
                closest = viewer;
            }
        }
        if (closest != null) {
            lookAt(closest);
        }
    }

    private void spawnTo(Player viewer) {
        Object handle = entityHandle;
        if (handle == null) {
            return;
        }
        if (entityType == EntityType.PLAYER) {
            NpcPackets.sendInfoAdd(viewer, player.getHandle());
            NpcPackets.sendSpawn(viewer, handle);
            // Spigot (and some Paper builds) always list the player on tab when adding
            // the info entry. Send a delayed info remove so the entity is already spawned
            // before the tab entry is cleared, mirroring how NpcApi hides NPCs from tab.
            Bukkit.getScheduler().runTaskLater(NpcManager.getPlugin(), () -> {
                if (viewer.isOnline()) {
                    NpcPackets.sendInfoRemove(viewer, uuid, player.getHandle());
                }
            }, 2L);
        } else {
            if (!net.blueva.foundation.version.Version.isAtLeast(1, 20)) {
                return;
            }
            NpcPackets.sendAddEntity(viewer, handle);
        }
        NpcPackets.sendMetadata(viewer, handle);
        if (canUsePose()) {
            NpcPackets.sendPose(viewer, handle, pose);
        }
        if (canWearEquipment()) {
            NpcPackets.sendEquipment(viewer, getEntityId(), equipment);
        }
        NpcPackets.sendHeadRotation(viewer, handle, location.getYaw());
        NpcPackets.sendScale(viewer, handle, scale);
        NpcPackets.sendGlow(viewer, handle, glowColor != null);
        updateTeamForAll();
    }

    private void despawnFrom(Player viewer) {
        despawnFrom(viewer, getEntityId());
    }

    private void despawnFrom(Player viewer, int entityId) {
        NpcPackets.sendDestroy(viewer, entityId);
        if (entityType == EntityType.PLAYER) {
            NpcPackets.sendInfoRemove(viewer, uuid, player.getHandle());
        }
    }

    private void recreatePlayer() {
        if (this.player == null) {
            return;
        }
        int oldEntityId = this.entityId;
        this.player = this.player.recreate(location, NpcConnection.create(), skin);
        if (this.player == null || this.player.getHandle() == null) {
            throw new IllegalStateException("Could not recreate the fake player entity for the NPC.");
        }
        this.entityId = resolveEntityId();
        if (oldEntityId != this.entityId) {
            NpcRegistry.updateEntityId(this, oldEntityId);
        }
    }

    private void updateTeamForAll() {
        String entry = player.getInternalName();
        if (entry == null || entry.isEmpty()) {
            entry = uuid.toString();
        }
        String entityEntry = getEntityTeamEntry();
        // Scoreboard team names are limited to 16 characters.
        String teamName = "bf_" + uuid.toString().replace("-", "").substring(0, 13);
        try {
            org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            java.lang.reflect.Method setColor = team.getClass().getMethod("setColor", ChatColor.class);
            if (setColor != null) {
                setColor.setAccessible(true);
                setColor.invoke(team, glowColor == null ? ChatColor.WHITE : glowColor);
            }
            team.setNameTagVisibility(nameVisible ? org.bukkit.scoreboard.NameTagVisibility.ALWAYS
                    : org.bukkit.scoreboard.NameTagVisibility.NEVER);
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            if (entityEntry != null && !entityEntry.equals(entry) && !team.hasEntry(entityEntry)) {
                team.addEntry(entityEntry);
            }
        } catch (Throwable e) {
            Bukkit.getLogger().log(java.util.logging.Level.WARNING, "BlueFoundation NPC team update failed", e);
        }
    }

    private String getEntityTeamEntry() {
        if (entityType == EntityType.PLAYER || entityHandle == null) {
            return null;
        }
        try {
            Method method = entityHandle.getClass().getMethod("getUUID");
            method.setAccessible(true);
            Object value = method.invoke(entityHandle);
            return value instanceof UUID ? value.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean canWearEquipment() {
        return entityType == EntityType.PLAYER
                || entityType == EntityType.SKELETON
                || entityType == EntityType.ZOMBIE;
    }

    private boolean canUsePose() {
        return entityType == EntityType.PLAYER;
    }
}
