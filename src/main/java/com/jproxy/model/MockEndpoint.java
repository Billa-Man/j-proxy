package com.jproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class MockEndpoint {

    @JsonProperty("path")
    private String path;

    @JsonProperty("method")
    private String method;

    @JsonProperty("response")
    private MockResponse response;

    // Constructors
    public MockEndpoint() {
    }

    public MockEndpoint(String path, String method, MockResponse response) {
        this.path = path;
        this.method = method;
        this.response = response;
    }

    // Getters and setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public MockResponse getResponse() {
        return response;
    }

    public void setResponse(MockResponse response) {
        this.response = response;
    }

    // Check if this endpoint matches a request
    public boolean matches(String requestMethod, String requestPath) {
        boolean methodMatches = method.equalsIgnoreCase(requestMethod);
        boolean pathMatches = pathMatches(requestPath);
        return methodMatches && pathMatches;
    }

    private boolean pathMatches(String requestPath) {
        // Exact match for now
        // TODO: Add path parameter support later (e.g., /users/{id})
        return path.equals(requestPath);
    }

    public static class MockResponse {
        @JsonProperty("status")
        private int status = 200;

        @JsonProperty("headers")
        private Map<String, String> headers = new HashMap<>();

        @JsonProperty("body")
        private Object body;

        @JsonProperty("delay")
        private int delay = 0; // milliseconds

        public MockResponse() {
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }
    }
}