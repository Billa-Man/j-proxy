package com.jproxy.model;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    
    private String method;
    private String url;
    private Map<String, String> headers;
    private byte[] body;
    
    public HttpRequest() {
        this.headers = new HashMap<>();
    }
    
    public HttpRequest(String method, String url) {
        this();
        this.method = method;
        this.url = url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public void setBody(byte[] body) {
        this.body = body;
    }
    
    public boolean hasBody() {
        return body != null && body.length > 0;
    }
}