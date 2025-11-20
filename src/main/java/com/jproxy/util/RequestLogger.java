package com.jproxy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RequestLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final boolean enabled;
    private final File logFile;
    
    public RequestLogger(boolean enabled, String logPath) {
        this.enabled = enabled;
        
        if (enabled) {
            this.logFile = new File(logPath);
            try {
                logFile.getParentFile().mkdirs();
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                logger.info("Request logging enabled: {}", logPath);
            } catch (IOException e) {
                logger.error("Failed to create request log file", e);
                throw new RuntimeException("Failed to initialize request logger", e);
            }
        } else {
            this.logFile = null;
        }
    }
    
    public void logRequest(String method, String url, int statusCode, long durationMs, 
                          boolean fromCache, String clientIp) {
        if (!enabled) {
            return;
        }
        
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", dateFormat.format(new Date()));
        logEntry.put("method", method);
        logEntry.put("url", url);
        logEntry.put("status", statusCode);
        logEntry.put("duration_ms", durationMs);
        logEntry.put("from_cache", fromCache);
        logEntry.put("client_ip", clientIp);
        
        try {
            String json = mapper.writeValueAsString(logEntry);
            synchronized (this) {
                try (FileWriter writer = new FileWriter(logFile, true)) {
                    writer.write(json + "\n");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to write request log", e);
        }
    }
}