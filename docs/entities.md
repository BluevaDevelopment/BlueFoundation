# Entities

`BlueFoundation.Entities` resolves renamed `EntityType` constants safely by trying multiple names, so code compiled against an old API keeps working on servers where the constant was renamed (and vice versa). Aliases are resolved bidirectionally: passing only the modern name still works on legacy servers, and only the legacy name works on modern ones.

```java
// EntityType.PRIMED_TNT was renamed to EntityType.TNT in 1.20.5
TNTPrimed tnt = (TNTPrimed) BlueFoundation.Entities.spawn(location, "TNT", "PRIMED_TNT");

EntityType type = BlueFoundation.Entities.require("ITEM", "DROPPED_ITEM");
boolean supported = BlueFoundation.Entities.isSupported("BLOCK_DISPLAY");
```

Known renames are covered out of the box (TNT, item drops, minecart variants, leash knots, firework rockets, piglins, mooshrooms, and more), so a single name is usually enough:

```java
EntityType tntType = BlueFoundation.Entities.require("TNT"); // resolves PRIMED_TNT on legacy servers
```
