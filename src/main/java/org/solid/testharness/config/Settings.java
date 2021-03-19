package org.solid.testharness.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class Settings {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.Settings");

    private static Settings INSTANCE;
    private String configPath;
    private String credentialsPath;
    private String targetServer;

    public synchronized static Settings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Settings();
        }
        return INSTANCE;
    }

    // the settings are taken in the following order of preference: system property, local-config, config
    // Run from IDE: unless env set in IDE, it is not present so load from local-config
    // Run from Gradle: it passes through system properties, use defaults (inc from local-config) if not set

    public void loadSystemProperties() {
        configPath = System.getProperty("config");
        credentialsPath = System.getProperty("credentials");
        targetServer = System.getProperty("karate.env");
    }

    public void loadLocalProperties() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        try (Reader fr = new FileReader(workingDirectory.resolve("local.properties").toFile())) {
            Properties prop = new Properties();
            prop.load(fr);
            if (targetServer == null && prop.containsKey("env")) {
                targetServer = prop.getProperty("env");
            }
            if (credentialsPath == null && prop.containsKey("credentials")) {
                credentialsPath = prop.getProperty("credentials");
            }
            if (configPath == null && prop.containsKey("config")) {
                configPath = workingDirectory.resolve(prop.getProperty("config")).toString();
            }
        } catch (IOException e) {
            logger.debug("Local properties file not loaded {}", e.getMessage());
        }

    }

    public File getConfigFile() {
        return new File(configPath);
    }

    public File getCredentialsPath() {
        return new File(credentialsPath);
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void logSettings() {
        logger.info("Config filename {}", configPath);
        logger.info("Credentials path {}", credentialsPath);
        logger.info("Target server: {}", targetServer);
        logger.info("Options {}", System.getProperty("karate.options"));
    }
}