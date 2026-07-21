# Multi-version events

`BlueFoundation.Events` provides wrapped events that register the correct Bukkit event implementation at runtime.

```java
public final class PickupListener implements BlueFoundation.Events.EntityPickup {

    @Override
    public boolean onEntityPickUpItemStack(Entity entity, Item item, int remaining, boolean cancelled) {
        return false; // return true to cancel
    }
}
```

Register wrapped listeners from your plugin:

```java
if (BlueFoundation.Events.supports(BlueFoundation.Events.Type.ENTITY_PICKUP)) {
    BlueFoundation.Events.register(
            this,
            new PickupListener(),
            EventPriority.NORMAL
    );
}
```

Current wrapped events:

- `BlueFoundation.Events.EntityPickup`
  - Uses `PlayerPickupItemEvent` on legacy servers.
  - Uses `EntityPickupItemEvent` on newer servers.
- `BlueFoundation.Events.PlayerSwapHandItems`
  - Available on servers with `PlayerSwapHandItemsEvent`.
- `BlueFoundation.Events.PlayerItemMend`
  - Available on servers with `PlayerItemMendEvent`.
- `BlueFoundation.Events.EntityToggleGlide`
  - Available on servers with `EntityToggleGlideEvent`.
- `BlueFoundation.Events.EntityAirChange`
  - Available on servers with `EntityAirChangeEvent`.
- `BlueFoundation.Events.PlayerInteractAtEntity`
  - Available on servers with `PlayerInteractAtEntityEvent`. Provides `handName` when available.
- `BlueFoundation.Events.PlayerArmorStandManipulate`
  - Available on servers with `PlayerArmorStandManipulateEvent`. Provides `slotName` and `handName` when available.

Use `BlueFoundation.Events.supports(BlueFoundation.Events.Type...)` to check capabilities before registering. Unsupported wrapped events also return `false` from `BlueFoundation.Events.register(...)`.
