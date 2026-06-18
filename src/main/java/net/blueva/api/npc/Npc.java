package net.blueva.api.npc;

import net.blueva.api.npc.event.NpcClickEvent;
import net.blueva.api.npc.util.NpcAnimation;
import net.blueva.api.npc.util.NpcPose;
import org.bukkit.Location;
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

    Npc lookAt(Player target);

    Npc animate(NpcAnimation animation);

    Npc onClick(Consumer<NpcClickEvent> action);

    void showTo(Player player);

    void hideFrom(Player player);

    boolean isShownTo(Player player);

    void refresh(Player player);

    void destroy();
}
