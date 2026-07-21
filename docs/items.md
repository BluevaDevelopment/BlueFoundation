# Items

`BlueFoundation.Items` creates and edits Bukkit `ItemStack` instances. Display names and lore are MiniMessage-first, then serialized to legacy strings only because older Bukkit item metadata APIs require strings.

```java
ItemStack item = BlueFoundation.Items.builder("OAK_SIGN", "SIGN")
        .amount(1)
        .name("<gold>Game Selector</gold>")
        .lore("<gray>Right click to open")
        .glow()
        .hideAllFlags()
        .build();
```

Useful helpers:

```java
BlueFoundation.Items.name(item, "<green>Ready");
BlueFoundation.Items.lore(item, "<gray>Line one", "<yellow>Line two");
ItemStack copy = BlueFoundation.Items.builder(existingItem).name("<green>Copy").build();
BlueFoundation.Items.enchant(item, "sharpness", 1);
BlueFoundation.Items.unbreakable(item, true);
BlueFoundation.Items.customModelData(item, 1001);
BlueFoundation.Items.pdcString(item, "myplugin", "item_id", "selector");
String itemId = BlueFoundation.Items.pdcString(item, "myplugin", "item_id");
BlueFoundation.Items.skullTexture(item, "http://textures.minecraft.net/texture/...");
BlueFoundation.Items.skullValue(item, "{player}", player);
BlueFoundation.Items.editMeta(item, meta -> {
    // Add plugin-specific metadata without leaving the BlueFoundation item flow.
});
```

If your plugin already has Adventure components after placeholder processing,
use the component overloads to avoid serializing and parsing text twice:

```java
BlueFoundation.Items.name(item, titleComponent);
BlueFoundation.Items.loreComponents(item, loreComponents);
BlueFoundation.Items.loreSplit(item, "<gray>Line one\n<yellow>Line two");
```
