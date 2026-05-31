package com.wd.custapi.service;

import com.wd.custapi.config.FileUploadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // 0755 = rwxr-xr-x. Nginx (or whatever process serves /storage on the VPS)
    // needs read+execute on each path under the shared storage root; without it,
    // documents/site-reports uploaded by the Java process under a restrictive
    // umask come back as 403 to the customer/portal web apps.
    // java:S2612 ("others" read+execute) is intentional and required for static
    // file serving — accepted as safe; tightening to 0750 would 403 the web apps.
    @SuppressWarnings("java:S2612")
    private static final Set<PosixFilePermission> STORAGE_PERMS =
            PosixFilePermissions.fromString("rwxr-xr-x");

    private final Path fileStorageLocation;

    public FileStorageService(FileUploadConfig fileUploadConfig) {
        // Trim any whitespace from the upload directory path
        String uploadDir = fileUploadConfig.getUploadDir().trim();
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            // Generate unique filename
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Create subdirectory if it doesn't exist
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);

            // Copy file to the target location
            Path destinationFile = targetLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            applyStoragePermissions(destinationFile);

            return subDirectory + "/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Set 0755 on a freshly-written file or directory under the storage root.
     * No-op on non-POSIX file systems (Windows dev boxes). IOException is
     * logged at WARN, not thrown — chmod failure shouldn't fail the upload.
     */
    public void applyStoragePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, STORAGE_PERMS);
        } catch (UnsupportedOperationException ignored) {
            // Windows / non-POSIX — chmod has no meaning here.
        } catch (IOException ex) {
            logger.warn("Could not chmod 0755 on {}: {}", path, ex.getMessage());
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path file = this.fileStorageLocation.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + filePath, ex);
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

}
