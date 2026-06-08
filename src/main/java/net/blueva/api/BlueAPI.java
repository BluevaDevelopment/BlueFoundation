package net.blueva.api;

/**
 * Master entry point for BlueAPI.
 *
 * <p>This class intentionally stays small. Real implementations live in
 * dedicated packages, while these nested aliases keep the public API reachable
 * from a single namespace: {@code BlueAPI.*}.</p>
 */
public final class BlueAPI {

    private BlueAPI() {
    }

    public static class Dependencies extends net.blueva.api.dependencies.Dependencies {
        private Dependencies() {
        }
    }

    public static class Version extends net.blueva.api.version.Version {
        private Version() {
        }
    }

    public static class Reflection extends net.blueva.api.reflection.Reflection {
        private Reflection() {
        }
    }

    public static class Materials extends net.blueva.api.materials.Materials {
        private Materials() {
        }
    }

    public static class Items extends net.blueva.api.items.Items {
        private Items() {
        }
    }

    public static class Sounds extends net.blueva.api.sounds.Sounds {
        private Sounds() {
        }
    }

    public static class Scheduler extends net.blueva.api.scheduler.Scheduler {
        private Scheduler() {
        }
    }

    public static class Commands extends net.blueva.api.commands.Commands {
        private Commands() {
        }
    }

    public static class Messages extends net.blueva.api.messages.Messages {
        private Messages() {
        }
    }

    public static class Text extends net.blueva.api.text.Text {
        private Text() {
        }
    }

    public static class Events extends net.blueva.api.events.Events {
        private Events() {
        }
    }
}
