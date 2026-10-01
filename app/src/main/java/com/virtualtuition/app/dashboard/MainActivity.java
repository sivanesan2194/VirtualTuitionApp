package com.virtualtuition.app.dashboard;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.virtualtuition.app.R;
import com.virtualtuition.app.auth.LoginActivity;
import com.virtualtuition.app.classroom.ClassroomActivity;
import com.virtualtuition.app.live.VideoClassActivity;
import com.virtualtuition.app.models.Classroom;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import com.virtualtuition.app.payment.PaymentActivity;
import com.virtualtuition.app.whiteboard.WhiteboardActivity;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ClassroomAdapter.OnClassroomClickListener {

    private TextView tvUserWelcome, tvUserRoleBadge;
    private Chip chipSubStatus;
    private ImageButton btnLogout;
    private CardView cardWarningBanner, cardWhiteboard, cardLiveVideo, cardSubscription, cardActionClass;
    private TextView tvBannerTitle, tvBannerMessage;
    private MaterialButton btnBannerPay;
    private ImageView ivWhiteboardIcon, ivLiveVideoIcon, ivClassActionIcon;
    private TextView tvWhiteboardStatus, tvLiveVideoStatus, tvFeeStatusText, tvClassActionTitle, tvClassActionSub;
    private TextView tvClassroomCount, tvEmptyClassrooms;
    private RecyclerView rvClassrooms;
    private SwipeRefreshLayout swipeRefreshClassrooms;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser mFirebaseUser;
    private UserProfile currentUserProfile;
    private ClassroomAdapter classroomAdapter;
    private ListenerRegistration classroomsListener;

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mFirebaseUser = mAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        initViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfileAndData();
    }

    private void initViews() {
        tvUserWelcome = findViewById(R.id.tvUserWelcome);
        tvUserRoleBadge = findViewById(R.id.tvUserRoleBadge);
        chipSubStatus = findViewById(R.id.chipSubStatus);
        btnLogout = findViewById(R.id.btnLogout);

        cardWarningBanner = findViewById(R.id.cardWarningBanner);
        tvBannerTitle = findViewById(R.id.tvBannerTitle);
        tvBannerMessage = findViewById(R.id.tvBannerMessage);
        btnBannerPay = findViewById(R.id.btnBannerPay);

        cardWhiteboard = findViewById(R.id.cardWhiteboard);
        ivWhiteboardIcon = findViewById(R.id.ivWhiteboardIcon);
        tvWhiteboardStatus = findViewById(R.id.tvWhiteboardStatus);

        cardLiveVideo = findViewById(R.id.cardLiveVideo);
        ivLiveVideoIcon = findViewById(R.id.ivLiveVideoIcon);
        tvLiveVideoStatus = findViewById(R.id.tvLiveVideoStatus);

        cardSubscription = findViewById(R.id.cardSubscription);
        tvFeeStatusText = findViewById(R.id.tvFeeStatusText);

        cardActionClass = findViewById(R.id.cardActionClass);
        ivClassActionIcon = findViewById(R.id.ivClassActionIcon);
        tvClassActionTitle = findViewById(R.id.tvClassActionTitle);
        tvClassActionSub = findViewById(R.id.tvClassActionSub);

        tvClassroomCount = findViewById(R.id.tvClassroomCount);
        tvEmptyClassrooms = findViewById(R.id.tvEmptyClassrooms);
        rvClassrooms = findViewById(R.id.rvClassrooms);
        swipeRefreshClassrooms = findViewById(R.id.swipeRefreshClassrooms);
    }

    private void setupRecyclerView() {
        classroomAdapter = new ClassroomAdapter(this, this);
        rvClassrooms.setLayoutManager(new LinearLayoutManager(this));
        rvClassrooms.setAdapter(classroomAdapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> performLogout());

        btnBannerPay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
            startActivity(intent);
        });

        cardSubscription.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PaymentActivity.class);
            startActivity(intent);
        });

        cardWhiteboard.setOnClickListener(v -> {
            if (currentUserProfile == null) return;
            if (AccessControlManager.isWhiteboardAllowed(currentUserProfile)) {
                Intent intent = new Intent(MainActivity.this, WhiteboardActivity.class);
                startActivity(intent);
            } else {
                AccessControlManager.showLockedFeatureDialog(
                        MainActivity.this,
                        "Drawpoint Whiteboard",
                        "Whiteboard access is locked due to overdue tuition fees (Phase 2 lock). Please pay your fee to continue using the whiteboard."
                );
            }
        });

        cardLiveVideo.setOnClickListener(v -> {
            if (currentUserProfile == null) return;
            if (AccessControlManager.isLiveVideoAllowed(currentUserProfile)) {
                Intent intent = new Intent(MainActivity.this, VideoClassActivity.class);
                intent.putExtra("roomId", "General_Campus_Room");
                intent.putExtra("roomTitle", "General Campus Live Room");
                startActivity(intent);
            } else {
                AccessControlManager.showLockedFeatureDialog(
                        MainActivity.this,
                        "Live Video Classes",
                        "Live video classes are locked due to overdue tuition fees (Phase 1 lock). Please pay your fee to restore video class access."
                );
            }
        });

        cardActionClass.setOnClickListener(v -> {
            if (currentUserProfile == null) return;
            if (currentUserProfile.isTeacher()) {
                showCreateClassroomDialog();
            } else {
                showJoinClassroomDialog();
            }
        });

        swipeRefreshClassrooms.setOnRefreshListener(this::loadUserProfileAndData);
    }

    private void loadUserProfileAndData() {
        swipeRefreshClassrooms.setRefreshing(true);
        mFirestore.collection("users").document(mFirebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserProfile = documentSnapshot.toObject(UserProfile.class);
                        if (currentUserProfile != null) {
                            currentUserProfile.setUid(documentSnapshot.getId());
                            applyUserProfileUI();
                            loadClassrooms();
                        }
                    } else {
                        swipeRefreshClassrooms.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefreshClassrooms.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyUserProfileUI() {
        if (currentUserProfile == null) return;

        // Day 15 check: Redirect directly to PaymentActivity if account is suspended!
        boolean allowedEntry = AccessControlManager.enforceEntryAccess(this, currentUserProfile);
        if (!allowedEntry) {
            return;
        }

        tvUserWelcome.setText("Hello, " + (currentUserProfile.getName() != null ? currentUserProfile.getName() : "User") + "!");
        boolean isTeacher = currentUserProfile.isTeacher();
        tvUserRoleBadge.setText(isTeacher ? "Teacher Account" : "Student Account");

        if (isTeacher) {
            // Teachers are exempt from decay
            chipSubStatus.setText("Teacher Access");
            chipSubStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            cardWarningBanner.setVisibility(View.GONE);
            tvClassActionTitle.setText("Create Class");
            tvClassActionSub.setText("Generate 6-char code");
            tvFeeStatusText.setText("Teacher Account");
        } else {
            // Student: Check tiered decay status
            int overdueDays = AccessControlManager.calculateOverdueDays(currentUserProfile);
            tvClassActionTitle.setText("Join Class");
            tvClassActionSub.setText("Enter 6-char code");

            if (overdueDays <= 0) {
                chipSubStatus.setText("Active");
                chipSubStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_active)));
                cardWarningBanner.setVisibility(View.GONE);
                tvFeeStatusText.setText("Fee Paid");
            } else if (overdueDays <= 3) {
                // Days 1-3: Grace period
                chipSubStatus.setText("Grace Period (Day " + overdueDays + ")");
                chipSubStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_grace)));
                cardWarningBanner.setVisibility(View.VISIBLE);
                tvBannerTitle.setText("Tuition Fee Due (Grace Period Active)");
                tvBannerMessage.setText("Your tuition fee is overdue by " + overdueDays + " day(s). Full access is still active, but will begin decaying soon.");
                tvFeeStatusText.setText("Grace: Day " + overdueDays);
            } else if (overdueDays < 7) {
                // Day 4: Phase 1 Lock
                chipSubStatus.setText("Phase 1 Lock");
                chipSubStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_phase1)));
                cardWarningBanner.setVisibility(View.VISIBLE);
                tvBannerTitle.setText("Feature Decay: Live Video Locked");
                tvBannerMessage.setText("Fee overdue by " + overdueDays + " days. Live video classes are suspended. Pay now to restore.");
                tvFeeStatusText.setText("Video Locked");
            } else {
                // Day 7: Phase 2 Lock
                chipSubStatus.setText("Phase 2 Lock");
                chipSubStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_phase2)));
                cardWarningBanner.setVisibility(View.VISIBLE);
                tvBannerTitle.setText("Feature Decay: Whiteboard & Materials Locked");
                tvBannerMessage.setText("Fee overdue by " + overdueDays + " days. Whiteboard and material downloads are suspended.");
                tvFeeStatusText.setText("Whiteboard Locked");
            }

            // Update visual locks on cards
            boolean videoAllowed = AccessControlManager.isLiveVideoAllowed(currentUserProfile);
            ivLiveVideoIcon.setImageResource(videoAllowed ? R.drawable.ic_video : R.drawable.ic_lock);
            tvLiveVideoStatus.setText(videoAllowed ? "Video Conference" : "LOCKED (Day 4+)");

            boolean whiteboardAllowed = AccessControlManager.isWhiteboardAllowed(currentUserProfile);
            ivWhiteboardIcon.setImageResource(whiteboardAllowed ? R.drawable.ic_whiteboard : R.drawable.ic_lock);
            tvWhiteboardStatus.setText(whiteboardAllowed ? "Draw & Share" : "LOCKED (Day 7+)");
        }
    }

    private void loadClassrooms() {
        if (currentUserProfile == null) return;

        if (classroomsListener != null) {
            classroomsListener.remove();
        }

        Query query;
        if (currentUserProfile.isTeacher()) {
            query = mFirestore.collection("classrooms")
                    .whereEqualTo("teacherId", currentUserProfile.getUid());
        } else {
            query = mFirestore.collection("classrooms")
                    .whereArrayContains("studentIds", currentUserProfile.getUid());
        }

        classroomsListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            swipeRefreshClassrooms.setRefreshing(false);
            if (error != null) {
                Toast.makeText(MainActivity.this, "Error fetching classrooms: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots != null) {
                List<Classroom> classrooms = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Classroom c = doc.toObject(Classroom.class);
                    if (c != null) {
                        c.setClassroomId(doc.getId());
                        classrooms.add(c);
                    }
                }
                classroomAdapter.setClassrooms(classrooms);
                tvClassroomCount.setText(classrooms.size() + " Classroom" + (classrooms.size() == 1 ? "" : "s"));
                tvEmptyClassrooms.setVisibility(classrooms.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showCreateClassroomDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_classroom, null);
        EditText etName = dialogView.findViewById(R.id.etCreateClassName);
        EditText etSubject = dialogView.findViewById(R.id.etCreateSubject);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String subject = etSubject.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(MainActivity.this, "Classroom name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String classCode = generateClassCode();
                    String classroomId = mFirestore.collection("classrooms").document().getId();

                    Classroom newClassroom = new Classroom(
                            classroomId,
                            name,
                            subject,
                            currentUserProfile.getUid(),
                            currentUserProfile.getName(),
                            classCode
                    );

                    mFirestore.collection("classrooms").document(classroomId)
                            .set(newClassroom.toMap())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Classroom created! Code: " + classCode, Toast.LENGTH_LONG).show();
                                loadClassrooms();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Failed to create classroom: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJoinClassroomDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_classroom, null);
        EditText etCode = dialogView.findViewById(R.id.etJoinCode);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = etCode.getText().toString().trim().toUpperCase();
                    if (code.length() != 6) {
                        Toast.makeText(MainActivity.this, "Code must be exactly 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Look up classroom with this code
                    mFirestore.collection("classrooms")
                            .whereEqualTo("classCode", code)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (queryDocumentSnapshots.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Invalid Classroom Code", Toast.LENGTH_SHORT).show();
                                } else {
                                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                                    doc.getReference().update("studentIds", FieldValue.arrayUnion(currentUserProfile.getUid()))
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(MainActivity.this, "Enrolled successfully!", Toast.LENGTH_SHORT).show();
                                                loadClassrooms();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(MainActivity.this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String generateClassCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Override
    public void onClassroomClick(Classroom classroom) {
        Intent intent = new Intent(MainActivity.this, ClassroomActivity.class);
        intent.putExtra("classroom", classroom);
        intent.putExtra("userProfile", currentUserProfile);
        startActivity(intent);
    }

    @Override
    public void onLiveClassClick(Classroom classroom) {
        if (currentUserProfile == null) return;
        if (AccessControlManager.isLiveVideoAllowed(currentUserProfile)) {
            Intent intent = new Intent(MainActivity.this, VideoClassActivity.class);
            intent.putExtra("roomId", "Classroom_" + classroom.getClassroomId());
            intent.putExtra("roomTitle", classroom.getClassName() + " Live Class");
            startActivity(intent);
        } else {
            AccessControlManager.showLockedFeatureDialog(
                    MainActivity.this,
                    "Live Video Classes",
                    "Live video access is locked due to overdue fees (Phase 1 lock). Pay tuition to restore live class access."
            );
        }
    }

    private void performLogout() {
        mAuth.signOut();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classroomsListener != null) {
            classroomsListener.remove();
        }
    }
}
