# NPCs

`BlueFoundation.NPCs` creates packet-based player NPCs that work across Bukkit/Spigot/Paper versions without depending on versioned NMS package names. It builds a fake `ServerPlayer`/`EntityPlayer` internally and sends spawn, equipment, teleport, animation and destroy packets only to selected viewers.

This approach avoids version-detection issues on modern Paper servers, because version detection is handled by `BlueFoundation.Version` instead of parsing `Bukkit.getServer().getClass().getPackage().getName()`.

> **Status:** usable MVP. Core features are implemented and the module compiles, but it has not yet been battle-tested on every modern Paper build. Feedback from real servers is welcome.

Initialize and shut down the module:

```java
@Override
public void onEnable() {
    BlueFoundation.NPCs.init(this);
}

@Override
public void onDisable() {
    BlueFoundation.NPCs.close();
}
```

Create and show an NPC:

```java
Npc npc = BlueFoundation.NPCs.create(location, "Shop Keeper");
npc.skin(Skin.fromPlayer(somePlayer));
npc.equipment(helmet, chestplate, leggings, boots, mainHand, offHand);
npc.showTo(player);
```

The `equipment` array follows the server's `EquipmentSlot` enum order. On modern versions that order is typically: main hand, off hand, feet, legs, chest, head.

Handle interactions:

```java
npc.onClick(event -> {
    if (event.getClickType() == NpcClickEvent.ClickType.RIGHT) {
        BlueFoundation.Messages.send(event.getPlayer(), "<green>Hello!");
    }
});
```

Movement, animations and cleanup:

```java
npc.teleport(newLocation);
npc.lookAt(targetPlayer);
npc.animate(NpcAnimation.SWING_MAIN_ARM);
npc.hideFrom(player);
npc.destroy();
```

Skins can also be fetched asynchronously from Mojang:

```java
Skin.fetch(this, playerUuid, skin -> {
    if (skin != null) {
        npc.skin(skin);
    }
});
```

Events fired by the module:

- `NpcSpawnEvent` — when an NPC is shown to a player.
- `NpcDespawnEvent` — when an NPC is hidden from a player.
- `NpcClickEvent` — when a player left/right-clicks an NPC.
