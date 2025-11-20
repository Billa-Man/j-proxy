package com.jproxy.core;

import com.jproxy.ProxyServer;
import com.jproxy.handler.ProxyHandler;
import com.jproxy.core.RequestListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerController {

    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);

    private ProxyServer server;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private int port;
    private String mode;
    private String configPath;

    public ServerController() {
    }

    public void configure(int port, String mode, String configPath) {
        this.port = port;
        this.mode = mode;
        this.configPath = configPath;
    }

    public void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("Server is already running");
        }

        logger.info("Starting server: mode={}, port={}", mode, port);
        server = new ProxyServer(port, mode, configPath);
        server.start();
        running.set(true);
        logger.info("Server started successfully");
    }

    public void stop() {
        if (!running.get()) {
            throw new IllegalStateException("Server is not running");
        }

        logger.info("Stopping server");
        if (server != null) {
            server.stop();
        }
        running.set(false);
        logger.info("Server stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return port;
    }

    public String getMode() {
        return mode;
    }

    public String getConfigPath() {
        return configPath;
    }

    public MetricsCollector getMetricsCollector() {
        return server != null ? server.getMetricsCollector() : null;
    }

    public ProxyHandler getProxyHandler() {
        if (server != null && server.getRequestHandler() != null) {
            return server.getRequestHandler().getProxyHandler();
        }
        return null;
    }

    public void setRequestListener(RequestListener listener) {
        if (server != null && server.getRequestHandler() != null) {
            server.getRequestHandler().setRequestListener(listener);
        }
    }
}