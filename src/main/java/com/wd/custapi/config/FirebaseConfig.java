package com.wd.custapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK initialisation for the Customer API.
 *
 * SETUP REQUIRED:
 *   1. Go to Firebase Console → Project Settings → Service Accounts
 *   2. Click "Generate new private key" and download the JSON file.
 *   3. Rename it to "firebase-service-account.json" and place it at:
 *        src/main/resources/firebase-service-account.json
 *   4. Ensure that file is NOT committed to version control
 *      (add it to .gitignore).
 *
 * If the service account file is absent the app still starts — push
 * notifications will be silently disabled (logged at WARN level).
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String SERVICE_ACCOUNT_PATH = "/firebase-service-account.json";

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.debug("Firebase already initialised — skipping.");
            return;
        }

        try (InputStream serviceAccount =
                     FirebaseConfig.class.getResourceAsStream(SERVICE_ACCOUNT_PATH)) {

            if (serviceAccount == null) {
                logger.warn("firebase-service-account.json not found on classpath. " +
                        "Push notifications will be DISABLED. " +
                        "Place the file at src/main/resources/firebase-service-account.json.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase Admin SDK initialised successfully (customer-api).");

        } catch (IOException e) {
            logger.error("Failed to initialise Firebase Admin SDK: {}. " +
                    "Push notifications will be DISABLED.", e.getMessage());
        }
    }
}
