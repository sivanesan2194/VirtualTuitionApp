package com.virtualtuition.app.classroom;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.virtualtuition.app.R;
import com.virtualtuition.app.live.VideoClassActivity;
import com.virtualtuition.app.models.Classroom;
import com.virtualtuition.app.models.MaterialItem;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassroomActivity extends AppCompatActivity {

    private Toolbar classroomToolbar;
    private TextView tvToolbarClassName, tvToolbarClassCode, tvEmptyMaterials;
    private ImageButton btnJoinClassVideo;
    private TabLayout tabLayoutClassroom;

    // Materials View
    private RecyclerView rvMaterials;
    private SwipeRefreshLayout swipeRefreshMaterials;
    private ProgressBar progressBarMaterials;

    // Enrolled Students View
    private LinearLayout layoutStudentsContainer;
    private TextView tvStudentSummaryHeader, tvEmptyStudents;
    private RecyclerView rvStudents;
    private ProgressBar progressBarStudents;

    private FloatingActionButton fabUploadMaterial;

    private Classroom currentClassroom;
    private UserProfile currentUserProfile;
    private MaterialAdapter materialAdapter;
    private StudentAdapter studentAdapter;
    private ListenerRegistration materialsListener;

    private Uri selectedFileUri = null;
    private String selectedFileName = "No file chosen";
    private TextView dialogTvSelectedFileName;
    private ProgressBar dialogUploadProgressBar;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    selectedFileName = queryFileName(uri);
                    if (dialogTvSelectedFileName != null) {
                        dialogTvSelectedFileName.setText(selectedFileName);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);

        currentClassroom = (Classroom) getIntent().getSerializableExtra("classroom");
        currentUserProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");

        if (currentClassroom == null) {
            Toast.makeText(this, "Classroom not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        startMaterialsRealtimeListener();
        loadEnrolledStudents();
    }

    private void initViews() {
        classroomToolbar = findViewById(R.id.classroomToolbar);
        tvToolbarClassName = findViewById(R.id.tvToolbarClassName);
        tvToolbarClassCode = findViewById(R.id.tvToolbarClassCode);
        btnJoinClassVideo = findViewById(R.id.btnJoinClassVideo);
        tabLayoutClassroom = findViewById(R.id.tabLayoutClassroom);

        // Materials View
        rvMaterials = findViewById(R.id.rvMaterials);
        swipeRefreshMaterials = findViewById(R.id.swipeRefreshMaterials);
        progressBarMaterials = findViewById(R.id.progressBarMaterials);
        tvEmptyMaterials = findViewById(R.id.tvEmptyMaterials);

        // Students View
        layoutStudentsContainer = findViewById(R.id.layoutStudentsContainer);
        tvStudentSummaryHeader = findViewById(R.id.tvStudentSummaryHeader);
        tvEmptyStudents = findViewById(R.id.tvEmptyStudents);
        rvStudents = findViewById(R.id.rvStudents);
        progressBarStudents = findViewById(R.id.progressBarStudents);

        fabUploadMaterial = findViewById(R.id.fabUploadMaterial);

        tvToolbarClassName.setText(currentClassroom.getClassName());
        tvToolbarClassCode.setText("Code: " + currentClassroom.getClassCode());

        // Adapters
        materialAdapter = new MaterialAdapter(this, currentUserProfile);
        rvMaterials.setLayoutManager(new LinearLayoutManager(this));
        rvMaterials.setAdapter(materialAdapter);

        studentAdapter = new StudentAdapter(this);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);

        // Both teachers and students can upload materials
        fabUploadMaterial.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        classroomToolbar.setNavigationOnClickListener(v -> finish());

        btnJoinClassVideo.setOnClickListener(v -> {
            if (currentUserProfile == null) return;
            if (AccessControlManager.isLiveVideoAllowed(currentUserProfile)) {
                Intent intent = new Intent(ClassroomActivity.this, VideoClassActivity.class);
                intent.putExtra("roomId", "Classroom_" + currentClassroom.getClassroomId());
                intent.putExtra("roomTitle", currentClassroom.getClassName() + " Live Class");
                startActivity(intent);
            } else {
                AccessControlManager.showLockedFeatureDialog(
                        ClassroomActivity.this,
                        "Live Video Classes",
                        "Live video access is locked due to overdue fees (Phase 1 lock). Pay tuition to restore live class access."
                );
            }
        });

        tabLayoutClassroom.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Materials View
                    swipeRefreshMaterials.setVisibility(View.VISIBLE);
                    layoutStudentsContainer.setVisibility(View.GONE);
                    fabUploadMaterial.setVisibility(View.VISIBLE);
                } else {
                    // Enrolled Students View
                    swipeRefreshMaterials.setVisibility(View.GONE);
                    layoutStudentsContainer.setVisibility(View.VISIBLE);
                    fabUploadMaterial.setVisibility(View.GONE);
                    loadEnrolledStudents();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabUploadMaterial.setOnClickListener(v -> {
            if (currentUserProfile == null) return;

            // Check if student has materials feature locked due to overdue fees
            if (!currentUserProfile.isTeacher() && !AccessControlManager.isMaterialsAllowed(currentUserProfile)) {
                AccessControlManager.showLockedFeatureDialog(
                        ClassroomActivity.this,
                        "Upload Materials",
                        "Material uploads are locked due to overdue tuition fees (Phase 2 lock). Please pay your fee to upload study materials."
                );
                return;
            }

            showUploadDialog();
        });

        swipeRefreshMaterials.setOnRefreshListener(() -> {
            swipeRefreshMaterials.setRefreshing(false);
        });
    }

    private void startMaterialsRealtimeListener() {
        progressBarMaterials.setVisibility(View.VISIBLE);
        materialsListener = FirebaseFirestore.getInstance()
                .collection("classrooms")
                .document(currentClassroom.getClassroomId())
                .collection("materials")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBarMaterials.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(ClassroomActivity.this, "Failed to load materials: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        List<MaterialItem> items = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            MaterialItem item = doc.toObject(MaterialItem.class);
                            if (item != null) {
                                item.setMaterialId(doc.getId());
                                items.add(item);
                            }
                        }
                        materialAdapter.setMaterials(items);
                        tvEmptyMaterials.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void loadEnrolledStudents() {
        List<String> studentIds = currentClassroom.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            progressBarStudents.setVisibility(View.GONE);
            tvEmptyStudents.setVisibility(View.VISIBLE);
            tvStudentSummaryHeader.setText("Enrolled Students (0)");
            studentAdapter.setStudents(new ArrayList<>());
            return;
        }

        progressBarStudents.setVisibility(View.VISIBLE);
        tvEmptyStudents.setVisibility(View.GONE);

        // Firestore whereIn supports up to 30 elements
        List<String> queryIds = studentIds.subList(0, Math.min(studentIds.size(), 30));

        FirebaseFirestore.getInstance()
                .collection("users")
                .whereIn("uid", queryIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBarStudents.setVisibility(View.GONE);
                    List<UserProfile> students = new ArrayList<>();
                    int activeCount = 0;
                    int overdueCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        UserProfile student = doc.toObject(UserProfile.class);
                        if (student != null) {
                            student.setUid(doc.getId());
                            students.add(student);

                            AccessControlManager.evaluateUserFeatures(student);
                            if (student.getOverdueDays() <= 0) {
                                activeCount++;
                            } else {
                                overdueCount++;
                            }
                        }
                    }

                    studentAdapter.setStudents(students);
                    tvEmptyStudents.setVisibility(students.isEmpty() ? View.VISIBLE : View.GONE);
                    tvStudentSummaryHeader.setText(String.format(Locale.getDefault(),
                            "Enrolled Students (%d) • Active Fee: %d • Overdue Fee: %d",
                            students.size(), activeCount, overdueCount));
                })
                .addOnFailureListener(e -> {
                    progressBarStudents.setVisibility(View.GONE);
                    Toast.makeText(ClassroomActivity.this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showUploadDialog() {
        selectedFileUri = null;
        selectedFileName = "No file chosen";

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_material, null);
        EditText etTitle = dialogView.findViewById(R.id.etMaterialTitle);
        EditText etDesc = dialogView.findViewById(R.id.etMaterialDesc);
        MaterialButton btnSelectFile = dialogView.findViewById(R.id.btnSelectFile);
        dialogTvSelectedFileName = dialogView.findViewById(R.id.tvSelectedFileName);
        dialogUploadProgressBar = dialogView.findViewById(R.id.uploadProgressBar);

        btnSelectFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Upload", null) // Handled manually to prevent auto-closing
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton uploadBtn = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            uploadBtn.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    etTitle.setError("Title is required");
                    return;
                }
                if (selectedFileUri == null) {
                    Toast.makeText(ClassroomActivity.this, "Please select a file to upload", Toast.LENGTH_SHORT).show();
                    return;
                }

                uploadFileToFirebase(title, desc, dialog);
            });
        });

        dialog.show();
    }

    private void uploadFileToFirebase(String title, String desc, AlertDialog dialog) {
        if (dialogUploadProgressBar != null) {
            dialogUploadProgressBar.setVisibility(View.VISIBLE);
        }

        String storageFileName = System.currentTimeMillis() + "_" + selectedFileName;
        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child("materials")
                .child(currentClassroom.getClassroomId())
                .child(storageFileName);

        fileRef.putFile(selectedFileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String fileUrl = uri.toString();
                    String fileType = determineFileType(selectedFileName);

                    String materialId = FirebaseFirestore.getInstance()
                            .collection("classrooms")
                            .document(currentClassroom.getClassroomId())
                            .collection("materials")
                            .document().getId();

                    String uploaderTag = currentUserProfile.isTeacher() ? " (Teacher)" : " (Student)";
                    String uploaderName = (currentUserProfile.getName() != null ? currentUserProfile.getName() : "User") + uploaderTag;

                    MaterialItem materialItem = new MaterialItem(
                            materialId,
                            currentClassroom.getClassroomId(),
                            title,
                            desc,
                            fileUrl,
                            selectedFileName,
                            fileType,
                            currentUserProfile.getUid(),
                            uploaderName
                    );

                    FirebaseFirestore.getInstance()
                            .collection("classrooms")
                            .document(currentClassroom.getClassroomId())
                            .collection("materials")
                            .document(materialId)
                            .set(materialItem.toMap())
                            .addOnSuccessListener(aVoid -> {
                                if (dialogUploadProgressBar != null) {
                                    dialogUploadProgressBar.setVisibility(View.GONE);
                                }
                                dialog.dismiss();
                                Toast.makeText(ClassroomActivity.this, "Material uploaded successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (dialogUploadProgressBar != null) {
                                    dialogUploadProgressBar.setVisibility(View.GONE);
                                }
                                Toast.makeText(ClassroomActivity.this, "Failed to save metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    if (dialogUploadProgressBar != null) {
                        dialogUploadProgressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(ClassroomActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String determineFileType(String name) {
        if (name == null) return "other";
        String lower = name.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
            return "image";
        } else if (lower.endsWith(".pdf")) {
            return "pdf";
        } else if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
            return "doc";
        }
        return "other";
    }

    private String queryFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "material_file";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (materialsListener != null) {
            materialsListener.remove();
        }
    }
}
