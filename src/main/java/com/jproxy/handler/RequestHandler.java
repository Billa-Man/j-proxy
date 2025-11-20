package com.jproxy.handler;

import com.jproxy.core.Config;
import com.jproxy.model.MockConfig;
import com.jproxy.model.ProxyConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RequestHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final String mode;
    private final MockHandler mockHandler;
    private final ProxyHandler proxyHandler;

    public RequestHandler(String mode, String configPath) {
        this.mode = mode;

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

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("==> Incoming {} {}", method, path);

        try {
            if ("mock".equalsIgnoreCase(mode)) {
                mockHandler.handle(exchange);
            } else if ("proxy".equalsIgnoreCase(mode)) {
                proxyHandler.handle(exchange);
            } else {
                sendError(exchange, 500, "Unknown mode: " + mode);
            }
        } catch (Exception e) {
            logger.error("Error handling request", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
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

    public ProxyHandler getProxyHandler() {
        return proxyHandler;
    }
}