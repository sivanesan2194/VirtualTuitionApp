package com.virtualtuition.app.payment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.virtualtuition.app.models.AllowedFeatures;
import com.virtualtuition.app.models.UserProfile;
import java.util.Date;

/**
 * AccessControlManager:
 * Implements Tiered Feature Decay System based on overdue tuition fees.
 *
 * Rules:
 * - Day 0 (Due Date): Full access, prompt fee alert.
 * - Days 1-3 (Grace Period): Full access active, warning banners.
 * - Day 4 (Phase 1 Lock): Live Video Classes locked (liveVideo = false).
 * - Day 7 (Phase 2 Lock): Whiteboard & Materials locked (whiteboard = false, materials = false).
 * - Day 15 (Full Suspension): Account suspended. Redirect all entry to PaymentActivity.
 */
public class AccessControlManager {

    public static final int PHASE_ACTIVE = 0;
    public static final int PHASE_GRACE = 1;
    public static final int PHASE_LOCK_VIDEO = 4;
    public static final int PHASE_LOCK_ALL = 7;
    public static final int PHASE_SUSPENDED = 15;

    /**
     * Calculates the number of days overdue based on nextDueDate vs current time.
     */
    public static int calculateOverdueDays(UserProfile user) {
        if (user == null || user.isTeacher() || user.getNextDueDate() == null) {
            return 0;
        }

        long dueMillis = user.getNextDueDate().toDate().getTime();
        long nowMillis = System.currentTimeMillis();

        if (nowMillis <= dueMillis) {
            return 0;
        }

        long diff = nowMillis - dueMillis;
        return (int) (diff / (1000L * 60 * 60 * 24));
    }

    /**
     * Evaluates the current decay state, updates the user's AllowedFeatures map,
     * and synchronizes with Firestore if changed.
     */
    public static void evaluateUserFeatures(UserProfile user) {
        if (user == null) return;

        // Teachers have unconstrained access
        if (user.isTeacher()) {
            user.setSubscriptionStatus(UserProfile.STATUS_ACTIVE);
            user.setOverdueDays(0);
            user.setAllowedFeatures(new AllowedFeatures(true, true, true));
            return;
        }

        int overdueDays = calculateOverdueDays(user);
        user.setOverdueDays(overdueDays);

        AllowedFeatures features = new AllowedFeatures();

        if (overdueDays <= 0) {
            user.setSubscriptionStatus(UserProfile.STATUS_ACTIVE);
            features.setLiveVideo(true);
            features.setWhiteboard(true);
            features.setMaterials(true);
        } else if (overdueDays <= 3) {
            user.setSubscriptionStatus(UserProfile.STATUS_GRACE_PERIOD);
            features.setLiveVideo(true);
            features.setWhiteboard(true);
            features.setMaterials(true);
        } else if (overdueDays < 7) {
            user.setSubscriptionStatus(UserProfile.STATUS_OVERDUE_PHASE1);
            features.setLiveVideo(false);
            features.setWhiteboard(true);
            features.setMaterials(true);
        } else if (overdueDays < 15) {
            user.setSubscriptionStatus(UserProfile.STATUS_OVERDUE_PHASE2);
            features.setLiveVideo(false);
            features.setWhiteboard(false);
            features.setMaterials(false);
        } else {
            user.setSubscriptionStatus(UserProfile.STATUS_SUSPENDED);
            features.setLiveVideo(false);
            features.setWhiteboard(false);
            features.setMaterials(false);
        }

        user.setAllowedFeatures(features);
    }

    /**
     * Enforces app entry access.
     * If the account is suspended (Day 15+), redirects immediately to PaymentActivity.
     * Returns true if entry allowed, false if redirected to Payment.
     */
    public static boolean enforceEntryAccess(Activity activity, UserProfile user) {
        if (user == null || user.isTeacher()) {
            return true;
        }

        evaluateUserFeatures(user);

        if (isSuspended(user)) {
            Intent intent = new Intent(activity, PaymentActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return false;
        }

        return true;
    }

    public static boolean isLiveVideoAllowed(UserProfile user) {
        if (user == null || user.isTeacher()) return true;
        evaluateUserFeatures(user);
        return user.getAllowedFeatures().isLiveVideo();
    }

    public static boolean isWhiteboardAllowed(UserProfile user) {
        if (user == null || user.isTeacher()) return true;
        evaluateUserFeatures(user);
        return user.getAllowedFeatures().isWhiteboard();
    }

    public static boolean isMaterialsAllowed(UserProfile user) {
        if (user == null || user.isTeacher()) return true;
        evaluateUserFeatures(user);
        return user.getAllowedFeatures().isMaterials();
    }

    public static boolean isSuspended(UserProfile user) {
        if (user == null || user.isTeacher()) return false;
        return calculateOverdueDays(user) >= PHASE_SUSPENDED
                || UserProfile.STATUS_SUSPENDED.equalsIgnoreCase(user.getSubscriptionStatus());
    }

    /**
     * Displays a standardized Feature Locked dialog with a direct button to PaymentActivity.
     */
    public static void showLockedFeatureDialog(Context context, String featureName, String explanation) {
        new AlertDialog.Builder(context)
                .setTitle("Feature Locked: " + featureName)
                .setMessage(explanation)
                .setPositiveButton("Pay Tuition Fee", (dialog, which) -> {
                    Intent intent = new Intent(context, PaymentActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    /**
     * Syncs updated status and allowedFeatures back to Firestore.
     */
    public static void syncFeaturesToFirestore(UserProfile user) {
        if (user == null || user.getUid() == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(
                        "subscriptionStatus", user.getSubscriptionStatus(),
                        "overdueDays", user.getOverdueDays(),
                        "allowedFeatures", user.getAllowedFeatures().toMap()
                );
    }
}
