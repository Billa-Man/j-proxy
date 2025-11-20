package com.jproxy.model;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    
    private int statusCode;
    private String reasonPhrase;
    private Map<String, String> headers;
    private byte[] body;
    
    public HttpResponse() {
        this.headers = new HashMap<>();
    }
    
    public HttpResponse(int statusCode) {
        this();
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public String getReasonPhrase() {
        return reasonPhrase;
    }
    
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
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