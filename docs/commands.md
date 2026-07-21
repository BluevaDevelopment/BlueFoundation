# Commands

`BlueFoundation.Commands` registers command handlers declared in `plugin.yml` and provides small sender/tab-completion helpers.

```java
BlueFoundation.Commands.register(
        this,
        "arena",
        (sender, command, label, args) -> {
            if (!BlueFoundation.Commands.hasPermission(sender, "blueva.arena", "<red>No permission.")) {
                return true;
            }
            BlueFoundation.Messages.send(sender, "<green>Arena command executed.");
            return true;
        },
        (sender, command, label, args) -> BlueFoundation.Commands.suggest(
                BlueFoundation.Commands.arg(args, 0),
                Arrays.asList("create", "delete", "edit")
        )
);
```
