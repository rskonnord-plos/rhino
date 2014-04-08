package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * {@link com.google.common.cache.Cache} that retains all values in a map on the heap, never evicts, and does not save
 * any stats. For tests only!
 */
public class TestGuavaCache<K, V> implements Cache<K, V> {

  private final ConcurrentMap<K, V> map = new ConcurrentHashMap<>();

  public TestGuavaCache() {
  }

  @Nullable
  @Override
  public V getIfPresent(Object key) {
    return map.get(key);
  }

  @Override
  public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
    V value = getIfPresent(key);
    if (value != null) {
      return value;
    }

    try {
      value = valueLoader.call();
      Preconditions.checkNotNull(value); // required by Cache.get contract
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
    map.put(key, value);
    return value;
  }

  @Override
  public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
    ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
    for (Object key : keys) {
      V value = getIfPresent(key);
      if (value != null) {
        builder.put((K) key, value);
      }
    }
    return builder.build();
  }

  @Override
  public void put(K key, V value) {
    map.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void invalidate(Object key) {
    map.remove(key);
  }

  @Override
  public void invalidateAll(Iterable<?> keys) {
    for (Object key : keys) {
      invalidate(key);
    }
  }

  @Override
  public void invalidateAll() {
    map.clear();
  }

  @Override
  public long size() {
    return map.size();
  }

  @Override
  public CacheStats stats() {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " does not save any stats");
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    return map;
  }

  @Override
  public void cleanUp() {
  }
}
