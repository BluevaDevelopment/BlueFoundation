# Reflection helpers

`BlueFoundation.Reflection` provides small helpers for CraftBukkit/NMS lookups and packet sending.

```java
Class<?> craftPlayer = BlueFoundation.Reflection.craftBukkitClass("entity.CraftPlayer");
Class<?> packetClass = BlueFoundation.Reflection.nmsClass("PacketPlayOutChat");
Object handle = BlueFoundation.Reflection.getHandle(player);
```
