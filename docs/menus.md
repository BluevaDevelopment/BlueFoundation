# Menus, forms and dialogs

**Available on:** Bukkit ✅ only - the other platforms have no inventory to draw into

Three related facades:

| Facade | What it draws | Needs |
|---|---|---|
| `BlueFoundation.Menus` | One definition; a chest inventory for Java players, a native Bedrock form for Bedrock ones | nothing |
| `BlueFoundation.Forms` | A Bedrock form directly, built by hand | nothing |
| `BlueFoundation.Dialogs` | A Java Edition dialog screen | Minecraft 1.21.6+ |

None of them requires Floodgate, Geyser or Cumulus as a dependency, on the compile classpath or in
your jar. See [Talking to Bedrock without Floodgate](#talking-to-bedrock-without-floodgate) for how.

## Menus

A menu is described once. Which surface a player gets is decided per player at open time, so a
server with both editions on it needs one config rather than a `java/` and a `bedrock/` folder that
have to be kept in step.

```java
MenuDefinition warps = BlueFoundation.Menus.load("warps", config.section("menu"));
BlueFoundation.Menus.register(warps);

BlueFoundation.Menus.open(plugin, player, "warps");
```

Or without a config file:

```java
MenuDefinition menu = MenuDefinition.builder("warps")
        .title("<gradient:#EC4899:#F43F5E>Warps</gradient>")
        .rows(3)
        .button(MenuButton.builder("spawn")
                .name("<green>Spawn")
                .lore("<gray>Go back to the hub")
                .material("PLAYER_HEAD", "SKULL_ITEM")
                .slot(11)
                .image("https://example.com/spawn.png")
                .onClick(p -> p.performCommand("spawn"))
                .build())
        .closeButton(MenuButton.builder("close")
                .name("<red>Close").material("BARRIER").slot(22).action("close").build())
        .build();

BlueFoundation.Menus.open(plugin, new MenuContext(player, menu));
```

A button with no name, no lore and nothing to do is wallpaper, and its tooltip
is hidden without being asked: hovering the gap between two groups of buttons
should not open an empty window. `hide-tooltip: true` says so explicitly for
anything else. It needs Minecraft 1.20.5 or later; on older servers a blank
name still carries most of the effect.

`material` takes several names and uses the first that exists on the running server, which is how
one definition covers 1.8 through modern without a version branch. `image` is the icon beside the
button on Bedrock and is ignored on Java; `material` is the item on Java and is ignored on Bedrock.
A button's lore is folded into the Bedrock button's label - Bedrock buttons wrap on newlines - so
the explanation a Java player reads in the tooltip is not lost.

### Dynamic content and pagination

Both `layout: static` and `layout: paginated` accept runtime buttons through
`MenuContext.entries(...)`. Dynamic content does not imply pagination.

- `static`: one view, no previous/next navigation. A chest fills its available
  dynamic slots; a form shows all supplied entries. `page-size` does not split
  the entries and `page:` actions are ignored.
- `paginated`: divides the entries into pages using `page-size` (or the form
  page size), with previous/next navigation.

Declared buttons and decoration keep their slots in either layout. Use `static`
for a dynamically updated control panel or independently scrolling rows, and
`paginated` for a list the user browses page by page. The legacy layout aliases
`list`, `grid` and `dynamic` still mean `paginated` for backward compatibility.

Set `layout: paginated` and hand the entries in at open time:

```java
List<MenuButton> entries = new ArrayList<>();
for (Warp warp : warps) {
    entries.add(MenuButton.builder(warp.id())
            .name("<aqua>" + warp.name())
            .material("ENDER_PEARL")
            .action("command: warp " + warp.id())
            .build());
}

BlueFoundation.Menus.open(plugin, new MenuContext(player, menu)
        .entries(entries)
        .placeholder("{world}", player.getWorld().getName()));
```

Entries fill the menu's `dynamic-slots`, or every slot no declared button occupies. On Bedrock they
become buttons after the declared ones, with previous/next at the end where a phone user scrolls to
find them. `{page}`, `{page_total}`, `{page_next}`, `{page_previous}`, `{page_has_next}`,
`{page_has_previous}` and `{entries_total}` are substituted in every string.

### Actions

```yaml
actions:
  - "command: spawn"        # as the player
  - "console: say hello"    # as the console
  - "message: <green>Done"
  - "menu: warps_admin"     # open another registered menu
  - "back"                  # return to the menu this one was opened from
  - "page: next"            # next | previous | first | last | 3
  - "close"
  - "sound: UI_BUTTON_CLICK"
  - "custom: shop buy diamond 3"
```

`custom` routes to a handler you register, which is the extension point that keeps the shared
vocabulary from growing a case for every plugin that uses it:

```java
BlueFoundation.Menus.handler("shop", (player, payload, context) -> {
    shop.buy(player, payload);
    return true;
});
```

Actions may also be written as mappings, which is the only form that can restrict an action to one
kind of click:

```yaml
actions:
  - type: command
    value: warp spawn
    click: RIGHT           # LEFT | RIGHT | SHIFT | MIDDLE | any ClickType name
```

A click filter never matches on Bedrock, where a form button has one kind of press.

### Placeholders

BlueFoundation ships no placeholder engine. Install a resolver once and every menu string - titles,
names, lore, form bodies, action values, visibility conditions - goes through it:

```java
BlueFoundation.Menus.resolver((player, text) -> PlaceholderAPI.setPlaceholders(player, text));
```

`visible-if` is resolved the same way and read as a boolean. Anything that is not a recognised false
counts as true, so a placeholder that failed to resolve leaves the button visible rather than
silently emptying a menu.

### The config schema

```yaml
title: "<gradient:#EC4899:#F43F5E>Warps</gradient>"
mode: auto             # auto | chest | form
layout: paginated      # static | paginated
size: 54               # or: rows: 6
page-size: 28
dynamic-slots: [10,11,12,13,14,15,16]
content:               # the form body; ignored by chest menus
  - "<gray>Pick somewhere to go"
empty: "<red>No warps yet"
form:
  style: simple        # simple | modal
  icon: "https://example.com/icon.png"
sounds:
  open: BLOCK_CHEST_OPEN
  click: UI_BUTTON_CLICK

decoration:
  material: GRAY_STAINED_GLASS_PANE
  name: " "
  slots: [0,1,2,3,4,5,6,7,8]

items:
  spawn:
    name: "<green>Spawn"
    lore: ["<gray>Go home"]
    material: "PLAYER_HEAD, SKULL_ITEM"
    amount: 1
    slot: 20             # or: slots: [20, 21]
    skull: "{player}"    # a name, a UUID, a base64 texture, or {player}
    image: "https://example.com/spawn.png"
    glow: false
    hide-flags: true
    hide-tooltip: false  # removes the box, not just the text
    enchantments: ["DURABILITY:1"]
    visible-if: "%some_placeholder%"
    actions:
      - "command: spawn"

template:                # the shape entries take in a paginated menu
  name: "<aqua>{name}"
  material: ENDER_PEARL
  actions: ["command: warp {name}"]

navigation:
  previous: { material: ARROW,   name: "<yellow>Back",  slot: 45 }
  next:     { material: ARROW,   name: "<yellow>Next",  slot: 53 }
  close:    { material: BARRIER, name: "<red>Close",   slot: 49 }
```

Every key is optional. A `menu:` wrapper key is unwrapped, so a menu can live in a file alongside a
plugin's own settings. Keys BlueFoundation does not recognise are kept and readable through
`definition.property(key)`.

Navigation buttons get `page: previous`, `page: next` and `close` for free; an explicit `actions`
list overrides that.

`mode: form` draws only for Bedrock players and is skipped, with a console warning, for Java ones.
`mode: chest` draws a chest for everybody - Geyser renders Java containers on Bedrock natively, it
just looks like a chest rather than a native screen.

## Forms

For when a Bedrock form is the point, rather than the Bedrock half of a menu:

```java
BlueFoundation.Forms.send(plugin, player, SimpleForm.builder()
        .title("Warps")
        .content("Pick somewhere to go")
        .button("Spawn", FormImage.url("https://example.com/spawn.png"), p -> p.performCommand("spawn"))
        .button("Shop", p -> p.performCommand("shop"))
        .onClosed(p -> p.sendMessage("Maybe later."))
        .build());
```

`ModalForm` is the two-button version. `CustomForm` is the one with fields, which has no chest
equivalent - it is the Bedrock answer to an anvil prompt:

```java
BlueFoundation.Forms.send(plugin, player, CustomForm.builder()
        .title("House settings")
        .label("<gray>These apply to everyone who visits.")
        .input("name", "House name", "My house", house.name())
        .toggle("pvp", "Allow PvP", house.pvp())
        .slider("radius", "Border radius", 16, 256, 16, house.radius())
        .dropdown("weather", "Weather", "clear", "rain", "thunder")
        .onSubmit((p, response) -> {
            house.name(response.getString("name"));
            house.pvp(response.getBoolean("pvp"));
            house.radius((int) response.getFloat("radius"));
            house.weather(response.getOption("weather"));
        })
        .build());
```

Responses are read by key rather than by position, so inserting a label above a field later does not
silently shift every value.

`BlueFoundation.Bedrock.isBedrockPlayer(player)` answers the same question the menus use to pick a
surface.

### Talking to Bedrock without Floodgate

Three transports, tried in order, none of them a dependency:

1. **Floodgate's API**, by reflection. Preferred when it is reachable - not because the others do
   not work, but because Floodgate then owns the form ids and the pending-form table.
2. **Geyser's API**, by reflection. Covers Geyser running as a plugin without Floodgate.
3. **The raw `floodgate:form` plugin message.** Geyser handles this channel itself: a form type
   byte, a two-byte id, a JSON body, and an answer on the same channel. BlueFoundation builds and
   parses that JSON with its own writer and reader, so this path needs no GeyserMC class at all.
   The cost, and the reason it is last: Floodgate also listens on that channel and logs
   `Couldn't find stored form for player` for a form it did not send. Harmless, but noisy.

`BlueFoundation.Forms.transportFor(plugin, player)` returns which one would carry a form right now,
which is worth a line in a startup log or a diagnostics command.

Detecting a Bedrock player follows the same idea: Floodgate's API, then Geyser's, then the UUID -
Floodgate mints Bedrock UUIDs as `new UUID(0, xuid)`, so a zero high half is a Bedrock player, which
is the test Floodgate's own `isFloodgateId` makes. That last check cannot see a Bedrock player who
logged in through a linked Java account; if neither API is reachable and your server uses linking,
tell BlueFoundation the username prefix from Floodgate's config:

```java
BlueFoundation.Bedrock.usernamePrefix(".");
```

## Dialogs

Dialogs are a **Java Edition** feature, added in Minecraft 1.21.6. A Bedrock player connected
through Geyser does see one, because Geyser translates the packet into a native Bedrock form, but
that is Geyser's doing and it drops what a form cannot draw - item bodies in particular. For a menu
meant to look right on both editions, use `Menus`.

```java
BlueFoundation.Dialogs.show(plugin, player, Dialog.confirmation("<red>Delete this house?")
        .body("<gray>Everything in it goes with it. This cannot be undone.")
        .yes(DialogButton.of("<red>Delete", DialogAction.callback((p, response) -> houses.delete(p))))
        .no(DialogButton.of("<gray>Cancel"))
        .build());
```

`Dialog.notice`, `Dialog.confirmation` and `Dialog.multiAction` cover the three shapes. Buttons can
run a command, suggest one, open a URL, copy to the clipboard, or call back into your plugin - and a
callback button carries whatever the player typed into the dialog's inputs:

```java
Dialog rename = Dialog.multiAction("<aqua>Rename house")
        .input(DialogInput.text("name", "New name", house.name(), 32, null))
        .input(DialogInput.bool("public", "Visible in discovery", house.isPublic()))
        .input(DialogInput.numberRange("radius", "Border radius", 16, 256, 64f, 16f))
        .input(DialogInput.singleOption("weather", "Weather", "clear", "rain", "thunder"))
        .button(DialogButton.of("<green>Save", DialogAction.callback((p, response) -> {
            house.name(response.getString("name"));
            house.setPublic(response.getBoolean("public"));
            house.radius(response.getInt("radius"));
            house.weather(response.getString("weather"));
        })))
        .button(DialogButton.of("<gray>Cancel"))
        .columns(2)
        .build();
```

### Versions

Two server APIs can draw a dialog and no server has both - Spigot's `net.md_5.bungee.api.dialog`,
shipped from 1.21.6 in the separate `net.md-5:bungeecord-dialog` artifact, and Paper's
`io.papermc.paper.registry.data.dialog`. Whichever is present is used, both by reflection.

Neither exists before 1.21.6, and there is no way to fake one on an older client. So:

```java
if (BlueFoundation.Dialogs.supported()) {
    BlueFoundation.Dialogs.show(plugin, player, dialog);   // throws if it cannot draw
} else {
    BlueFoundation.Menus.open(plugin, player, "confirm_delete");
}
```

`show` throws `DialogUnsupportedException` and writes one line to the console explaining what is
missing and what the server is running. `trySend` returns `false` instead, for callers that would
rather branch than catch. `BlueFoundation.Dialogs.backendName()` reports `paper`, `spigot` or
`null`.

## Shutting down

Both facades hold a listener and, for the plugin-message transport, a channel registration. On a
plugin that supports reloading:

```java
@Override
public void onDisable() {
    BlueFoundation.Menus.release(this);
    BlueFoundation.Dialogs.release(this);
}
```
