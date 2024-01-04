package org.tron.common.cache;

import com.google.errorprone.annotations.CompatibleWith;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.parameter.CommonParameter;

import java.util.concurrent.ConcurrentMap;

@Slf4j(topic = "app")
public class CommonCache<K, V> {

  private com.github.benmanes.caffeine.cache.Cache caffeineCache;
  private com.google.common.cache.Cache guavaCache;
  private boolean isCaffeine = CommonParameter.getInstance().caffeineCacheActive;

  public CommonCache(com.github.benmanes.caffeine.cache.Cache caffeineCache,
                     com.google.common.cache.Cache guavaCache) {
    if (isCaffeine) {
      this.caffeineCache = caffeineCache;
      this.guavaCache = null;
    } else {
      this.caffeineCache = null;
      this.guavaCache = guavaCache;
    }
  }

  public V getIfPresent(@CompatibleWith("K") Object key) {
    if (isCaffeine) {
      logger.info("isCaffeine");
      return (V)caffeineCache.getIfPresent(key);
    } else {
      logger.info("isGuava");
      return (V)guavaCache.getIfPresent(key);
    }
  }

  public void put(K key, V value) {
    if (isCaffeine) {
      logger.info("isCaffeine");
      caffeineCache.put(key, value);
    } else {
      logger.info("isGuava");
      guavaCache.put(key, value);
    }
  }

  public void invalidate(K key) {
    if (isCaffeine) {
      caffeineCache.invalidate(key);
    } else {
      guavaCache.invalidate(key);
    }
  }

  public ConcurrentMap<K, V> asMap() {
    if (isCaffeine) {
      return caffeineCache.asMap();
    } else {
      return guavaCache.asMap();
    }
  }


  public void cleanUp() {
    if (isCaffeine) {
      caffeineCache.cleanUp();
    } else {
      guavaCache.cleanUp();
    }
  }

  public long size() {
    if (isCaffeine) {
      return caffeineCache.estimatedSize();
    } else {
      return guavaCache.size();
    }
  }

}
