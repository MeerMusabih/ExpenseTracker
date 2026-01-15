package com.mycompany.expensetracker;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;

public class FirebaseService {

    private static Firestore firestore;

    public static void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = FirebaseService.class.getClassLoader().getResourceAsStream("serviceAccountKey.json");
            
            if (serviceAccount == null) {
                throw new IOException("serviceAccountKey.json not found in resources");
            }

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        }
        firestore = FirestoreClient.getFirestore();
    }

    public static Firestore getFirestore() {
        if (firestore == null) {
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize Firebase", e);
            }
        }
        return firestore;
    }
}
