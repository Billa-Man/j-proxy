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

    public ProxyHandler(ProxyConfig config) {
        this.config = config;
        this.cache = config.isCacheEnabled() ? new Cache(config.getCacheMaxSize()) : null;

        this.rateLimiter = config.isRateLimitEnabled()
                ? new RateLimiter(config.getRateLimitMaxRequests(), config.getRateLimitWindowSeconds())
                : null;

        if (cache != null) {
            logger.info("Cache enabled: max {} entries, TTL {}s",
                    config.getCacheMaxSize(), config.getCacheTtlSeconds());
        }

        if (rateLimiter != null) {
            logger.info("Rate limiter enabled: {} requests per {}s (strategy: {})",
                    config.getRateLimitMaxRequests(),
                    config.getRateLimitWindowSeconds(),
                    config.getRateLimitKeyStrategy());
        }
    }

    public void handle(HttpExchange exchange) throws IOException {
        HttpRequest request = buildRequest(exchange);

        // Check rate limit first
        if (rateLimiter != null) {
            String rateLimitKey = buildRateLimitKey(exchange, request);
            RateLimiter.RateLimitResult rateLimitResult = rateLimiter.allowRequest(rateLimitKey);

            if (!rateLimitResult.isAllowed()) {
                logger.warn("Rate limit exceeded for key: {}", rateLimitKey);
                sendRateLimitExceeded(exchange, rateLimitResult);
                return;
            }

            // Add rate limit headers to response
            exchange.getResponseHeaders().set("X-RateLimit-Limit",
                    String.valueOf(rateLimitResult.getLimit()));
            exchange.getResponseHeaders().set("X-RateLimit-Remaining",
                    String.valueOf(rateLimitResult.getRemaining()));
            exchange.getResponseHeaders().set("X-RateLimit-Reset",
                    String.valueOf(rateLimitResult.getResetAt()));
        }

        // Check cache
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
            HttpResponse response = HttpUtil.executeRequest(request, config.getTimeout());

            if (cacheKey != null && shouldCacheResponse(response)) {
                long ttlMillis = config.getCacheTtlSeconds() * 1000L;
                cache.put(cacheKey, response, ttlMillis);
            }

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
        exchange.getResponseBody().write(errorBytes);
        exchange.getResponseBody().close();
    }

    public RateLimiter.RateLimitStats getRateLimitStats(String key) {
        return rateLimiter != null ? rateLimiter.getStats(key) : null;
    }
}