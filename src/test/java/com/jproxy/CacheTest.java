package com.jproxy;

import com.jproxy.core.Cache;
import com.jproxy.model.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {
    
    private Cache cache;
    
    @BeforeEach
    public void setUp() {
        cache = new Cache(3); // Small cache for testing
    }
    
    @Test
    public void testPutAndGet() {
        HttpResponse response = new HttpResponse(200);
        response.setBody("test body".getBytes());
        
        cache.put("key1", response, 10000);
        
        HttpResponse retrieved = cache.get("key1");
        assertNotNull(retrieved);
        assertEquals(200, retrieved.getStatusCode());
        assertArrayEquals("test body".getBytes(), retrieved.getBody());
    }
    
    @Test
    public void testCacheMiss() {
        HttpResponse response = cache.get("nonexistent");
        assertNull(response);
    }
    
    @Test
    public void testExpiration() throws InterruptedException {
        HttpResponse response = new HttpResponse(200);
        cache.put("key1", response, 100); // 100ms TTL
        
        assertNotNull(cache.get("key1"));
        
        Thread.sleep(150); // Wait for expiration
        
        assertNull(cache.get("key1"));
    }
    
    @Test
    public void testLRUEviction() {
        HttpResponse response1 = new HttpResponse(200);
        HttpResponse response2 = new HttpResponse(200);
        HttpResponse response3 = new HttpResponse(200);
        HttpResponse response4 = new HttpResponse(200);
        
        cache.put("key1", response1, 10000);
        cache.put("key2", response2, 10000);
        cache.put("key3", response3, 10000);
        
        assertEquals(3, cache.size());
        
        // Adding 4th item should evict key1 (oldest)
        cache.put("key4", response4, 10000);
        
        assertEquals(3, cache.size());
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
        assertNotNull(cache.get("key4"));
    }
    
    @Test
    public void testClear() {
        cache.put("key1", new HttpResponse(200), 10000);
        cache.put("key2", new HttpResponse(200), 10000);
        
        assertEquals(2, cache.size());
        
        cache.clear();
        
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }
    
    @Test
    public void testStats() {
        cache.put("key1", new HttpResponse(200), 10000);
        
        cache.get("key1"); // Hit
        cache.get("key1"); // Hit
        cache.get("key2"); // Miss
        
        Cache.CacheStats stats = cache.getStats();
        assertEquals(2, stats.getHits());
        assertEquals(1, stats.getMisses());
        assertEquals(3, stats.getTotal());
        assertTrue(stats.getHitRate() > 66.0 && stats.getHitRate() < 67.0);
    }
}