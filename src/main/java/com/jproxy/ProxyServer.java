package com.jproxy;

import com.jproxy.handler.RequestHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ProxyServer {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    
    private final int port;
    private final String mode;
    private final String configPath;
    private HttpServer server;

    public ProxyServer(int port, String mode, String configPath) {
        this.port = port;
        this.mode = mode;
        this.configPath = configPath;
    }

    public void start() throws IOException {
        // Create HTTP server
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Create request handler
        RequestHandler handler = new RequestHandler(mode, configPath);
        
        // Set up context (all paths go to same handler)
        server.createContext("/", handler);
        
        // Use thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Start server
        server.start();
        
        logger.info("HTTP server bound to port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Server stopped");
        }
    }
}