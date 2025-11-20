package com.jproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MockConfig {
    
    @JsonProperty("endpoints")
    private List<MockEndpoint> endpoints = new ArrayList<>();
    
    public MockConfig() {}
    
    public List<MockEndpoint> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<MockEndpoint> endpoints) {
        this.endpoints = endpoints;
    }
    
    public MockEndpoint findMatch(String method, String path) {
        for (MockEndpoint endpoint : endpoints) {
            if (endpoint.matches(method, path)) {
                return endpoint;
            }
        }
        return null;
    }
}