package org.tron.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import org.tron.common.parameter.CommonParameter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import lombok.Getter;


public class TronCache<K, V> {

  @Getter
  private final CacheType name;
  private final Cache<K, V> cache;
  private final com.github.benmanes.caffeine.cache.Cache<K, V> caffeineCache;
  private boolean isCaffeine = CommonParameter.getInstance().caffeineCacheActive;

  TronCache(CacheType name, String strategy) {
    this.name = name;
    if (isCaffeine) {
      this.cache = null;
      strategy = castGuavaSpec2Caffeine(strategy);
      this.caffeineCache = Caffeine.from(strategy).build();
    } else {
      this.cache = CacheBuilder.from(strategy).build();
      this.caffeineCache = null;
    }
  }

  TronCache(CacheType name, String strategy, CacheLoader<K, V> loader) {
    this.name = name;
    this.caffeineCache = null;
    this.cache = CacheBuilder.from(strategy).build(loader);
  }

  public void put(K k, V v) {
    if(isCaffeine) {
      this.caffeineCache.put(k, v);
    } else {
      this.cache.put(k, v);
    }
  }

  public V getIfPresent(K k) {
    if(isCaffeine) {
      return this.caffeineCache.getIfPresent(k);
    } else {
      return this.cache.getIfPresent(k);
    }
  }

  public V get(K k, Callable<? extends V> loader) throws ExecutionException {
    if(isCaffeine) {
      return this.caffeineCache.get(k, (Function<? super K, ? extends V>) loader);
    } else {
      return this.cache.get(k, loader);
    }
  }

  public CacheStats stats() {
    if(isCaffeine){
      return CommonCacheStat.castCaffeineToGuavaStat(this.caffeineCache.stats());
    } else {
      return this.cache.stats();
    }
  }

  public void invalidateAll() {
    if (isCaffeine) {
      this.caffeineCache.invalidateAll();
    } else {
      this.cache.invalidateAll();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TronCache<?, ?> tronCache = (TronCache<?, ?>) o;
    return Objects.equal(name, tronCache.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  private static String castGuavaSpec2Caffeine(String guavaSpec) {
    int beforeConcurrencyLevel = guavaSpec.indexOf(",concurrencyLevel");
    int beforeRecordStats = guavaSpec.indexOf(",recordStats");
    String ret = guavaSpec.substring(0, beforeConcurrencyLevel) + guavaSpec.substring(beforeRecordStats, guavaSpec.length());
    return ret;
  }
}
