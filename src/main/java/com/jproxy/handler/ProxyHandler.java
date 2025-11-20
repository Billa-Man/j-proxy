package com.jproxy.handler;

import com.jproxy.core.Cache;
import com.jproxy.core.RateLimiter;
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
    private final RateLimiter rateLimiter;
    private static final ThreadLocal<Boolean> cacheHitFlag = new ThreadLocal<>();
    private static final ThreadLocal<Integer> responseStatusCode = new ThreadLocal<>();

    public ProxyHandler(ProxyConfig config) {
        this.config = config;
        this.cache = config.isCacheEnabled() ? new Cache(config.getCacheMaxSize()) : null;

        this.rateLimiter = config.isRateLimitEnabled()
                ? new RateLimiter(config.getRateLimitMaxRequests(), config.getRateLimitWindowSeconds())
                : null;

        if (cache != null) {
            logger.info("Cache ENABLED: max {} entries, TTL {}s, methods: {}",
                    config.getCacheMaxSize(),
                    config.getCacheTtlSeconds(),
                    config.getCacheMethods());
        } else {
            logger.info("Cache DISABLED");
        }

        if (rateLimiter != null) {
            logger.info("Rate limiter enabled: {} requests per {}s (strategy: {})",
                    config.getRateLimitMaxRequests(),
                    config.getRateLimitWindowSeconds(),
                    config.getRateLimitKeyStrategy());
        }
    }

    public void handle(HttpExchange exchange) throws IOException {
        // Clear flags at start
        cacheHitFlag.set(false);
        responseStatusCode.set(200);

        HttpRequest request = buildRequest(exchange);

        // Check rate limit first
        RateLimiter.RateLimitResult rateLimitResult = null;
        if (rateLimiter != null) {
            String rateLimitKey = buildRateLimitKey(exchange, request);
            rateLimitResult = rateLimiter.allowRequest(rateLimitKey);

            logger.info("Rate limit check: key={}, allowed={}, remaining={}/{}",
                    rateLimitKey, rateLimitResult.isAllowed(),
                    rateLimitResult.getRemaining(), rateLimitResult.getLimit());

            if (!rateLimitResult.isAllowed()) {
                logger.warn("Rate limit exceeded for key: {}", rateLimitKey);
                sendRateLimitExceeded(exchange, rateLimitResult);
                responseStatusCode.set(429);
                return;
            }
        }

        // Check cache
        String cacheKey = null;
        if (cache != null && config.shouldCache(request.getMethod())) {
            cacheKey = buildCacheKey(request);
            HttpResponse cachedResponse = cache.get(cacheKey);

            if (cachedResponse != null) {
                logger.info("Serving from cache: {} {}", request.getMethod(), request.getUrl());
                cacheHitFlag.set(true);
                responseStatusCode.set(cachedResponse.getStatusCode());
                sendResponse(exchange, cachedResponse, true, rateLimitResult);
                return;
            }
        }

        try {
            HttpResponse response = HttpUtil.executeRequest(request, config.getTimeout());
            responseStatusCode.set(response.getStatusCode());

            if (cacheKey != null && shouldCacheResponse(response)) {
                long ttlMillis = config.getCacheTtlSeconds() * 1000L;
                cache.put(cacheKey, response, ttlMillis);
            }

            sendResponse(exchange, response, false, rateLimitResult);

            logger.info("Proxied {} {} -> {}",
                    request.getMethod(),
                    request.getUrl(),
                    response.getStatusCode());

        } catch (IOException e) {
            logger.error("Failed to proxy request to {}", request.getUrl(), e);
            responseStatusCode.set(502);
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

    private void sendResponse(HttpExchange exchange, HttpResponse response, boolean fromCache,
            RateLimiter.RateLimitResult rateLimitResult) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();

        // Set response headers from proxied response
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            responseHeaders.set(entry.getKey(), entry.getValue());
        }

        // Add proxy signature
        responseHeaders.set("X-Proxied-By", "J-Proxy");

        // Add cache header
        if (fromCache) {
            responseHeaders.set("X-Cache", "HIT");
        } else {
            responseHeaders.set("X-Cache", "MISS");
        }

        // Add rate limit headers if rate limiting is enabled
        if (rateLimitResult != null) {
            responseHeaders.set("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
            responseHeaders.set("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
            responseHeaders.set("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetAt()));
        }

        // Send response
        byte[] body = response.getBody();
        int contentLength = (body != null) ? body.length : 0;

        exchange.sendResponseHeaders(response.getStatusCode(), contentLength);

        // Only write and close if there's a body
        OutputStream os = exchange.getResponseBody();
        try {
            if (contentLength > 0) {
                os.write(body);
            }
        } finally {
            // Always close the output stream in a finally block
            try {
                os.close();
            } catch (IOException e) {
                // Ignore close errors - connection might already be closed
                logger.debug("Error closing response stream (safe to ignore): {}", e.getMessage());
            }
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorBody = String.format("{\"error\":\"%s\"}", message);
        byte[] errorBytes = errorBody.getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorBytes.length);

        OutputStream os = exchange.getResponseBody();
        try {
            os.write(errorBytes);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                logger.debug("Error closing error stream (safe to ignore): {}", e.getMessage());
            }
        }
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

    private String buildRateLimitKey(HttpExchange exchange, HttpRequest request) {
        String strategy = config.getRateLimitKeyStrategy();

        switch (strategy.toLowerCase()) {
            case "ip":
                return getClientIp(exchange);
            case "url":
                return request.getUrl();
            case "url+ip":
                return request.getUrl() + ":" + getClientIp(exchange);
            default:
                return request.getUrl();
        }
    }

    private String getClientIp(HttpExchange exchange) {
        // Try X-Forwarded-For header first
        String forwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private void sendRateLimitExceeded(HttpExchange exchange, RateLimiter.RateLimitResult result) throws IOException {
        String errorBody = String.format(
                "{\"error\":\"Rate limit exceeded\",\"limit\":%d,\"retry_after\":%d}",
                result.getLimit(),
                result.getRetryAfter());

        byte[] errorBytes = errorBody.getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        exchange.getResponseHeaders().set("X-RateLimit-Remaining", "0");
        exchange.getResponseHeaders().set("X-RateLimit-Reset", String.valueOf(result.getResetAt()));
        exchange.getResponseHeaders().set("Retry-After", String.valueOf(result.getRetryAfter()));

        exchange.sendResponseHeaders(429, errorBytes.length);

        OutputStream os = exchange.getResponseBody();
        try {
            os.write(errorBytes);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                logger.debug("Error closing rate limit stream (safe to ignore): {}", e.getMessage());
            }
        }
    }

    public RateLimiter.RateLimitStats getRateLimitStats(String key) {
        return rateLimiter != null ? rateLimiter.getStats(key) : null;
    }

    public static boolean wasCacheHit() {
        Boolean hit = cacheHitFlag.get();
        return hit != null && hit;
    }

    public static void clearCacheFlag() {
        cacheHitFlag.remove();
    }

    public static int getLastStatusCode() {
        Integer status = responseStatusCode.get();
        return status != null ? status : 200;
    }

    public static void clearStatusCode() {
        responseStatusCode.remove();
    }
}