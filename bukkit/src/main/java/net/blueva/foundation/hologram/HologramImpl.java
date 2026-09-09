package net.blueva.foundation.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Internal implementation of {@link Hologram}. */
final class HologramImpl implements Hologram {

    private static final byte SHADOW_BIT = 0x01;

    private final Set<UUID> viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private Location anchor;
    private List<String> lines;
    private List<HologramLine> hologramLines;
    private double lineHeight = 0.25D;
    private float scale = 1.0f;
    private Integer background;
    private byte styleFlags = 0;
    private int teleportDuration = 0;

    HologramImpl(Location anchor, List<String> lines) {
        this.anchor = anchor.clone();
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        this.hologramLines = buildLines(this.lines);
    }

    @Override
    public synchronized Hologram text(List<String> newLines) {
        List<String> sanitized = newLines == null ? new ArrayList<>() : new ArrayList<>(newLines);
        List<HologramLine> oldHologramLines = this.hologramLines;
        Set<UUID> currentViewers = new HashSet<>(viewers);
        for (UUID viewerId : currentViewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                despawnFrom(viewer, oldHologramLines);
            }
        }
        this.lines = sanitized;
        this.hologramLines = buildLines(sanitized);
        for (UUID viewerId : currentViewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && viewers.contains(viewerId)) {
                spawnTo(viewer, this.hologramLines);
            }
        }
        return this;
    }

    @Override
    public List<String> getText() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public synchronized Hologram lineHeight(double lineHeight) {
        this.lineHeight = lineHeight;
        // Re-run through text() so every line's location and its viewers get refreshed with the
        // new spacing instead of only updating the field.
        return text(this.lines);
    }

    @Override
    public double getLineHeight() {
        return lineHeight;
    }

    @Override
    public synchronized Location getLocation() {
        return anchor.clone();
    }

    @Override
    public synchronized void teleport(Location location) {
        this.anchor = location.clone();
        List<HologramLine> current = this.hologramLines;
        for (int i = 0; i < current.size(); i++) {
            Location lineLocation = lineLocation(i);
            HologramPackets.updateEntityPosition(current.get(i).entityHandle, lineLocation);
            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    HologramPackets.sendTeleport(viewer, current.get(i).entityHandle, lineLocation);
                }
            }
        }
    }

    @Override
    public void showTo(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (viewers.add(id)) {
            spawnTo(player, hologramLines);
        }
    }

    @Override
    public void hideFrom(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (viewers.remove(id)) {
            despawnFrom(player, hologramLines);
        }
    }

    @Override
    public boolean isShownTo(Player player) {
        return player != null && viewers.contains(player.getUniqueId());
    }

    @Override
    public synchronized Hologram scale(float scale) {
        this.scale = scale;
        for (HologramLine line : hologramLines) {
            HologramPackets.setScale(line.entityHandle, scale);
        }
        pushMetadataToViewers();
        return this;
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public synchronized Hologram background(Integer argb) {
        this.background = argb;
        if (argb != null) {
            for (HologramLine line : hologramLines) {
                HologramPackets.setBackground(line.entityHandle, argb);
            }
            pushMetadataToViewers();
        }
        return this;
    }

    @Override
    public Integer getBackground() {
        return background;
    }

    @Override
    public synchronized Hologram shadow(boolean shadow) {
        this.styleFlags = shadow ? (byte) (styleFlags | SHADOW_BIT) : (byte) (styleFlags & ~SHADOW_BIT);
        for (HologramLine line : hologramLines) {
            HologramPackets.setStyleFlags(line.entityHandle, styleFlags);
        }
        pushMetadataToViewers();
        return this;
    }

    @Override
    public boolean hasShadow() {
        return (styleFlags & SHADOW_BIT) == SHADOW_BIT;
    }

    @Override
    public synchronized Hologram teleportDuration(int ticks) {
        this.teleportDuration = Math.max(0, ticks);
        for (HologramLine line : hologramLines) {
            HologramPackets.setTeleportDuration(line.entityHandle, this.teleportDuration);
        }
        pushMetadataToViewers();
        return this;
    }

    @Override
    public int getTeleportDuration() {
        return teleportDuration;
    }

    private void pushMetadataToViewers() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                for (HologramLine line : hologramLines) {
                    HologramPackets.sendMetadata(viewer, line.entityHandle);
                }
            }
        }
    }

    @Override
    public synchronized void destroy() {
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                despawnFrom(viewer, hologramLines);
            }
        }
        viewers.clear();
        HologramRegistry.unregister(this);
    }

    private List<HologramLine> buildLines(List<String> text) {
        List<HologramLine> built = new ArrayList<>(text.size());
        for (int i = 0; i < text.size(); i++) {
            String line = text.get(i);
            Object handle = HologramEntityFactory.create(lineLocation(i));
            if (handle == null) {
                continue;
            }
            HologramPackets.setText(handle, line);
            HologramPackets.setBillboardCenter(handle);
            HologramPackets.setScale(handle, scale);
            if (background != null) {
                HologramPackets.setBackground(handle, background);
            }
            HologramPackets.setStyleFlags(handle, styleFlags);
            if (teleportDuration > 0) {
                HologramPackets.setTeleportDuration(handle, teleportDuration);
            }
            built.add(new HologramLine(handle, HologramPackets.resolveEntityId(handle), line));
        }
        return built;
    }

    private Location lineLocation(int index) {
        return anchor.clone().subtract(0.0D, index * lineHeight, 0.0D);
    }

    private void spawnTo(Player viewer, List<HologramLine> lines) {
        for (HologramLine line : lines) {
            HologramPackets.sendAddEntity(viewer, line.entityHandle);
            HologramPackets.sendMetadata(viewer, line.entityHandle);
        }
    }

    private void despawnFrom(Player viewer, List<HologramLine> lines) {
        for (HologramLine line : lines) {
            HologramPackets.sendDestroy(viewer, line.entityId);
        }
    }
}
