package net.blueva.foundation.version;

import org.bukkit.Bukkit;

/** Minecraft/Bukkit version utilities. */
public class Version {

    protected Version() {
    }

    public static MinecraftVersion current() {
        return MinecraftVersion.parse(bukkitVersion());
    }

    public static String bukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Throwable ignored) {
            return "0.0";
        }
    }

    public static String craftBukkitPackage() {
        try {
            Package serverPackage = Bukkit.getServer().getClass().getPackage();
            return serverPackage == null ? "" : serverPackage.getName();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static String craftBukkitRevision() {
        String packageName = craftBukkitPackage();
        int index = packageName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        String revision = packageName.substring(index + 1);
        return revision.startsWith("v") ? revision : "";
    }

    public static boolean isAtLeast(int major, int minor) {
        return current().isAtLeast(major, minor);
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        return current().isAtLeast(major, minor, patch);
    }

    public static boolean isOlderThan(int major, int minor) {
        return current().isOlderThan(major, minor);
    }

    public static boolean isOlderThan(int major, int minor, int patch) {
        return current().isOlderThan(major, minor, patch);
    }

    public static boolean isLegacy() {
        MinecraftVersion version = current();
        return version.getMajor() == 1 && version.getMinor() <= 12;
    }

    /** Parsed Minecraft version. Examples: 1.8.8, 1.21.11, 26.1. */
    public static class MinecraftVersion implements Comparable<MinecraftVersion> {
        private final int major;
        private final int minor;
        private final int patch;
        private final String raw;

        protected MinecraftVersion(int major, int minor, int patch, String raw) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.raw = raw;
        }

        public static MinecraftVersion parse(String version) {
            if (version == null) {
                return new MinecraftVersion(0, 0, 0, "");
            }

            String raw = version;
            String clean = version.trim();
            int dash = clean.indexOf('-');
            if (dash != -1) {
                clean = clean.substring(0, dash);
            }

            String[] parts = clean.split("\\.");
            int major = parsePart(parts, 0);
            int minor = parsePart(parts, 1);
            int patch = parsePart(parts, 2);
            return new MinecraftVersion(major, minor, patch, raw);
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }

        public String getRaw() {
            return raw;
        }

        public boolean isAtLeast(int major, int minor) {
            return isAtLeast(major, minor, 0);
        }

        public boolean isAtLeast(int major, int minor, int patch) {
            return compareTo(new MinecraftVersion(major, minor, patch, major + "." + minor + "." + patch)) >= 0;
        }

        public boolean isOlderThan(int major, int minor) {
            return isOlderThan(major, minor, 0);
        }

        public boolean isOlderThan(int major, int minor, int patch) {
            return compareTo(new MinecraftVersion(major, minor, patch, major + "." + minor + "." + patch)) < 0;
        }

        @Override
        public int compareTo(MinecraftVersion other) {
            if (major != other.major) {
                return major - other.major;
            }
            if (minor != other.minor) {
                return minor - other.minor;
            }
            return patch - other.patch;
        }

        @Override
        public String toString() {
            if (patch > 0) {
                return major + "." + minor + "." + patch;
            }
            return major + "." + minor;
        }

        private static int parsePart(String[] parts, int index) {
            if (parts == null || parts.length <= index) {
                return 0;
            }
            String part = parts[index];
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c >= '0' && c <= '9') {
                    digits.append(c);
                } else {
                    break;
                }
            }
            if (digits.length() == 0) {
                return 0;
            }
            try {
                return Integer.parseInt(digits.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
