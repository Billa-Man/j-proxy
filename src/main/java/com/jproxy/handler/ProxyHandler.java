package com.jproxy.handler;

import com.jproxy.core.Cache;
import com.jproxy.model.HttpRequest;
import com.jproxy.model.HttpResponse;
import com.jproxy.model.ProxyConfig;
import com.jproxy.util.HttpUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class ProxyHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    
    private final ProxyConfig config;
    private final Cache cache;
    
    public ProxyHandler(ProxyConfig config) {
        this.config = config;
        this.cache = config.isCacheEnabled() ? 
            new Cache(config.getCacheMaxSize()) : null;
        
        if (cache != null) {
            logger.info("Cache enabled: max {} entries, TTL {}s", 
                config.getCacheMaxSize(), config.getCacheTtlSeconds());
        }
    }
    
    public void handle(HttpExchange exchange) throws IOException {
        HttpRequest request = buildRequest(exchange);
        
        // Check cache first
        String cacheKey = null;
        if (cache != null && config.shouldCache(request.getMethod())) {
            cacheKey = buildCacheKey(request);
            HttpResponse cachedResponse = cache.get(cacheKey);
            
            if (cachedResponse != null) {
                logger.info("Serving from cache: {} {}", request.getMethod(), request.getUrl());
                sendResponse(exchange, cachedResponse, true);
                return;
            }
        }
        
        try {
            // Execute the proxied request
            HttpResponse response = HttpUtil.executeRequest(request, config.getTimeout());
            
            // Cache the response if applicable
            if (cacheKey != null && shouldCacheResponse(response)) {
                long ttlMillis = config.getCacheTtlSeconds() * 1000L;
                cache.put(cacheKey, response, ttlMillis);
            }
            
            // Send response back to client
            sendResponse(exchange, response, false);
            
            logger.info("Proxied {} {} -> {}", 
                request.getMethod(), 
                request.getUrl(), 
                response.getStatusCode());
            
        } catch (IOException e) {
            logger.error("Failed to proxy request to {}", request.getUrl(), e);
            sendError(exchange, 502, "Bad Gateway: " + e.getMessage());
        }
    }
    
    private String buildCacheKey(HttpRequest request) {
        // Cache key: METHOD:URL
        return request.getMethod() + ":" + request.getUrl();
    }
    
    private boolean shouldCacheResponse(HttpResponse response) {
        int status = response.getStatusCode();
        // Only cache successful responses
        return status >= 200 && status < 300;
    }
    
    private HttpRequest buildRequest(HttpExchange exchange) throws IOException {
        HttpRequest request = new HttpRequest();
        
        request.setMethod(exchange.getRequestMethod());
        
        String targetUrl = buildTargetUrl(exchange);
        request.setUrl(targetUrl);
        
        Headers incomingHeaders = exchange.getRequestHeaders();
        for (Map.Entry<String, List<String>> entry : incomingHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                request.addHeader(entry.getKey(), entry.getValue().get(0));
            }
        }
        
        for (Map.Entry<String, String> entry : config.getDefaultHeaders().entrySet()) {
            if (!request.getHeaders().containsKey(entry.getKey())) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        InputStream is = exchange.getRequestBody();
        if (is.available() > 0) {
            request.setBody(readAllBytes(is));
        }
        
        return request;
    }
    
    private String buildTargetUrl(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        
        String baseUrl = config.getTargetUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://httpbin.org";
        }
        
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        String url = baseUrl + path;
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }
        
        return url;
    }
    
    private void sendResponse(HttpExchange exchange, HttpResponse response, boolean fromCache) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            responseHeaders.set(entry.getKey(), entry.getValue());
        }
        
        responseHeaders.set("X-Proxied-By", "J-Proxy");
        if (fromCache) {
            responseHeaders.set("X-Cache", "HIT");
        } else {
            responseHeaders.set("X-Cache", "MISS");
        }
        
        byte[] body = response.getBody();
        int contentLength = (body != null) ? body.length : 0;
        
        exchange.sendResponseHeaders(response.getStatusCode(), contentLength);
        
        if (contentLength > 0) {
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.close();
        } else {
            exchange.getResponseBody().close();
        }
    }
    
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorBody = String.format("{\"error\":\"%s\"}", message);
        byte[] errorBytes = errorBody.getBytes();
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorBytes.length);
        exchange.getResponseBody().write(errorBytes);
        exchange.getResponseBody().close();
    }
    
    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        return buffer.toByteArray();
    }
    
    public Cache.CacheStats getCacheStats() {
        return cache != null ? cache.getStats() : null;
    }
}