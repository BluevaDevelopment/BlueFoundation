package net.blueva.foundation.scoreboard;

import org.bukkit.entity.Player;

/**
 * Public entry point for BlueFoundation scoreboards.
 */
public final class Scoreboards {

    private Scoreboards() {
    }

    /**
     * Creates a new sidebar scoreboard for the given player.
     *
     * @param player the scoreboard owner
     * @return the created scoreboard
     */
    public static BfScoreboard create(Player player) {
        return new BfScoreboard(player);
    }
}
