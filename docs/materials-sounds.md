# Materials and sounds

`BlueFoundation.Materials` and `BlueFoundation.Sounds` resolve renamed constants safely by trying multiple names. Sounds also accept modern namespaced keys on servers that expose the Bukkit sound registry.

```java
Material oakSign = BlueFoundation.Materials.require("OAK_SIGN", "SIGN");
Sound levelUp = BlueFoundation.Sounds.require("ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
Sound pling = BlueFoundation.Sounds.require("minecraft:block.note_block.pling", "NOTE_PLING");

BlueFoundation.Sounds.play(player, 1.0F, 1.0F, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
```
