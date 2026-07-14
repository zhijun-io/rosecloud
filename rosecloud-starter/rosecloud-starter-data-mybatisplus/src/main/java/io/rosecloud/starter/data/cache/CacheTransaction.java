package io.rosecloud.starter.data.cache;

import java.io.Serializable;

public interface CacheTransaction<K extends Serializable, V> {
    void put(K key, V value);
    boolean commit();
    void rollback();
}
