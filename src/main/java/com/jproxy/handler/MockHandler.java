package com.jproxy.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jproxy.model.MockConfig;
import com.jproxy.model.MockEndpoint;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MockHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ThreadLocal<Integer> statusCodeFlag = new ThreadLocal<>();

    private final MockConfig config;

    public MockHandler(MockConfig config) {
        this.config = config;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        logger.info("Mock mode: Looking for match for {} {}", method, path);

        // Find matching endpoint
        MockEndpoint endpoint = config.findMatch(method, path);

        if (endpoint == null) {
            statusCodeFlag.set(404);
            sendNotFound(exchange, method, path);
            return;
        }

        logger.info("Found matching endpoint for {} {}", method, path);

        // Get response config
        MockEndpoint.MockResponse mockResponse = endpoint.getResponse();

        // Set status code
        statusCodeFlag.set(mockResponse.getStatus());

        // Simulate delay if configured
        if (mockResponse.getDelay() > 0) {
            try {
                logger.debug("Simulating delay of {}ms", mockResponse.getDelay());
                Thread.sleep(mockResponse.getDelay());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Convert body to JSON string
        String responseBody;
        if (mockResponse.getBody() instanceof String) {
            responseBody = (String) mockResponse.getBody();
        } else {
            responseBody = mapper.writeValueAsString(mockResponse.getBody());
        }

        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

        // Set custom headers
        Map<String, String> headers = mockResponse.getHeaders();
        if (headers.isEmpty()) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
        } else {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                exchange.getResponseHeaders().set(header.getKey(), header.getValue());
            }
        }

        // Always add our signature
        exchange.getResponseHeaders().set("X-Powered-By", "J-Proxy");

        // Send response
        int statusCode = mockResponse.getStatus();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        OutputStream os = exchange.getResponseBody();
        try {
            os.write(responseBytes);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                logger.debug("Error closing mock stream (safe to ignore): {}", e.getMessage());
            }
        }

        logger.info("Mock response sent: {} ({})", statusCode, responseBytes.length + " bytes");
    }

    private void sendNotFound(HttpExchange exchange, String method, String path) throws IOException {
        String errorBody = String.format(
                "{\"error\":\"No mock configured\",\"method\":\"%s\",\"path\":\"%s\"}",
                method, path);

        byte[] errorBytes = errorBody.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("X-Powered-By", "J-Proxy");
        exchange.sendResponseHeaders(404, errorBytes.length);

        OutputStream os = exchange.getResponseBody();
        try {
            os.write(errorBytes);
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                logger.debug("Error closing not found stream (safe to ignore): {}", e.getMessage());
            }
        }

        logger.warn("No mock found for {} {}", method, path);
    }

    public static int getLastStatusCode() {
        Integer status = statusCodeFlag.get();
        return status != null ? status : 200;
    }

    public static void clearStatusCode() {
        statusCodeFlag.remove();
    }
}