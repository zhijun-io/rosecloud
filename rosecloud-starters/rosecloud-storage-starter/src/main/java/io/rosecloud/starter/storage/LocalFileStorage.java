package io.rosecloud.starter.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Local-filesystem {@link FileStorage}. Content is stored under a configured
 * base directory, keyed by a relative path (e.g. {@code "tenant/1/avatar.png"}).
 * Keys are normalized and confined to the base directory to prevent path
 * traversal. Active when {@code rosecloud.storage.type=local} (the default);
 * consumers needing S3/OSS provide their own {@link FileStorage} bean.
 */
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file: " + key, e);
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load file: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file: " + key, e);
        }
    }

    private Path resolve(String key) {
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storage key (escapes base dir): " + key);
        }
        return target;
    }
}
