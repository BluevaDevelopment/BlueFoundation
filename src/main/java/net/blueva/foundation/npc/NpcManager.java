package net.blueva.foundation.npc;

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

    private static Plugin plugin;
    private static int lookAtTaskId = -1;

    NpcManager(Plugin plugin) {
        NpcManager.plugin = plugin;
    }

    static Plugin getPlugin() {
        return plugin;
    }

    void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        NpcPacketListener.injectAll();
        startLookAtTask();
    }

    void disable() {
        HandlerList.unregisterAll(this);
        if (lookAtTaskId != -1) {
            Bukkit.getScheduler().cancelTask(lookAtTaskId);
            lookAtTaskId = -1;
        }
        List<NpcImpl> copy = new ArrayList<>(NpcRegistry.all());
        for (NpcImpl npc : copy) {
            npc.destroy();
        }
        NpcRegistry.clear();
        NpcPacketListener.uninjectAll();
    }

    private static void startLookAtTask() {
        lookAtTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (NpcImpl npc : NpcRegistry.all()) {
                try {
                    npc.updateLookAt();
                } catch (Throwable ignored) {
                }
            }
        }, 10L, 5L).getTaskId();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final org.bukkit.entity.Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> NpcPacketListener.inject(player), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        NpcPacketListener.uninject(event.getPlayer());
    }
}
