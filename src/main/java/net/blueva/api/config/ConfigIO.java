package net.blueva.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

final class ConfigIO {
    private ConfigIO() {
    }

    static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    static void writeAtomic(Path path, String text) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Files.write(tmp, text.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Path backup(Path file, Path cacheFile) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        Path root = cacheFile.getParent() == null ? file.getParent() : cacheFile.getParent().getParent();
        if (root == null) {
            root = file.getParent();
        }
        Path directory = (root == null ? file.toAbsolutePath().getParent() : root).resolve("config-backups");
        Files.createDirectories(directory);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
        Path target = directory.resolve(file.getFileName().toString() + "." + stamp + ".bak");
        Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
        return target;
    }
}
