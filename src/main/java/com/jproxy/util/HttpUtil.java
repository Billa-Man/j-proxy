package com.jproxy.util;

import com.jproxy.model.HttpRequest;
import com.jproxy.model.HttpResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HttpUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    
    // Headers we shouldn't forward
    private static final Set<String> EXCLUDED_HEADERS = new HashSet<>(Arrays.asList(
        "host", "connection", "keep-alive", "proxy-connection",
        "transfer-encoding", "te", "trailer", "upgrade"
    ));
    
    public static HttpResponse executeRequest(HttpRequest request, int timeout) throws IOException {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setRedirectsEnabled(true)
            .build();
        
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build()) {
            
            HttpUriRequest httpRequest = buildRequest(request);
            
            logger.info("Forwarding {} to {}", request.getMethod(), request.getUrl());
            
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                return convertResponse(response);
            }
        }
    }
    
    private static HttpUriRequest buildRequest(HttpRequest request) throws IOException {
        HttpRequestBase httpRequest;
        
        switch (request.getMethod().toUpperCase()) {
            case "GET":
                httpRequest = new HttpGet(request.getUrl());
                break;
            case "POST":
                httpRequest = new HttpPost(request.getUrl());
                if (request.hasBody()) {
                    ((HttpPost) httpRequest).setEntity(new ByteArrayEntity(request.getBody()));
                }
                break;
            case "PUT":
                httpRequest = new HttpPut(request.getUrl());
                if (request.hasBody()) {
                    ((HttpPut) httpRequest).setEntity(new ByteArrayEntity(request.getBody()));
                }
                break;
            case "DELETE":
                httpRequest = new HttpDelete(request.getUrl());
                break;
            case "PATCH":
                httpRequest = new HttpPatch(request.getUrl());
                if (request.hasBody()) {
                    ((HttpPatch) httpRequest).setEntity(new ByteArrayEntity(request.getBody()));
                }
                break;
            case "HEAD":
                httpRequest = new HttpHead(request.getUrl());
                break;
            case "OPTIONS":
                httpRequest = new HttpOptions(request.getUrl());
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + request.getMethod());
        }
        
        // Copy headers (excluding hop-by-hop headers)
        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            String headerName = header.getKey().toLowerCase();
            if (!EXCLUDED_HEADERS.contains(headerName)) {
                httpRequest.setHeader(header.getKey(), header.getValue());
            }
        }
        
        return httpRequest;
    }
    
    private static HttpResponse convertResponse(CloseableHttpResponse apacheResponse) throws IOException {
        HttpResponse response = new HttpResponse();
        
        // Status code
        response.setStatusCode(apacheResponse.getStatusLine().getStatusCode());
        response.setReasonPhrase(apacheResponse.getStatusLine().getReasonPhrase());
        
        // Headers
        for (Header header : apacheResponse.getAllHeaders()) {
            String headerName = header.getName().toLowerCase();
            if (!EXCLUDED_HEADERS.contains(headerName)) {
                response.addHeader(header.getName(), header.getValue());
            }
        }
        
        // Body
        HttpEntity entity = apacheResponse.getEntity();
        if (entity != null) {
            response.setBody(EntityUtils.toByteArray(entity));
        }
        
        logger.debug("Response: {} {} ({} bytes)", 
            response.getStatusCode(), 
            response.getReasonPhrase(),
            response.hasBody() ? response.getBody().length : 0);
        
        return response;
    }
}