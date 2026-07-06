package net.blueva.foundation.commands;

import net.blueva.foundation.messages.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Small command registration and sender helper utilities. */
public class Commands {

    protected Commands() {
    }

    public static PluginCommand register(JavaPlugin plugin, String name, Handler handler) {
        return register(plugin, name, handler, null);
    }

    public static PluginCommand register(JavaPlugin plugin, String name, Handler handler, Completer completer) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }

        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalArgumentException("Command '" + name + "' is not declared in plugin.yml");
        }

        command.setExecutor(new ExecutorAdapter(handler));
        if (completer != null) {
            command.setTabCompleter(new CompleterAdapter(completer));
        }
        return command;
    }

    public static boolean playerOnly(CommandSender sender, String message) {
        if (sender instanceof Player) {
            return true;
        }
        Messages.send(sender, isBlank(message) ? "<red>This command can only be used by players." : message);
        return false;
    }

    public static Player player(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    public static boolean hasPermission(CommandSender sender, String permission, String message) {
        if (sender == null) {
            return false;
        }
        if (isBlank(permission) || sender.hasPermission(permission)) {
            return true;
        }
        Messages.send(sender, isBlank(message) ? "<red>You do not have permission." : message);
        return false;
    }

    public static List<String> suggest(String current, Collection<String> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }

        String prefix = current == null ? "" : current.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option != null && option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(option);
            }
        }
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);
        return matches;
    }

    public static String arg(String[] args, int index) {
        return args != null && index >= 0 && args.length > index ? args[index] : "";
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public interface Handler {
        boolean execute(CommandSender sender, Command command, String label, String[] args);
    }

    public interface Completer {
        List<String> complete(CommandSender sender, Command command, String label, String[] args);
    }

    private static final class ExecutorAdapter implements CommandExecutor {
        private final Handler handler;

        private ExecutorAdapter(Handler handler) {
            this.handler = handler;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            return handler.execute(sender, command, label, args);
        }
    }

    private static final class CompleterAdapter implements TabCompleter {
        private final Completer completer;

        private CompleterAdapter(Completer completer) {
            this.completer = completer;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> result = completer.complete(sender, command, alias, args);
            return result == null ? Collections.<String>emptyList() : result;
        }
    }
}
