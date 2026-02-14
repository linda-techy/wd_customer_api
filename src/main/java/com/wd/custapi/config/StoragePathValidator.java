package com.wd.custapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates storage path configuration at application startup
 */
@Component
public class StoragePathValidator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StoragePathValidator.class);

    @Value("${storageBasePath}")
    private String storageBasePath;

    @Override
    public void run(String... args) throws Exception {
        logger.info("========================================");
        logger.info("=== STORAGE PATH VALIDATION ===");
        logger.info("Configured Storage Path: {}", storageBasePath);
        
        try {
            Path path = Paths.get(storageBasePath);
            Path absolutePath = path.toAbsolutePath().normalize();
            
            logger.info("Absolute Path: {}", absolutePath);
            logger.info("Path Exists: {}", Files.exists(path));
            logger.info("Is Directory: {}", Files.isDirectory(path));
            logger.info("Is Readable: {}", Files.isReadable(path));
            logger.info("Is Writable: {}", Files.isWritable(path));
            
            if (!Files.exists(path)) {
                logger.warn("WARNING: Storage path does not exist!");
            } else if (!Files.isDirectory(path)) {
                logger.warn("WARNING: Storage path is not a directory!");
            } else if (!Files.isReadable(path)) {
                logger.warn("WARNING: Storage path is not readable!");
            } else {
                logger.info("Storage path is valid and accessible!");
            }
            
            // Show Java process user info
            logger.info("");
            logger.info("=== JAVA PROCESS INFO ===");
            logger.info("User: {}", System.getProperty("user.name"));
            logger.info("User Home: {}", System.getProperty("user.home"));
            logger.info("Working Dir: {}", System.getProperty("user.dir"));
            
        } catch (Exception e) {
            logger.error("ERROR validating storage path: {}", e.getMessage(), e);
        }
        
        logger.info("================================");
        logger.info("");
    }
}

