package com.jproxy.core;

import com.jproxy.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache {
    
    private static final Logger logger = LoggerFactory.getLogger(Cache.class);
    
    private final int maxSize;
    private final LinkedHashMap<String, CacheEntry> cache;
    private final ReadWriteLock lock;
    
    // Stats
    private long hits = 0;
    private long misses = 0;
    
    public Cache(int maxSize) {
        this.maxSize = maxSize;
        this.lock = new ReentrantReadWriteLock();
        
        // LRU cache using LinkedHashMap with access order
        this.cache = new LinkedHashMap<String, CacheEntry>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean shouldRemove = size() > Cache.this.maxSize;
                if (shouldRemove) {
                    logger.debug("Evicting cache entry: {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
        
        logger.info("Cache initialized with max size: {}", maxSize);
    }
    
    public HttpResponse get(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            
            if (entry == null) {
                misses++;
                logger.debug("Cache MISS: {}", key);
                return null;
            }
            
            // Check if expired
            if (entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    misses++;
                    logger.debug("Cache EXPIRED: {}", key);
                    return null;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            
            hits++;
            logger.debug("Cache HIT: {}", key);
            return entry.getResponse();
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void put(String key, HttpResponse response, long ttlMillis) {
        lock.writeLock().lock();
        try {
            long expiresAt = System.currentTimeMillis() + ttlMillis;
            CacheEntry entry = new CacheEntry(response, expiresAt);
            cache.put(key, entry);
            logger.debug("Cache PUT: {} (TTL: {}ms)", key, ttlMillis);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void invalidate(String key) {
        lock.writeLock().lock();
        try {
            cache.remove(key);
            logger.debug("Cache INVALIDATE: {}", key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            logger.info("Cache cleared ({} entries)", size);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public CacheStats getStats() {
        lock.readLock().lock();
        try {
            return new CacheStats(hits, misses, cache.size(), maxSize);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // Inner class for cache entries
    private static class CacheEntry {
        private final HttpResponse response;
        private final long expiresAt;
        
        public CacheEntry(HttpResponse response, long expiresAt) {
            this.response = response;
            this.expiresAt = expiresAt;
        }
        
        public HttpResponse getResponse() {
            return response;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    // Stats class
    public static class CacheStats {
        private final long hits;
        private final long misses;
        private final int size;
        private final int maxSize;
        
        public CacheStats(long hits, long misses, int size, int maxSize) {
            this.hits = hits;
            this.misses = misses;
            this.size = size;
            this.maxSize = maxSize;
        }
        
        public long getHits() {
            return hits;
        }
        
        public long getMisses() {
            return misses;
        }
        
        public int getSize() {
            return size;
        }
        
        public int getMaxSize() {
            return maxSize;
        }
        
        public long getTotal() {
            return hits + misses;
        }
        
        public double getHitRate() {
            long total = getTotal();
            return total == 0 ? 0.0 : (double) hits / total * 100;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Cache Stats: %d/%d entries, %d hits, %d misses, %.2f%% hit rate",
                size, maxSize, hits, misses, getHitRate()
            );
        }
    }
}