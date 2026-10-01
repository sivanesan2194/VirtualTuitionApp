package com.virtualtuition.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.virtualtuition.app.R;
import com.virtualtuition.app.dashboard.MainActivity;
import com.virtualtuition.app.models.AllowedFeatures;
import com.virtualtuition.app.models.UserProfile;
import java.util.Calendar;
import java.util.Date;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etRegEmail, etRegPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbTeacher;
    private MaterialButton btnRegister;
    private TextView tvSignInBack;
    private ProgressBar regProgressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        rgRole = findViewById(R.id.rgRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbTeacher = findViewById(R.id.rbTeacher);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignInBack = findViewById(R.id.tvSignInBack);
        regProgressBar = findViewById(R.id.regProgressBar);

        btnRegister.setOnClickListener(v -> performRegistration());
        tvSignInBack.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String role = rbTeacher.isChecked() ? UserProfile.ROLE_TEACHER : UserProfile.ROLE_STUDENT;

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etRegPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Update Firebase Auth display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build();
                        user.updateProfile(profileUpdates);

                        // Build User Profile model
                        UserProfile userProfile = new UserProfile(user.getUid(), name, email, role);

                        if (UserProfile.ROLE_STUDENT.equals(role)) {
                            // Initialize student subscription with 30-day active period
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.DAY_OF_MONTH, 30);
                            userProfile.setNextDueDate(new Timestamp(cal.getTime()));
                            userProfile.setLastPaymentDate(Timestamp.now());
                            userProfile.setSubscriptionStatus(UserProfile.STATUS_ACTIVE);
                            userProfile.setOverdueDays(0);
                            userProfile.setAllowedFeatures(new AllowedFeatures(true, true, true));
                        } else {
                            // Teachers always have full access
                            userProfile.setAllowedFeatures(new AllowedFeatures(true, true, true));
                        }

                        // Attach FCM Token if available
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                userProfile.setFcmToken(task.getResult());
                            }
                            saveProfileAndProceed(userProfile);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveProfileAndProceed(UserProfile profile) {
        mFirestore.collection("users")
                .document(profile.getUid())
                .set(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        regProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
