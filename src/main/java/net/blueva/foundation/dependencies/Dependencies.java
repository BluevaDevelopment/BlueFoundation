package net.blueva.foundation.dependencies;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Runtime dependency manager. */
public class Dependencies {

    protected Dependencies() {
    }

    public static Loader loader(JavaPlugin plugin) {
        return new Loader(plugin);
    }

    public static Loader loader(Path dataFolder, ClassLoader classLoader, Logger logger) {
        return new Loader(dataFolder, classLoader, logger);
    }

    public static void load(JavaPlugin plugin, Collection<? extends RuntimeDependency> dependencies) {
        loader(plugin).loadAll(dependencies);
    }

    public static void load(JavaPlugin plugin, RuntimeDependency dependency) {
        loader(plugin).load(dependency);
    }

    /**
     * Previously loaded the Adventure stack required by BlueFoundation text/message
     * utilities. This is now a no-op: BlueFoundation provides its own MiniMessage
     * parser and legacy serializers that do not require Adventure at runtime.
     *
     * @deprecated kept for backwards compatibility; does nothing.
     */
    @Deprecated
    public static void loadAdventure(JavaPlugin plugin) {
        // Adventure is no longer required at runtime.
    }

    /**
     * @return an empty list; Adventure is no longer required at runtime.
     * @deprecated BlueFoundation no longer needs Adventure on Spigot/Bukkit.
     */
    @Deprecated
    public static List<RuntimeDependency> adventureDependencies() {
        return new ArrayList<>();
    }

    public static RuntimeDependency of(String groupId, String artifactId, String version) {
        return new RuntimeDependency(groupId, artifactId, version);
    }

    public static RuntimeDependency of(String groupId, String artifactId, String version, String repositoryUrl) {
        return new RuntimeDependency(groupId, artifactId, version, repositoryUrl);
    }

    public static RuntimeDependency mavenCentral(String groupId, String artifactId, String version) {
        return new RuntimeDependency(groupId, artifactId, version, Repositories.MAVEN_CENTRAL);
    }

    public static RuntimeDependency codeMc(String groupId, String artifactId, String version) {
        return new RuntimeDependency(groupId, artifactId, version, Repositories.CODEMC);
    }

    public static RuntimeDependency jitPack(String groupId, String artifactId, String version) {
        return new RuntimeDependency(groupId, artifactId, version, Repositories.JITPACK);
    }

    public static List<RuntimeDependency> list(RuntimeDependency... dependencies) {
        return Arrays.asList(dependencies);
    }

    /** Common Maven repository URLs. */
    public static class Repositories {
        public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
        public static final String CODEMC = "https://repo.codemc.io/repository/maven-public/";
        public static final String JITPACK = "https://jitpack.io/";

        protected Repositories() {
        }
    }

    /** Versions used by BlueFoundation runtime dependency profiles. */
    public static class Versions {
        public static final String ADVENTURE_LEGACY = "4.26.1";
        public static final String ADVENTURE_MODERN = "5.1.1";
        public static final String ADVENTURE_PLATFORM = "4.4.1";
        public static final String EXAMINATION = "1.3.0";

        protected Versions() {
        }
    }

    /** Descriptor for a Maven artifact downloaded at runtime. */
    public static class RuntimeDependency {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String repositoryUrl;

        public RuntimeDependency(String groupId, String artifactId, String version) {
            this(groupId, artifactId, version, Repositories.MAVEN_CENTRAL);
        }

        public RuntimeDependency(String groupId, String artifactId, String version, String repositoryUrl) {
            if (isBlank(groupId)) {
                throw new IllegalArgumentException("groupId cannot be blank");
            }
            if (isBlank(artifactId)) {
                throw new IllegalArgumentException("artifactId cannot be blank");
            }
            if (isBlank(version)) {
                throw new IllegalArgumentException("version cannot be blank");
            }

            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.repositoryUrl = isBlank(repositoryUrl) ? Repositories.MAVEN_CENTRAL : repositoryUrl;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getVersion() {
            return version;
        }

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public String getResolvedRepositoryUrl() {
            return repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + "/";
        }

        public String getFileName() {
            return artifactId + "-" + version + ".jar";
        }

        public String getCoordinates() {
            return groupId + ":" + artifactId + ":" + version;
        }

        public String getMavenPath() {
            return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + getFileName();
        }

        public String getDownloadUrl() {
            return getResolvedRepositoryUrl() + getMavenPath();
        }

        @Override
        public String toString() {
            return getCoordinates();
        }
    }

    /** Runtime dependency downloader and classpath injector. */
    public static class Loader {
        private final Path librariesDirectory;
        private final URLClassLoader classLoader;
        private final Logger logger;

        public Loader(JavaPlugin plugin) {
            this(plugin.getDataFolder().toPath(), plugin.getClass().getClassLoader(), plugin.getLogger());
        }

        public Loader(Path dataFolder, ClassLoader classLoader, Logger logger) {
            this.librariesDirectory = dataFolder.resolve("libraries");
            if (!(classLoader instanceof URLClassLoader)) {
                throw new IllegalStateException("Unsupported plugin classloader: " + classLoader.getClass().getName());
            }
            this.classLoader = (URLClassLoader) classLoader;
            this.logger = logger;
        }

        public Path getLibrariesDirectory() {
            return librariesDirectory;
        }

