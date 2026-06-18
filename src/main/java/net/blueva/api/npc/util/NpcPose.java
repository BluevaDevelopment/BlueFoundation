package net.blueva.api.npc.util;

/**
 * Poses supported by player NPCs.
 *
 * <p>Modern versions support all values; older versions fall back to
 * standing/sneaking when a pose is not available.</p>
 */
public enum NpcPose {
    STANDING,
    FALL_FLYING,
    SLEEPING,
    SWIMMING,
    SPIN_ATTACK,
    CROUCHING,
    LONG_JUMPING,
    DYING,
    CRAWLING
}
