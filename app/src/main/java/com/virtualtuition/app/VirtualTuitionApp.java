package com.virtualtuition.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.firebase.FirebaseApp;
import com.razorpay.Checkout;

public class VirtualTuitionApp extends Application {

    public static final String CHANNEL_ID_ALERTS = "tuition_fee_alerts";

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Preload Razorpay Checkout safely
        try {
            Checkout.preload(getApplicationContext());
        } catch (Throwable t) {
            // Ignore preload errors on emulator or devices lacking native webview acceleration
        }

        // Create Notification Channel for fee alerts & class updates
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Tuition & Classroom Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for fee due dates, class alerts, and subscription status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
