package org.tron.common.cache;

import com.google.common.cache.CacheStats;
import lombok.Getter;

public class CommonCacheStat {
  @Getter
  private CacheStats guavaCacheStats;
  @Getter
  private com.github.benmanes.caffeine.cache.stats.CacheStats caffeineCacheStats;
  public CommonCacheStat(CacheStats guavaCacheStats,
                         com.github.benmanes.caffeine.cache.stats.CacheStats caffeineCacheStats) {
    this.guavaCacheStats = guavaCacheStats;
    this.caffeineCacheStats = caffeineCacheStats;
  }


  public static CacheStats castCaffeineToGuavaStat(com.github.benmanes.caffeine.cache.stats.CacheStats caffeineCacheStats) {
    CacheStats guavaCacheStats = new CacheStats(
        caffeineCacheStats.hitCount(), caffeineCacheStats.missCount(),
        caffeineCacheStats.loadSuccessCount(), caffeineCacheStats.loadFailureCount(),
        caffeineCacheStats.totalLoadTime(), caffeineCacheStats.evictionCount()
    );
    return  guavaCacheStats;
  }
}
