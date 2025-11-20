package com.jproxy;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "jproxy", mixinStandardHelpOptions = true, version = "J-Proxy 1.0.0", description = "HTTP Proxy and Mock Server")
public class Main implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = { "-p", "--port" }, description = "Port to listen on (default: 8080)", defaultValue = "8080")
    private int port;

    @Option(names = { "-m", "--mode" }, description = "Mode: mock or proxy (default: mock)", defaultValue = "mock")
    private String mode;

    @Option(names = { "-c", "--config" }, description = "Path to config file (default: config/mocks.json)")
    private String configPath;

    @Option(names = { "-t", "--target" }, description = "Target URL for proxy mode (e.g., https://api.example.com)")
    private String targetUrl;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            logger.info("Starting J-Proxy v1.0.0");
            logger.info("Mode: {}", mode);
            logger.info("Port: {}", port);

            if (targetUrl != null) {
                logger.info("Target: {}", targetUrl);
            }

            // Create temporary config if target URL is provided
            String effectiveConfigPath = configPath;
            if (targetUrl != null && "proxy".equalsIgnoreCase(mode)) {
                effectiveConfigPath = createTempProxyConfig(targetUrl);
            }

            ProxyServer server = new ProxyServer(port, mode, configPath);
            server.start();

            logger.info("Server started successfully on port {}", port);
            logger.info("Press Ctrl+C to stop");

            // Keep running
            Thread.currentThread().join();

            return 0;
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            return 1;
        }
    }

    private String createTempProxyConfig(String targetUrl) throws IOException {
        File tempConfig = new File("config/temp-proxy-config.json");
        tempConfig.getParentFile().mkdirs();

        String json = String.format("{\"target_url\":\"%s\"}", targetUrl);
        java.nio.file.Files.write(tempConfig.toPath(), json.getBytes());

        return tempConfig.getAbsolutePath();
    }
}