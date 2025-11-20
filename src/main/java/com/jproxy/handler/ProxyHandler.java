package com.jproxy.handler;

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
    
    public ProxyHandler(ProxyConfig config) {
        this.config = config;
    }
    
    public void handle(HttpExchange exchange) throws IOException {
        // Build request from incoming exchange
        HttpRequest request = buildRequest(exchange);
        
        try {
            // Execute the proxied request
            HttpResponse response = HttpUtil.executeRequest(request, config.getTimeout());
            
            // Send response back to client
            sendResponse(exchange, response);
            
            logger.info("Proxied {} {} -> {}", 
                request.getMethod(), 
                request.getUrl(), 
                response.getStatusCode());
            
        } catch (IOException e) {
            logger.error("Failed to proxy request to {}", request.getUrl(), e);
            sendError(exchange, 502, "Bad Gateway: " + e.getMessage());
        }
    }
    
    private HttpRequest buildRequest(HttpExchange exchange) throws IOException {
        HttpRequest request = new HttpRequest();
        
        // Method
        request.setMethod(exchange.getRequestMethod());
        
        // Build target URL
        String targetUrl = buildTargetUrl(exchange);
        request.setUrl(targetUrl);
        
        // Headers
        Headers incomingHeaders = exchange.getRequestHeaders();
        for (Map.Entry<String, List<String>> entry : incomingHeaders.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                request.addHeader(entry.getKey(), entry.getValue().get(0));
            }
        }
        
        // Add default headers from config
        for (Map.Entry<String, String> entry : config.getDefaultHeaders().entrySet()) {
            if (!request.getHeaders().containsKey(entry.getKey())) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        // Body (if present)
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
            baseUrl = "http://httpbin.org"; // Default test target
        }
        
        // Remove trailing slash from base URL
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        String url = baseUrl + path;
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }
        
        return url;
    }
    
    private void sendResponse(HttpExchange exchange, HttpResponse response) throws IOException {
        // Set response headers
        Headers responseHeaders = exchange.getResponseHeaders();
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            responseHeaders.set(entry.getKey(), entry.getValue());
        }
        
        // Add proxy signature
        responseHeaders.set("X-Proxied-By", "J-Proxy");
        
        // Send response
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
}