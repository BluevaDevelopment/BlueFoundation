package net.blueva.api.npc.event;

import net.blueva.api.npc.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player interacts with (clicks) an NPC.
 */
public class NpcClickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Npc npc;
    private final ClickType clickType;
    private boolean cancelled;

    public NpcClickEvent(Player player, Npc npc, ClickType clickType) {
        this.player = player;
        this.npc = npc;
        this.clickType = clickType;
    }

    public Player getPlayer() {
        return player;
    }

    public Npc getNpc() {
        return npc;
    }

    public ClickType getClickType() {
        return clickType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum ClickType {
        LEFT,
        RIGHT
    }
}
