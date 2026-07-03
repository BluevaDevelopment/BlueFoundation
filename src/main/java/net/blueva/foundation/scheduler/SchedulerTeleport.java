package net.blueva.foundation.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

final class SchedulerTeleport {
    private SchedulerTeleport() {
    }

    static CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (entity == null || location == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        CompletableFuture<Boolean> paper = invokeTeleportAsync(entity, location, cause);
        if (paper != null) {
            return paper;
        }
        try {
            return CompletableFuture.completedFuture(entity.teleport(location, cause));
        } catch (Throwable ignored) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<Boolean> invokeTeleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        try {
            Method method = entity.getClass().getMethod("teleportAsync", Location.class, PlayerTeleportEvent.TeleportCause.class);
            Object value = method.invoke(entity, location, cause);
            if (value instanceof CompletableFuture) {
                return (CompletableFuture<Boolean>) value;
            }
        } catch (Throwable ignored) {
            try {
                Method method = entity.getClass().getMethod("teleportAsync", Location.class);
                Object value = method.invoke(entity, location);
                if (value instanceof CompletableFuture) {
                    return (CompletableFuture<Boolean>) value;
                }
            } catch (Throwable ignoredAgain) {
                // Legacy Bukkit/Spigot path.
            }
        }
        return null;
    }
}
