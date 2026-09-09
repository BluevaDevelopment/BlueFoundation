package net.blueva.foundation.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A multi-line fake hologram: one fake {@code TextDisplay} entity per line, stacked vertically
 * below the anchor location. Purely packet-driven (no real entities ever enter the world),
 * per-viewer visibility - mirrors {@code net.blueva.foundation.npc.Npc} for the parts that make
 * sense for a hologram (no click handling, no skin/equipment/pose).
 *
 * <p>Each line is its own entity with an explicit, deterministic Y position instead of a single
 * entity whose text embeds newlines - this gives full control over line spacing rather than
 * depending on a text renderer's own (possibly buggy) line-splitting logic.</p>
 */
public interface Hologram {

    /** Replaces the hologram's lines and respawns it to every current viewer. */
    Hologram text(List<String> lines);

    List<String> getText();

    /** Vertical distance between consecutive lines, in blocks. Default {@code 0.25}. Changing it
     * respawns the hologram to every current viewer. */
    Hologram lineHeight(double lineHeight);

    double getLineHeight();

    /** The anchor location - the position of the first (topmost) line. */
    Location getLocation();

    /** Moves the hologram (all lines) to a new anchor location. */
    void teleport(Location location);

    void showTo(Player player);

    void hideFrom(Player player);

    boolean isShownTo(Player player);

    Hologram scale(float scale);

    float getScale();

    Hologram background(Integer argb);

    Integer getBackground();

    Hologram shadow(boolean shadow);

    boolean hasShadow();

    /** Ticks the client spends interpolating to a teleported position instead of snapping. Default
     * {@code 0}. Set 2-3 when the hologram is moved often (e.g. following a walking NPC) so it glides. */
    Hologram teleportDuration(int ticks);

    int getTeleportDuration();

    /** Destroys the hologram for every current viewer and unregisters it. */
    void destroy();
}
