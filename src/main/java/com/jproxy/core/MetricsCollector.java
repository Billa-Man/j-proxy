package com.jproxy.core;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {
    
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    private volatile long startTime;
    
    public MetricsCollector() {
        this.startTime = System.currentTimeMillis();
    }
    
    public void recordRequest(boolean success, long responseTimeMs) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalResponseTime.addAndGet(responseTimeMs);
    }
    
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    public long getSuccessfulRequests() {
        return successfulRequests.get();
    }
    
    public long getFailedRequests() {
        return failedRequests.get();
    }
    
    public double getAverageResponseTime() {
        long total = totalRequests.get();
        return total == 0 ? 0.0 : (double) totalResponseTime.get() / total;
    }
    
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total == 0 ? 100.0 : (double) successfulRequests.get() / total * 100;
    }
    
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        startTime = System.currentTimeMillis();
    }
}