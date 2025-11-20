package com.jproxy.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jproxy.model.MockConfig;
import com.jproxy.model.ProxyConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static MockConfig loadMockConfig(String configPath) throws IOException {
        if (configPath == null || configPath.isEmpty()) {
            // Try default location
            configPath = "config/mocks.json";
        }

        File configFile = new File(configPath);

        if (!configFile.exists()) {
            logger.warn("Config file not found at {}, using default config", configPath);
            return loadDefaultConfig();
        }

        logger.info("Loading mock config from {}", configPath);
        MockConfig config = mapper.readValue(configFile, MockConfig.class);
        logger.info("Loaded {} mock endpoints", config.getEndpoints().size());

        return config;
    }

    private static MockConfig loadDefaultConfig() throws IOException {
        logger.info("Loading default mock config from resources");
        InputStream is = Config.class.getClassLoader()
                .getResourceAsStream("default-mocks.json");

        if (is == null) {
            logger.warn("No default config found, returning empty config");
            return new MockConfig();
        }

        return mapper.readValue(is, MockConfig.class);
    }

    public static ProxyConfig loadProxyConfig(String configPath) throws IOException {
        if (configPath == null || configPath.isEmpty()) {
            configPath = "config/proxy-config.json";
        }

        File configFile = new File(configPath);

        if (!configFile.exists()) {
            logger.warn("Proxy config not found at {}, using defaults", configPath);
            return new ProxyConfig();
        }

        logger.info("Loading proxy config from {}", configPath);
        return mapper.readValue(configFile, ProxyConfig.class);
    }
}