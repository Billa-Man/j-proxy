package com.jproxy;

import com.jproxy.handler.MockHandler;
import com.jproxy.model.MockConfig;
import com.jproxy.model.MockEndpoint;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class MockHandlerTest {
    
    @Mock
    private HttpExchange exchange;
    
    private MockHandler mockHandler;
    private MockConfig mockConfig;
    private ByteArrayOutputStream responseBody;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        mockConfig = new MockConfig();
        
        // Add test endpoint
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setPath("/test");
        endpoint.setMethod("GET");
        
        MockEndpoint.MockResponse response = new MockEndpoint.MockResponse();
        response.setStatus(200);
        
        HashMap<String, Object> body = new HashMap<>();
        body.put("message", "test");
        response.setBody(body);
        
        endpoint.setResponse(response);
        mockConfig.getEndpoints().add(endpoint);
        
        mockHandler = new MockHandler(mockConfig);
        responseBody = new ByteArrayOutputStream();
    }
    
    @Test
    public void testMatchingEndpoint() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/test"));
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);
        
        mockHandler.handle(exchange);
        
        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("message"));
        assertTrue(response.contains("test"));
    }
    
    @Test
    public void testNonMatchingEndpoint() throws IOException {
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(URI.create("/notfound"));
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getResponseBody()).thenReturn(responseBody);
        
        mockHandler.handle(exchange);
        
        verify(exchange).sendResponseHeaders(eq(404), anyLong());
        String response = responseBody.toString();
        assertTrue(response.contains("No mock configured"));
    }
}