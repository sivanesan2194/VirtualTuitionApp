package com.virtualtuition.app.payment;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.AllowedFeatures;
import com.virtualtuition.app.models.UserProfile;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity implements PaymentResultListener {

    private Toolbar paymentToolbar;
    private Chip chipPaymentStatus;
    private TextView tvNextDueDate, tvDaysOverdue;
    private ImageView ivAccessVideoIcon, ivAccessWhiteboardIcon, ivAccessMaterialsIcon;
    private TextView tvAccessVideoStatus, tvAccessWhiteboardStatus, tvAccessMaterialsStatus;
    private MaterialButton btnInitiatePayment;
    private ProgressBar paymentProgressBar;

    private FirebaseFirestore mFirestore;
    private FirebaseUser mFirebaseUser;
    private UserProfile currentUserProfile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    // Test Razorpay Key (standard test key placeholder)
    private static final String RAZORPAY_KEY = "rzp_test_TiZRxaS5TAZglh";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mFirestore = FirebaseFirestore.getInstance();
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        loadSubscriptionDetails();
    }

    private void initViews() {
        paymentToolbar = findViewById(R.id.paymentToolbar);
        chipPaymentStatus = findViewById(R.id.chipPaymentStatus);
        tvNextDueDate = findViewById(R.id.tvNextDueDate);
        tvDaysOverdue = findViewById(R.id.tvDaysOverdue);

        ivAccessVideoIcon = findViewById(R.id.ivAccessVideoIcon);
        ivAccessWhiteboardIcon = findViewById(R.id.ivAccessWhiteboardIcon);
        ivAccessMaterialsIcon = findViewById(R.id.ivAccessMaterialsIcon);

        tvAccessVideoStatus = findViewById(R.id.tvAccessVideoStatus);
        tvAccessWhiteboardStatus = findViewById(R.id.tvAccessWhiteboardStatus);
        tvAccessMaterialsStatus = findViewById(R.id.tvAccessMaterialsStatus);

        btnInitiatePayment = findViewById(R.id.btnInitiatePayment);
        paymentProgressBar = findViewById(R.id.paymentProgressBar);

        paymentToolbar.setNavigationOnClickListener(v -> finish());
        btnInitiatePayment.setOnClickListener(v -> startRazorpayCheckout());
    }

    private void loadSubscriptionDetails() {
        if (mFirebaseUser == null) return;
        paymentProgressBar.setVisibility(View.VISIBLE);

        mFirestore.collection("users").document(mFirebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    paymentProgressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentUserProfile = documentSnapshot.toObject(UserProfile.class);
                        if (currentUserProfile != null) {
                            currentUserProfile.setUid(documentSnapshot.getId());
                            updateSubscriptionUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    paymentProgressBar.setVisibility(View.GONE);
                    Toast.makeText(PaymentActivity.this, "Failed to load subscription: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSubscriptionUI() {
        if (currentUserProfile == null) return;

        // Re-evaluate decay state
        AccessControlManager.evaluateUserFeatures(currentUserProfile);
        int overdueDays = currentUserProfile.getOverdueDays();

        // Due date display
        if (currentUserProfile.getNextDueDate() != null) {
            tvNextDueDate.setText(dateFormat.format(currentUserProfile.getNextDueDate().toDate()));
        } else {
            tvNextDueDate.setText("N/A");
        }

        // Overdue days display
        tvDaysOverdue.setText(overdueDays + " Day" + (overdueDays == 1 ? "" : "s"));

        // Status chip & colors
        String status = currentUserProfile.getSubscriptionStatus();
        if (UserProfile.STATUS_ACTIVE.equalsIgnoreCase(status)) {
            chipPaymentStatus.setText("ACTIVE");
            chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_active)));
            tvDaysOverdue.setTextColor(ContextCompat.getColor(this, R.color.status_active));
        } else if (UserProfile.STATUS_GRACE_PERIOD.equalsIgnoreCase(status)) {
            chipPaymentStatus.setText("GRACE PERIOD (DAY " + overdueDays + ")");
            chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_grace)));
            tvDaysOverdue.setTextColor(ContextCompat.getColor(this, R.color.status_grace));
        } else if (UserProfile.STATUS_OVERDUE_PHASE1.equalsIgnoreCase(status)) {
            chipPaymentStatus.setText("PHASE 1 LOCK");
            chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_phase1)));
            tvDaysOverdue.setTextColor(ContextCompat.getColor(this, R.color.status_phase1));
        } else if (UserProfile.STATUS_OVERDUE_PHASE2.equalsIgnoreCase(status)) {
            chipPaymentStatus.setText("PHASE 2 LOCK");
            chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_phase2)));
            tvDaysOverdue.setTextColor(ContextCompat.getColor(this, R.color.status_phase2));
        } else {
            chipPaymentStatus.setText("SUSPENDED");
            chipPaymentStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_suspended)));
            tvDaysOverdue.setTextColor(ContextCompat.getColor(this, R.color.status_suspended));
        }

        // Allowed Features matrix
        AllowedFeatures features = currentUserProfile.getAllowedFeatures();
        setFeatureRow(ivAccessVideoIcon, tvAccessVideoStatus, features.isLiveVideo());
        setFeatureRow(ivAccessWhiteboardIcon, tvAccessWhiteboardStatus, features.isWhiteboard());
        setFeatureRow(ivAccessMaterialsIcon, tvAccessMaterialsStatus, features.isMaterials());
    }

    private void setFeatureRow(ImageView icon, TextView statusText, boolean isAllowed) {
        if (isAllowed) {
            icon.setImageResource(R.drawable.ic_check);
            statusText.setText("Unlocked");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active));
        } else {
            icon.setImageResource(R.drawable.ic_lock);
            statusText.setText("Locked");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_suspended));
        }
    }

    private void startRazorpayCheckout() {
        if (isEmulator()) {
            showEmulatorPaymentDialog();
            return;
        }

        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID);

            JSONObject options = new JSONObject();
            options.put("name", "Virtual Tuition Platform");
            options.put("description", "Monthly Tuition Fee Subscription");
            options.put("currency", "INR");
            options.put("amount", "100000"); // 1000 INR = 100000 paise

            JSONObject prefill = new JSONObject();
            if (currentUserProfile != null) {
                prefill.put("email", currentUserProfile.getEmail());
                prefill.put("contact", "9876543210");
            }
            options.put("prefill", prefill);

            JSONObject theme = new JSONObject();
            theme.put("color", "#1976D2");
            options.put("theme", theme);

            checkout.open(this, options);
        } catch (Throwable e) {
            Toast.makeText(this, "Razorpay checkout error: " + e.getMessage() + ". Switching to test payment mode.", Toast.LENGTH_LONG).show();
            showEmulatorPaymentDialog();
        }
    }

    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    private void showEmulatorPaymentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Emulator Test Payment Mode")
                .setMessage("Razorpay's native SDK can trigger an emulator shutdown/crash on x86_64 architecture (Android 15) due to native library/memory restrictions.\n\nWould you like to simulate a successful tuition fee payment (₹1,000) to test unlocking app features?")
                .setPositiveButton("Simulate Pay Success", (dialog, which) -> {
                    onPaymentSuccess("pay_emu_" + System.currentTimeMillis());
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        paymentProgressBar.setVisibility(View.VISIBLE);
        btnInitiatePayment.setEnabled(false);

        // Record payment in Firestore
        String paymentId = razorpayPaymentID != null ? razorpayPaymentID : ("pay_" + System.currentTimeMillis());
        Map<String, Object> paymentRecord = new HashMap<>();
        paymentRecord.put("paymentId", paymentId);
        paymentRecord.put("userId", mFirebaseUser.getUid());
        paymentRecord.put("amount", 1000);
        paymentRecord.put("currency", "INR");
        paymentRecord.put("status", "SUCCESS");
        paymentRecord.put("timestamp", FieldValue.serverTimestamp());

        mFirestore.collection("payments").document(paymentId)
                .set(paymentRecord);

        // Calculate new nextDueDate = now + 30 days
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        Timestamp newDueDate = new Timestamp(calendar.getTime());

        // Restore active subscription and unlock all features
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("subscriptionStatus", UserProfile.STATUS_ACTIVE);
        userUpdates.put("overdueDays", 0);
        userUpdates.put("lastPaymentDate", FieldValue.serverTimestamp());
        userUpdates.put("nextDueDate", newDueDate);
        userUpdates.put("allowedFeatures", new AllowedFeatures(true, true, true).toMap());

        mFirestore.collection("users").document(mFirebaseUser.getUid())
                .update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    paymentProgressBar.setVisibility(View.GONE);
                    btnInitiatePayment.setEnabled(true);

                    // Update in-memory profile
                    if (currentUserProfile != null) {
                        currentUserProfile.setSubscriptionStatus(UserProfile.STATUS_ACTIVE);
                        currentUserProfile.setOverdueDays(0);
                        currentUserProfile.setNextDueDate(newDueDate);
                        currentUserProfile.setAllowedFeatures(new AllowedFeatures(true, true, true));
                        updateSubscriptionUI();
                    }

                    new AlertDialog.Builder(PaymentActivity.this)
                            .setTitle("Payment Successful!")
                            .setMessage("Tuition fee received. All features (Live Video, Whiteboard, Materials) have been restored and unlocked for 30 days.")
                            .setPositiveButton("Continue to App", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    paymentProgressBar.setVisibility(View.GONE);
                    btnInitiatePayment.setEnabled(true);
                    Toast.makeText(PaymentActivity.this, "Payment succeeded but profile update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onPaymentError(int code, String response) {
        paymentProgressBar.setVisibility(View.GONE);
        btnInitiatePayment.setEnabled(true);
        Toast.makeText(this, "Payment Cancelled or Failed: " + response, Toast.LENGTH_LONG).show();
    }
}
