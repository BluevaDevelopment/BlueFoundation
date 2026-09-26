package net.blueva.foundation.npc.util;

/**
 * Animations that can be played on a player NPC.
 *
 * <p>Ids are the vanilla entity animation packet ids up to 26.2; newer versions map each
 * animation to its own packet. Older versions silently ignore unsupported ids.</p>
 */
public enum NpcAnimation {
    SWING_MAIN_ARM(0),
    TAKE_DAMAGE(1),
    LEAVE_BED(2),
    SWING_OFFHAND(3),
    CRITICAL(4),
    MAGIC_CRITICAL(5);

    private final int id;

    NpcAnimation(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
