package com.jproxy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jproxy.core.Cache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class StatsHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(StatsHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ProxyHandler proxyHandler;

    public StatsHandler(ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("service", "J-Proxy");
        stats.put("version", "1.0.0");
        stats.put("timestamp", System.currentTimeMillis());

        if (proxyHandler != null) {
            // Cache stats
            Cache.CacheStats cacheStats = proxyHandler.getCacheStats();
            if (cacheStats != null) {
                Map<String, Object> cache = new HashMap<>();
                cache.put("enabled", true);
                cache.put("hits", cacheStats.getHits());
                cache.put("misses", cacheStats.getMisses());
                cache.put("total_requests", cacheStats.getTotal());
                cache.put("hit_rate_percent", String.format("%.2f", cacheStats.getHitRate()));
                cache.put("size", cacheStats.getSize());
                cache.put("max_size", cacheStats.getMaxSize());
                stats.put("cache", cache);
            } else {
                stats.put("cache", Map.of("enabled", false));
            }

            // Rate limit stats (note: this would need to be expanded to show per-key stats)
            Map<String, Object> rateLimit = new HashMap<>();
            rateLimit.put("enabled", proxyHandler.getRateLimitStats("") != null);
            stats.put("rate_limit", rateLimit);
        }

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
        byte[] response = json.getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);

        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();

        logger.debug("Stats endpoint accessed");
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorBody = String.format("{\"error\":\"%s\"}", message);
        byte[] errorBytes = errorBody.getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorBytes.length);
        exchange.getResponseBody().write(errorBytes);
        exchange.getResponseBody().close();
    }
}