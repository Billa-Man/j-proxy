package com.jproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProxyConfig {

    @JsonProperty("target_url")
    private String targetUrl;

    @JsonProperty("timeout")
    private int timeout = 30000; // 30 seconds default

    @JsonProperty("follow_redirects")
    private boolean followRedirects = true;

    @JsonProperty("headers")
    private Map<String, String> defaultHeaders = new HashMap<>();

    @JsonProperty("cache_enabled")
    private boolean cacheEnabled = true;

    @JsonProperty("cache_max_size")
    private int cacheMaxSize = 100;

    @JsonProperty("cache_ttl_seconds")
    private int cacheTtlSeconds = 300; // 5 minutes default

    @JsonProperty("cache_methods")
    private Set<String> cacheMethods = new HashSet<>(Arrays.asList("GET", "HEAD"));

    public ProxyConfig() {
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public Map<String, String> getDefaultHeaders() {
        return defaultHeaders;
    }

    public void setDefaultHeaders(Map<String, String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public Set<String> getCacheMethods() {
        return cacheMethods;
    }

    public void setCacheMethods(Set<String> cacheMethods) {
        this.cacheMethods = cacheMethods;
    }

    public boolean shouldCache(String method) {
        return cacheEnabled && cacheMethods.contains(method.toUpperCase());
    }
}