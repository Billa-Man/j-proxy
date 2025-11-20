package com.jproxy;

import com.jproxy.core.MetricsCollector;
import com.jproxy.handler.RequestHandler;
import com.jproxy.handler.StatsHandler;
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
    private RequestHandler requestHandler;
    private MetricsCollector metricsCollector;

    public ProxyServer(int port, String mode, String configPath) {
        this.port = port;
        this.mode = mode;
        this.configPath = configPath;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        metricsCollector = new MetricsCollector();
        requestHandler = new RequestHandler(mode, configPath, metricsCollector);

        // Main handler for all requests
        server.createContext("/", requestHandler);

        // Stats endpoint (only in proxy mode)
        if ("proxy".equalsIgnoreCase(mode) && requestHandler.getProxyHandler() != null) {
            StatsHandler statsHandler = new StatsHandler(requestHandler.getProxyHandler());
            server.createContext("/__stats", statsHandler);
            logger.info("Stats endpoint available at /__stats");
        }

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        logger.info("HTTP server bound to port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("Server stopped");
        }
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }
}