package com.wd.custapi.config;

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

    @Value("${storageBasePath}")
    private String storageBasePath;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("=== STORAGE PATH VALIDATION ===");
        System.out.println("Configured Storage Path: " + storageBasePath);
        
        try {
            Path path = Paths.get(storageBasePath);
            Path absolutePath = path.toAbsolutePath().normalize();
            
            System.out.println("Absolute Path: " + absolutePath);
            System.out.println("Path Exists: " + Files.exists(path));
            System.out.println("Is Directory: " + Files.isDirectory(path));
            System.out.println("Is Readable: " + Files.isReadable(path));
            System.out.println("Is Writable: " + Files.isWritable(path));
            
            if (!Files.exists(path)) {
                System.err.println("WARNING: Storage path does not exist!");
            } else if (!Files.isDirectory(path)) {
                System.err.println("WARNING: Storage path is not a directory!");
            } else if (!Files.isReadable(path)) {
                System.err.println("WARNING: Storage path is not readable!");
            } else {
                System.out.println("Storage path is valid and accessible!");
                
                // Check for the test PDF file
                Path testPdfPath = path.resolve("projects/1/documents/ground-floor-plan-7294f446-3070-443b-aa62-4cd091d371a6.pdf");
                System.out.println();
                System.out.println("=== TEST PDF FILE ===");
                System.out.println("Path: " + testPdfPath);
                System.out.println("Exists: " + Files.exists(testPdfPath));
                if (Files.exists(testPdfPath)) {
                    System.out.println("Readable: " + Files.isReadable(testPdfPath));
                    System.out.println("Size: " + Files.size(testPdfPath) + " bytes");
                    
                    // Try to read first few bytes
                    try {
                        byte[] firstBytes = Files.readAllBytes(testPdfPath);
                        System.out.println("Can Read File: YES");
                        System.out.println("Actual Size: " + firstBytes.length + " bytes");
                        if (firstBytes.length >= 4) {
                            String header = new String(firstBytes, 0, 4);
                            System.out.println("PDF Header Valid: " + header.equals("%PDF"));
                        }
                    } catch (Exception e) {
                        System.err.println("CANNOT READ FILE: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            // Show Java process user info
            System.out.println();
            System.out.println("=== JAVA PROCESS INFO ===");
            System.out.println("User: " + System.getProperty("user.name"));
            System.out.println("User Home: " + System.getProperty("user.home"));
            System.out.println("Working Dir: " + System.getProperty("user.dir"));
            
        } catch (Exception e) {
            System.err.println("ERROR validating storage path: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("================================");
        System.out.println();
    }
}

