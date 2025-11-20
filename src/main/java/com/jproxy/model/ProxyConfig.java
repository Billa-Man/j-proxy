package com.jproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ProxyConfig {
    
    @JsonProperty("target_url")
    private String targetUrl;
    
    @JsonProperty("timeout")
    private int timeout = 30000; // 30 seconds default
    
    @JsonProperty("follow_redirects")
    private boolean followRedirects = true;
    
    @JsonProperty("headers")
    private Map<String, String> defaultHeaders = new HashMap<>();
    
    public ProxyConfig() {}
    
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
}