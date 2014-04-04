package org.ambraproject.rhino.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * {@link com.google.common.cache.Cache} that never stores or returns anything.
 */
public class NullGuavaCache<K, V> implements Cache<K, V> {
  private NullGuavaCache() {
  }

  private static final NullGuavaCache<Object, Object> INSTANCE = new NullGuavaCache<>();

  public static <K, V> NullGuavaCache<K, V> getInstance() {
    return (NullGuavaCache<K, V>) INSTANCE;
  }

  @Nullable
  @Override
  public V getIfPresent(Object key) {
    return null;
  }

  @Override
  public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
    try {
      return valueLoader.call();
    } catch (Exception e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
    return ImmutableMap.of();
  }

  @Override
  public void put(K key, V value) {
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
  }

  @Override
  public void invalidate(Object key) {
  }

  @Override
  public void invalidateAll(Iterable<?> keys) {
  }

  @Override
  public void invalidateAll() {
  }

  @Override
  public long size() {
    return 0;
  }

  @Override
  public CacheStats stats() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ConcurrentMap<K, V> asMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void cleanUp() {
  }
}
