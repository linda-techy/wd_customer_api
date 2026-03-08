package com.wd.custapi.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Logs critical startup diagnostics and ensures the log directory exists.
 *
 * Runs after all beans are initialized (ApplicationRunner) so all config values
 * are available. Also auto-creates the log directory if it doesn't exist.
 *
 * Example output on startup:
 *   STARTUP | Java: 21.0.8 | Profile: production | Port: 8081 | DB: jdbc:postgresql://host/db
 */
@Component
@Order(1)
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private static final String LOG_DIR = "/home/backenduser/logs/customer-api";

    private final Environment environment;

    @Value("${spring.datasource.url:NOT_CONFIGURED}")
    private String dbUrl;

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${spring.application.name:cust-api}")
    private String appName;

    public StartupLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureLogDirectoryExists();
        logStartupDiagnostics();
    }

    private void ensureLogDirectoryExists() {
        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                Files.createDirectories(logPath);
                log.info("{} | Log directory created: {}", LoggingConstants.PREFIX_STARTUP, LOG_DIR);
            }

            // Also create the archive subdirectory for rotated logs
            Path archivePath = Paths.get(LOG_DIR, "archived");
            if (!Files.exists(archivePath)) {
                Files.createDirectories(archivePath);
            }

            // Verify write permissions
            File logDir = logPath.toFile();
            if (!logDir.canWrite()) {
                log.error("{} | Log directory is not writable: {}", LoggingConstants.PREFIX_STARTUP, LOG_DIR);
            }
        } catch (Exception e) {
            // Don't fail startup over logging — just warn
            log.warn("{} | Could not create log directory '{}': {}",
                    LoggingConstants.PREFIX_STARTUP, LOG_DIR, e.getMessage());
        }
    }

    private void logStartupDiagnostics() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";

        // Mask password from DB URL for logging
        String safeDbUrl = maskDbUrl(dbUrl);

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║        {} — STARTUP DIAGNOSTICS                 ║", appName);
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  Java Version  : {}",      System.getProperty("java.version"));
        log.info("║  Spring Profile: {}",      profile);
        log.info("║  Server Port   : {}",      serverPort);
        log.info("║  Database URL  : {}",      safeDbUrl);
        log.info("║  Log Directory : {}",      LOG_DIR);
        log.info("║  App Version   : {}",      "1.0.0-SNAPSHOT");
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    /** Mask password from JDBC URL if it somehow appears in the URL string */
    private String maskDbUrl(String url) {
        if (url == null) return "NOT_CONFIGURED";
        return url.replaceAll("(?i)(password=)[^&;]+", "$1****");
    }
}