        public void load(String groupId, String artifactId, String version) {
            load(new RuntimeDependency(groupId, artifactId, version));
        }

        public void load(String groupId, String artifactId, String version, String repositoryUrl) {
            load(new RuntimeDependency(groupId, artifactId, version, repositoryUrl));
        }

        public void load(RuntimeDependency dependency) {
            try {
                Path jar = download(dependency);
                inject(jar.toUri().toURL());
            } catch (Exception exception) {
                throw new RuntimeException("Failed to load runtime dependency " + dependency.getCoordinates(), exception);
            }
        }

        public void loadAll(Collection<? extends RuntimeDependency> dependencies) {
            cleanStaleJars(librariesDirectory, dependencies);
            for (RuntimeDependency dependency : dependencies) {
                load(dependency);
            }
        }

        public Path download(RuntimeDependency dependency) throws IOException {
            Files.createDirectories(librariesDirectory);

            Path target = librariesDirectory.resolve(dependency.getFileName());
            if (Files.exists(target) && Files.size(target) > 0L) {
                return target;
            }

            String url = dependency.getDownloadUrl();
            if (logger != null) {
                logger.info("Downloading runtime dependency " + dependency.getCoordinates());
            }

            try (InputStream inputStream = new URL(url).openStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(target) || Files.size(target) == 0L) {
                Files.deleteIfExists(target);
                throw new IOException("Downloaded empty runtime dependency: " + dependency.getCoordinates());
            }

            return target;
        }

        public static void cleanStaleJars(Path librariesDirectory, Collection<? extends RuntimeDependency> dependencies) {
            if (!Files.isDirectory(librariesDirectory)) {
                return;
            }

            Set<String> keep = new HashSet<>();
            for (RuntimeDependency dependency : dependencies) {
                keep.add(dependency.getFileName());
            }

            try (Stream<Path> files = Files.list(librariesDirectory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> !keep.contains(path.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }

        private void inject(URL url) throws Exception {
            for (URL existing : classLoader.getURLs()) {
                if (existing.equals(url)) {
                    return;
                }
            }

            try {
                Method method = classLoader.getClass().getMethod("addURL", URL.class);
                method.invoke(classLoader, url);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, url);
                return;
            } catch (Exception ignored) {
            }

            injectViaUnsafe(url);
        }

        @SuppressWarnings("unchecked")
        private void injectViaUnsafe(URL url) throws Exception {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);

            Method objectFieldOffset = unsafeClass.getMethod("objectFieldOffset", Field.class);
            Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
            Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);

            Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
            long ucpOffset = ((Number) objectFieldOffset.invoke(unsafe, ucpField)).longValue();
            Object ucp = getObject.invoke(unsafe, classLoader, ucpOffset);

            Field pathField = ucp.getClass().getDeclaredField("path");
            long pathOffset = ((Number) objectFieldOffset.invoke(unsafe, pathField)).longValue();
            List<URL> path = (List<URL>) getObject.invoke(unsafe, ucp, pathOffset);
            if (path == null) {
                path = new ArrayList<>();
                putObject.invoke(unsafe, ucp, pathOffset, path);
            }

            synchronized (path) {
                if (!path.contains(url)) {
                    path.add(url);
                }

                try {
                    Field unopenedField = ucp.getClass().getDeclaredField("unopenedUrls");
                    long unopenedOffset = ((Number) objectFieldOffset.invoke(unsafe, unopenedField)).longValue();
                    Object unopened = getObject.invoke(unsafe, ucp, unopenedOffset);
                    if (unopened instanceof Collection) {
                        Collection<URL> collection = (Collection<URL>) unopened;
                        if (!collection.contains(url)) {
                            collection.add(url);
                        }
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean nativeAdventureAudienceAvailable() {
        try {
            Class<?> audience = Class.forName("net.kyori.adventure.audience.Audience", false, Dependencies.class.getClassLoader());
            Class<?> sender = Class.forName("org.bukkit.command.CommandSender", false, Dependencies.class.getClassLoader());
            return audience.isAssignableFrom(sender);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, Dependencies.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String adventureCoreRuntimeVersion() {
        return isModernAdventureRuntime() ? Versions.ADVENTURE_MODERN : Versions.ADVENTURE_LEGACY;
    }

    /**
     * Adventure 5.x is Java 21 bytecode. Use it only on modern Java/Minecraft
     * runtimes; keep Adventure 4.x for old Bukkit/Spigot compatibility.
     */
    private static boolean isModernAdventureRuntime() {
        return isJavaAtLeast(21) && isMinecraftAtLeast(1, 21);
    }

    private static boolean isJavaAtLeast(int required) {
        String version = System.getProperty("java.specification.version", "0");
        int major;
        if (version.startsWith("1.")) {
            major = parseInt(version.substring(2), 0);
        } else {
            int dot = version.indexOf('.');
            major = parseInt(dot == -1 ? version : version.substring(0, dot), 0);
        }
        return major >= required;
    }

    private static boolean isMinecraftAtLeast(int requiredMajor, int requiredMinor) {
        try {
            String version = org.bukkit.Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = version.split("\\.");
            int major = parts.length > 0 ? parseInt(parts[0], 0) : 0;
            int minor = parts.length > 1 ? parseInt(parts[1], 0) : 0;

            if (major != requiredMajor) {
                return major > requiredMajor;
            }
            return minor >= requiredMinor;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
