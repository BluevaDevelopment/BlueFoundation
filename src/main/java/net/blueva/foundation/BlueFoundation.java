package net.blueva.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Master entry point for BlueFoundation.
 *
 * <p>This class intentionally stays small. Real implementations live in
 * dedicated packages, while these nested aliases keep the public API reachable
 * from a single namespace: {@code BlueFoundation.*}.</p>
 */
public final class BlueFoundation {

    private static final String UNKNOWN_VERSION = "unknown";

    private BlueFoundation() {
    }

    public static String version() {
        Package foundationPackage = BlueFoundation.class.getPackage();
        if (foundationPackage != null && foundationPackage.getImplementationVersion() != null) {
            return foundationPackage.getImplementationVersion();
        }

        try (InputStream input = BlueFoundation.class.getClassLoader().getResourceAsStream(
                "META-INF/maven/net.blueva.foundation/BlueFoundation/pom.properties")) {
            if (input == null) {
                return UNKNOWN_VERSION;
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("version", UNKNOWN_VERSION);
        } catch (IOException ignored) {
            return UNKNOWN_VERSION;
        }
    }

    public static class Dependencies extends net.blueva.foundation.dependencies.Dependencies {
        private Dependencies() {
        }
    }

    public static class Version extends net.blueva.foundation.version.Version {
        private Version() {
        }
    }

    public static class Reflection extends net.blueva.foundation.reflection.Reflection {
        private Reflection() {
        }
    }

    public static class Materials extends net.blueva.foundation.materials.Materials {
        private Materials() {
        }
    }

    public static class Items extends net.blueva.foundation.items.Items {
        private Items() {
        }
    }

    public static class AdventureItems extends net.blueva.foundation.items.AdventureItems {
        private AdventureItems() {
        }
    }

    public static class Sounds extends net.blueva.foundation.sounds.Sounds {
        private Sounds() {
        }
    }

    public static class Scheduler extends net.blueva.foundation.scheduler.Scheduler {
        private Scheduler() {
        }
    }

    public static class Commands extends net.blueva.foundation.commands.Commands {
        private Commands() {
        }
    }

    public static class Messages extends net.blueva.foundation.messages.Messages {
        private Messages() {
        }
    }

    public static class Text extends net.blueva.foundation.text.Text {
        private Text() {
        }
    }

    public static class AdventureText extends net.blueva.foundation.text.AdventureText {
        private AdventureText() {
        }
    }

    public static class Events extends net.blueva.foundation.events.Events {
        private Events() {
        }
    }

    public static class Configs extends net.blueva.foundation.config.Configs {
        private Configs() {
        }
    }

    public static class NPCs extends net.blueva.foundation.npc.NPCs {
        private NPCs() {
        }
    }

    public static class Scoreboards {
        private Scoreboards() {
        }

        public static net.blueva.foundation.scoreboard.BfScoreboard create(org.bukkit.entity.Player player) {
            return net.blueva.foundation.scoreboard.Scoreboards.create(player);
        }
    }
}
