package net.blueva.api.npc.event;

import net.blueva.api.npc.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when an NPC is hidden from a player.
 */
public class NpcDespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Npc npc;
    private final Player viewer;

    public NpcDespawnEvent(Npc npc, Player viewer) {
        this.npc = npc;
        this.viewer = viewer;
    }

    public Npc getNpc() {
        return npc;
    }

    public Player getViewer() {
        return viewer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
