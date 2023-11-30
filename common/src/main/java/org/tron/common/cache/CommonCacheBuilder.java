package org.tron.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;
import org.tron.common.parameter.CommonParameter;

import java.util.concurrent.TimeUnit;

public class CommonCacheBuilder<K, V> {
  private  boolean isCaffeine = CommonParameter.getInstance().caffeineCacheActive;
  private  int initialCapacity = 0;
  private  long maximumSize = 0L;
  private  long expireAfterWrite = 0L;
  private  TimeUnit timeUnit;
  private  boolean recordStats = false;

  public CommonCacheBuilder initialCapacity(int initialCapacity) {
    this.initialCapacity = initialCapacity;
    return this;
  }

  public CommonCacheBuilder maximumSize(long maximumSize) {
    this.maximumSize = maximumSize;
    return this;
  }

  public CommonCacheBuilder expireAfterWrite(long expireAfterWrite,
                                             TimeUnit timeUnit) {
    this.expireAfterWrite = expireAfterWrite;
    this.timeUnit = timeUnit;
    return this;
  }

  public CommonCacheBuilder recordStats() {
    this.recordStats = true;
    return this;
  }

  public CommonCache<K, V> build() {
    //check
    if (isCaffeine)  {
      com.github.benmanes.caffeine.cache.Caffeine caffeine = initCaffeineCacheBuilder(
          initialCapacity, maximumSize, expireAfterWrite, timeUnit, recordStats
      );
      com.github.benmanes.caffeine.cache.Cache caffeineCache = caffeine.build();
      return new CommonCache(caffeineCache, null);
    } else {
      com.google.common.cache.CacheBuilder cacheBuilder = initGuavaCacheBuilder(
          initialCapacity, maximumSize, expireAfterWrite, timeUnit, recordStats
      );
      com.google.common.cache.Cache guavaCache = cacheBuilder.build();
      return new CommonCache(null, guavaCache);
    }
  }


  private com.google.common.cache.CacheBuilder initGuavaCacheBuilder(int initialCapacity
      , long maximumSize
      , long expireAfterWrite
      , TimeUnit timeUnit
      , boolean recordStats) {
    com.google.common.cache.CacheBuilder cacheBuilder = CacheBuilder.newBuilder();
    if (initialCapacity > 0) {
      cacheBuilder = cacheBuilder.initialCapacity(initialCapacity);
    }
    if (maximumSize > 0) {
      cacheBuilder = cacheBuilder.maximumSize(maximumSize);
    }
    if (expireAfterWrite > 0) {
      cacheBuilder = cacheBuilder.expireAfterWrite(expireAfterWrite, timeUnit);
    }
    if (recordStats) {
      cacheBuilder = cacheBuilder.recordStats();
    }
    return cacheBuilder;
  }

  private com.github.benmanes.caffeine.cache.Caffeine initCaffeineCacheBuilder(int initialCapacity
      , long maximumSize
      , long expireAfterWrite
      , TimeUnit timeUnit
      , boolean recordStats) {
    com.github.benmanes.caffeine.cache.Caffeine caffeine = Caffeine.newBuilder();
    if (initialCapacity > 0) {
      caffeine = caffeine.initialCapacity(initialCapacity);
    }
    if (maximumSize > 0) {
      caffeine = caffeine.maximumSize(maximumSize);
    }
    if (expireAfterWrite > 0) {
      caffeine = caffeine.expireAfterWrite(expireAfterWrite, timeUnit);
    }
    if (recordStats) {
      caffeine = caffeine.recordStats();
    }
    return caffeine;
  }

  public static CommonCacheBuilder<Object, Object> newBuilder() {
    return new CommonCacheBuilder<>();
  }
}
