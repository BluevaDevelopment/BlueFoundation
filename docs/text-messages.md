# Text and messages

`BlueFoundation.Text` parses MiniMessage into Adventure `Component` values and serializes components back to legacy strings only for old Bukkit APIs that still require strings.

```java
Component title = BlueFoundation.Text.component("<gold>Victory!</gold>");
String inventoryTitle = BlueFoundation.Text.legacySection(title);
```

`BlueFoundation.Messages` sends Adventure components to players, command senders, action bars, and titles. Message strings are parsed as MiniMessage by default. Legacy `&` colors are accepted only as a compatibility input path.

```java
BlueFoundation.Messages.send(player, "<green>Hello!");
BlueFoundation.Messages.actionBar(player, "<yellow>Action bar text");
BlueFoundation.Messages.title(player, "<aqua>Title", "<gray>Subtitle", 10, 70, 20);
```
