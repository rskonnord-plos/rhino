package org.ambraproject.rhino.mocks;

import org.ambraproject.service.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Created by jkrzemien on 8/12/14.
 */
public enum MockCacheFactory {

  SINGLETON;

  private final Logger log = LoggerFactory.getLogger(MockCache.class);

  public static MockCacheFactory getInstance() {
    return SINGLETON;
  }

  public Cache getCache() {
    log.info("Retrieving a NEW mock cache from factory!");
    return new MockCache();
  }

  class MockCache implements Cache {

    private final Logger log = LoggerFactory.getLogger(MockCache.class);
    private final Map<Object, Item> cache = new ConcurrentHashMap<Object, Item>();

    @Override
    public Item get(Object o) {
      log.info(format("Attempting to retrieve item [%s] from mock cache", o.toString()));
      return cache.get(o);
    }

    @Override
    public <T, E extends Exception> T get(Object o, int i, Lookup<T, E> teLookup) throws E {
      return (T) get(o);
    }

    @Override
    public <T, E extends Exception> T get(Object o, Lookup<T, E> teLookup) throws E {
      return (T) get(o);
    }

    @Override
    public void put(Object o, Item item) {
      log.info(format("Putting item [%s] into mock cache with key [%s]", item.toString(), o.toString()));
      cache.put(o, item);
    }

    @Override
    public void remove(Object o) {
      log.info(format("Attempting to remove item [%s] from mock cache", o.toString()));
      cache.remove(o);
    }

    @Override
    public void removeAll() {
      log.info("Clearing all mock cache content");
      cache.clear();
    }
  }
}
