package com.jproxy.core;

public interface RequestListener {
    void onRequest(String method, String path, int statusCode, long duration, boolean fromCache);
}