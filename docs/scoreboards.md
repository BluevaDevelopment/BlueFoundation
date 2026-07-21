# Scoreboard

`BlueFoundation.Scoreboards` creates packet-based sidebar scoreboards for individual players. It works from Minecraft 1.8 to modern versions without hard NMS dependencies.

```java
BfScoreboard board = BlueFoundation.Scoreboards.create(player);
board.updateTitle("<gold>My Server</gold>");
board.updateLines(
        "<yellow>Online: <white>" + Bukkit.getOnlinePlayers().size(),
        "<yellow>Coins: <white>" + coins,
        "",
        "<gray>play.myserver.com"
);

// Update a single line later
board.updateLine(1, "<green>Coins: <white>" + updatedCoins);

// Remove it when done
board.delete();
```

Scoreboard text is parsed as MiniMessage by default. You can also pass Adventure `Component` values to `updateTitle`, `updateLine` and `updateLines`.
