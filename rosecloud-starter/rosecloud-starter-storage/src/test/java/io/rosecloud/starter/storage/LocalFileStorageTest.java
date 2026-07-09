package io.rosecloud.starter.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link LocalFileStorage}: store/load round-trip with nested keys,
 * exists, delete, and path-traversal confinement.
 */
class LocalFileStorageTest {

    @TempDir
    Path baseDir;

    @Test
    void storeLoadExistsAndDelete() throws IOException {
        LocalFileStorage storage = new LocalFileStorage(baseDir);

        assertThat(storage.exists("docs/readme.txt")).isFalse();

        storage.store("docs/readme.txt", new ByteArrayInputStream("hello".getBytes()));
        assertThat(storage.exists("docs/readme.txt")).isTrue();

        try (InputStream in = storage.load("docs/readme.txt")) {
            assertThat(in.readAllBytes()).isEqualTo("hello".getBytes());
        }

        storage.delete("docs/readme.txt");
        assertThat(storage.exists("docs/readme.txt")).isFalse();
    }

    @Test
    void rejectsPathTraversal() {
        LocalFileStorage storage = new LocalFileStorage(baseDir);

        assertThatThrownBy(() -> storage.store("../escape.txt", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
