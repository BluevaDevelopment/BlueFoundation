# Game rules

**Available on:** Bukkit ✅ only - no world state on a proxy or (yet) mapped on Hytale

`BlueFoundation.GameRules` sets and reads boolean game rules on any server version. 1.13 replaced
`World#setGameRuleValue(String, String)` with the typed `World#setGameRule(GameRule, T)` API and
deprecated the string form; `org.bukkit.GameRule` does not exist at all before that.

```java
BlueFoundation.GameRules.set(world, "doDaylightCycle", false);
BlueFoundation.GameRules.set(world, "doMobSpawning", false);

boolean weather = BlueFoundation.GameRules.getBoolean(world, "doWeatherCycle", true);
boolean known = BlueFoundation.GameRules.isSupported(world, "announceAdvancements");
```

Both spellings are accepted. `GameRule#getByName` only takes the camelCase Minecraft id, so an
UPPER_SNAKE name (the Bukkit field spelling) is converted before lookup:

```java
// These two are equivalent.
BlueFoundation.GameRules.set(world, "showDeathMessages", false);
BlueFoundation.GameRules.set(world, "SHOW_DEATH_MESSAGES", false);
```

That makes migrating existing `GameRule.SHOW_DEATH_MESSAGES` call sites a straight substitution:
pass the constant's name as a string and the conversion is handled for you.

1.21.11 moved game rules to lowercase registry keys and renamed several of them (`doDaylightCycle`
became `advance_time`, `doMobSpawning` became `spawn_mobs`, and so on). Legacy names keep working
there too: they are mapped to the new key when the old one is not found. New keys such as
`advance_time` are accepted as well.

`set` returns whether the rule was applied and `getBoolean` takes the value to return when the rule
is missing or unreadable, so a rule that does not exist on the running version degrades quietly
instead of throwing.
