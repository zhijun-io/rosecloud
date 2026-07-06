package io.rosecloud.starter.storage;

import java.io.InputStream;

/**
 * Blob storage abstraction keyed by a relative path. The local default writes to
 * the filesystem; consumers needing object storage (S3/OSS) provide their own
 * bean (which takes precedence via {@code @ConditionalOnMissingBean}). Callers
 * manage keys/filenames; content is opaque bytes.
 */
public interface FileStorage {

    void store(String key, InputStream content);

    InputStream load(String key);

    boolean exists(String key);

    void delete(String key);
}
