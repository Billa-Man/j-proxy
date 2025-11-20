package com.jproxy.handler;

import com.jproxy.core.Config;
import com.jproxy.core.MetricsCollector;
import com.jproxy.core.RequestListener;
import com.jproxy.model.MockConfig;
import com.jproxy.model.ProxyConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class RequestHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final String mode;
    private final MockHandler mockHandler;
    private final ProxyHandler proxyHandler;
    private final MetricsCollector metricsCollector;
    private RequestListener requestListener;

    public RequestHandler(String mode, String configPath, MetricsCollector metricsCollector) {
        this.mode = mode;
        this.metricsCollector = metricsCollector;

        if ("mock".equalsIgnoreCase(mode)) {
            try {
                MockConfig mockConfig = Config.loadMockConfig(configPath);
                this.mockHandler = new MockHandler(mockConfig);
                this.proxyHandler = null;
                logger.info("Mock handler initialized with {} endpoints",
                        mockConfig.getEndpoints().size());
            } catch (IOException e) {
                logger.error("Failed to load mock config", e);
                throw new RuntimeException("Failed to initialize mock handler", e);
            }
        } else if ("proxy".equalsIgnoreCase(mode)) {
            try {
                ProxyConfig proxyConfig = Config.loadProxyConfig(configPath);
                this.proxyHandler = new ProxyHandler(proxyConfig);
                this.mockHandler = null;
                String target = proxyConfig.getTargetUrl() != null ? proxyConfig.getTargetUrl()
                        : "httpbin.org (default)";
                logger.info("Proxy handler initialized, target: {}", target);
            } catch (IOException e) {
                logger.error("Failed to load proxy config", e);
                throw new RuntimeException("Failed to initialize proxy handler", e);
            }
        } else {
            this.mockHandler = null;
            this.proxyHandler = null;
        }
    }

    public void setRequestListener(RequestListener listener) {
        this.requestListener = listener;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("==> Incoming {} {}", method, path);

        long startTime = System.currentTimeMillis();
        boolean success = false;
        int statusCode = 500;
        boolean fromCache = false;

        try {
            if ("mock".equalsIgnoreCase(mode)) {
                mockHandler.handle(exchange);
                success = true;
                statusCode = MockHandler.getLastStatusCode();
                MockHandler.clearStatusCode();
            } else if ("proxy".equalsIgnoreCase(mode)) {
                proxyHandler.handle(exchange);
                success = true;
                statusCode = ProxyHandler.getLastStatusCode();
                fromCache = ProxyHandler.wasCacheHit();
                ProxyHandler.clearCacheFlag();
                ProxyHandler.clearStatusCode();
            } else {
                sendError(exchange, 500, "Unknown mode: " + mode);
            }
        } catch (Exception e) {
            logger.error("Error handling request", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
            statusCode = 500;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (metricsCollector != null) {
                metricsCollector.recordRequest(success, duration);
            }

            // Notify listener with actual status and cache info
            if (requestListener != null) {
                requestListener.onRequest(method, path, statusCode, duration, fromCache);
            }
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorBody = String.format("{\"error\":\"%s\"}", message);
        byte[] errorBytes = errorBody.getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");

        try {
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
        } catch (IOException e) {
            // If we can't send the error response, just log it
            logger.error("Failed to send error response: {}", e.getMessage());
        }
    }

    public ProxyHandler getProxyHandler() {
        return proxyHandler;
    }
}