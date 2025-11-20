package com.jproxy.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, TokenBucket> buckets;
    
    public RateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.buckets = new ConcurrentHashMap<>();
        
        logger.info("Rate limiter initialized: {} requests per {} seconds", 
            maxRequests, windowSeconds);
    }
    
    public RateLimitResult allowRequest(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxRequests, windowMillis));
        return bucket.tryConsume();
    }
    
    public RateLimitStats getStats(String key) {
        TokenBucket bucket = buckets.get(key);
        if (bucket == null) {
            return new RateLimitStats(maxRequests, maxRequests, 0, windowMillis);
        }
        return bucket.getStats();
    }
    
    public void reset(String key) {
        buckets.remove(key);
        logger.debug("Rate limit reset for key: {}", key);
    }
    
    public void resetAll() {
        int size = buckets.size();
        buckets.clear();
        logger.info("All rate limits reset ({} keys)", size);
    }
    
    // Token Bucket implementation
    private static class TokenBucket {
        private final int capacity;
        private final long windowMillis;
        private final ReentrantLock lock;
        
        private int availableTokens;
        private long windowStart;
        private long totalRequests;
        private long rejectedRequests;
        
        public TokenBucket(int capacity, long windowMillis) {
            this.capacity = capacity;
            this.windowMillis = windowMillis;
            this.lock = new ReentrantLock();
            this.availableTokens = capacity;
            this.windowStart = System.currentTimeMillis();
            this.totalRequests = 0;
            this.rejectedRequests = 0;
        }
        
        public RateLimitResult tryConsume() {
            lock.lock();
            try {
                long now = System.currentTimeMillis();
                
                // Check if we need to reset the window
                if (now - windowStart >= windowMillis) {
                    availableTokens = capacity;
                    windowStart = now;
                }
                
                totalRequests++;
                
                if (availableTokens > 0) {
                    availableTokens--;
                    long resetAt = windowStart + windowMillis;
                    return RateLimitResult.allowed(availableTokens, capacity, resetAt);
                } else {
                    rejectedRequests++;
                    long resetAt = windowStart + windowMillis;
                    long retryAfter = (resetAt - now) / 1000; // seconds
                    return RateLimitResult.rejected(0, capacity, resetAt, retryAfter);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public RateLimitStats getStats() {
            lock.lock();
            try {
                return new RateLimitStats(
                    availableTokens,
                    capacity,
                    totalRequests,
                    rejectedRequests,
                    windowMillis
                );
            } finally {
                lock.unlock();
            }
        }
    }
    
    // Result of rate limit check
    public static class RateLimitResult {
        private final boolean allowed;
        private final int remaining;
        private final int limit;
        private final long resetAt;
        private final long retryAfter;
        
        private RateLimitResult(boolean allowed, int remaining, int limit, long resetAt, long retryAfter) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.limit = limit;
            this.resetAt = resetAt;
            this.retryAfter = retryAfter;
        }
        
        public static RateLimitResult allowed(int remaining, int limit, long resetAt) {
            return new RateLimitResult(true, remaining, limit, resetAt, 0);
        }
        
        public static RateLimitResult rejected(int remaining, int limit, long resetAt, long retryAfter) {
            return new RateLimitResult(false, remaining, limit, resetAt, retryAfter);
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public long getResetAt() {
            return resetAt;
        }
        
        public long getRetryAfter() {
            return retryAfter;
        }
    }
    
    // Statistics
    public static class RateLimitStats {
        private final int remaining;
        private final int limit;
        private final long totalRequests;
        private final long rejectedRequests;
        private final long windowMillis;
        
        public RateLimitStats(int remaining, int limit, long totalRequests, long windowMillis) {
            this(remaining, limit, totalRequests, 0, windowMillis);
        }
        
        public RateLimitStats(int remaining, int limit, long totalRequests, long rejectedRequests, long windowMillis) {
            this.remaining = remaining;
            this.limit = limit;
            this.totalRequests = totalRequests;
            this.rejectedRequests = rejectedRequests;
            this.windowMillis = windowMillis;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public long getTotalRequests() {
            return totalRequests;
        }
        
        public long getRejectedRequests() {
            return rejectedRequests;
        }
        
        public long getWindowMillis() {
            return windowMillis;
        }
        
        public double getRejectionRate() {
            return totalRequests == 0 ? 0.0 : (double) rejectedRequests / totalRequests * 100;
        }
    }
}