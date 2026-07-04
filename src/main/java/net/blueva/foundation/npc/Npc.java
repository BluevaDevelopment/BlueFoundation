package net.blueva.foundation.npc;

import net.blueva.foundation.npc.event.NpcClickEvent;
import net.blueva.foundation.npc.util.NpcAnimation;
import net.blueva.foundation.npc.util.NpcPose;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Public API for a player NPC.
 */
public interface Npc {

    UUID getUuid();

    int getEntityId();

    Location getLocation();

    void setLocation(Location location);

    void teleport(Location location);

    Npc entityType(EntityType type);

    EntityType getEntityType();

    double getHeight();

    String getName();

    Npc name(String name);

    Npc skin(Skin skin);

    /**
     * Sets the NPC equipment.
     *
     * <p>The array order must follow the server's {@code EquipmentSlot} enum order.
     * On modern versions that is typically: main hand, off hand, feet, legs, chest, head.</p>
     *
     * @param equipment equipment items, {@code null} entries are ignored
     */
    Npc equipment(ItemStack... equipment);

    Npc pose(NpcPose pose);

    NpcPose getPose();

    Npc lookAt(Player target);

    Npc lookAtClosestPlayer(boolean enabled);

    Npc lookAtClosestPlayer(boolean enabled, double range);

    boolean isLookingAtClosestPlayer();

    Npc glow(ChatColor color);

    Npc glow(boolean glow);

    ChatColor getGlowColor();

    Npc scale(double scale);

    double getScale();

    Npc nameVisible(boolean visible);

    boolean isNameVisible();

    Npc listed(boolean listed);

    boolean isListed();

    Npc animate(NpcAnimation animation);

    Npc onClick(Consumer<NpcClickEvent> action);

    void showTo(Player player);

    void hideFrom(Player player);

    boolean isShownTo(Player player);

    void refresh(Player player);

    void destroy();
}
