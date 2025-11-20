package com.jproxy;

import com.jproxy.core.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    public void setUp() {
        rateLimiter = new RateLimiter(5, 1); // 5 requests per 1 second
    }
    
    @Test
    public void testAllowRequests() {
        // First 5 requests should be allowed
        for (int i = 0; i < 5; i++) {
            RateLimiter.RateLimitResult result = rateLimiter.allowRequest("test-key");
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(5, result.getLimit());
            assertEquals(4 - i, result.getRemaining());
        }
    }
    
    @Test
    public void testRejectExcessRequests() {
        // First 5 should pass
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest("test-key").isAllowed());
        }
        
        // 6th should be rejected
        RateLimiter.RateLimitResult result = rateLimiter.allowRequest("test-key");
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertTrue(result.getRetryAfter() > 0);
    }
    
    @Test
    public void testWindowReset() throws InterruptedException {
        // Use up all tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest("test-key").isAllowed());
        }
        
        // Should be rejected
        assertFalse(rateLimiter.allowRequest("test-key").isAllowed());
        
        // Wait for window to reset
        Thread.sleep(1100);
        
        // Should be allowed again
        assertTrue(rateLimiter.allowRequest("test-key").isAllowed());
    }
    
    @Test
    public void testMultipleKeys() {
        // Key1: use 3 tokens
        for (int i = 0; i < 3; i++) {
            assertTrue(rateLimiter.allowRequest("key1").isAllowed());
        }
        
        // Key2: should have full capacity
        RateLimiter.RateLimitResult result = rateLimiter.allowRequest("key2");
        assertTrue(result.isAllowed());
        assertEquals(4, result.getRemaining());
    }
    
    @Test
    public void testStats() {
        rateLimiter.allowRequest("test-key"); // Allowed
        rateLimiter.allowRequest("test-key"); // Allowed
        rateLimiter.allowRequest("test-key"); // Allowed
        
        RateLimiter.RateLimitStats stats = rateLimiter.getStats("test-key");
        assertEquals(5, stats.getLimit());
        assertEquals(2, stats.getRemaining());
        assertEquals(3, stats.getTotalRequests());
        assertEquals(0, stats.getRejectedRequests());
    }
    
    @Test
    public void testReset() {
        // Use up tokens
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowRequest("test-key");
        }
        
        assertFalse(rateLimiter.allowRequest("test-key").isAllowed());
        
        // Reset
        rateLimiter.reset("test-key");
        
        // Should be allowed again
        assertTrue(rateLimiter.allowRequest("test-key").isAllowed());
    }
}