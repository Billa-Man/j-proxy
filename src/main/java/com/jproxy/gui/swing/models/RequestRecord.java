package com.jproxy.gui.swing.models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RequestRecord {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final String timestamp;
    private final String method;
    private final String path;
    private final int statusCode;
    private final long duration;
    private final boolean fromCache;
    
    public RequestRecord(String method, String path, int statusCode, long duration, boolean fromCache) {
        this.timestamp = LocalTime.now().format(TIME_FORMATTER);
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.duration = duration;
        this.fromCache = fromCache;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public String getMethod() {
        return method;
    }
    
    public String getPath() {
        return path;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public boolean isFromCache() {
        return fromCache;
    }
    
    public String getStatusText() {
        if (statusCode >= 200 && statusCode < 300) {
            return "Success";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "Redirect";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "Client Error";
        } else {
            return "Server Error";
        }
    }
}