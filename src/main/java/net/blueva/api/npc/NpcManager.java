package net.blueva.api.npc;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle manager for the NPC module.
 */
final class NpcManager implements Listener {

    private final Plugin plugin;

    NpcManager(Plugin plugin) {
        this.plugin = plugin;
    }

    void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        NpcPacketListener.injectAll();
    }

    void disable() {
        HandlerList.unregisterAll(this);
        List<NpcImpl> copy = new ArrayList<>(NpcRegistry.all());
        for (NpcImpl npc : copy) {
            npc.destroy();
        }
        NpcRegistry.clear();
        NpcPacketListener.uninjectAll();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        NpcPacketListener.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        NpcPacketListener.uninject(event.getPlayer());
    }
}
