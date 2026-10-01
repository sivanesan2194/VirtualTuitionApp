package com.virtualtuition.app.whiteboard;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class WhiteboardActivity extends AppCompatActivity {

    private DrawView drawView;
    private Toolbar whiteboardToolbar;
    private ImageButton btnUndoStroke, btnToggleEraser, btnClearCanvas, btnExportCanvas;
    private SeekBar seekBarThickness;
    private TextView tvBrushSize;

    private UserProfile currentUserProfile;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        setContentView(R.layout.activity_whiteboard);

        initViews();
        setupPaletteAndTools();
        verifyWhiteboardAccess();
    }

    private void verifyWhiteboardAccess() {
        if (firebaseUser == null) return;
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserProfile = documentSnapshot.toObject(UserProfile.class);
                        if (currentUserProfile != null) {
                            currentUserProfile.setUid(documentSnapshot.getId());
                            if (!AccessControlManager.isWhiteboardAllowed(currentUserProfile)) {
                                AccessControlManager.showLockedFeatureDialog(
                                        WhiteboardActivity.this,
                                        "Drawpoint Whiteboard",
                                        "Access to the whiteboard is locked due to overdue tuition fees (Phase 2 lock). Please pay your tuition fee to resume whiteboard access."
                                );
                                finish();
                            }
                        }
                    }
                });
    }

    private void initViews() {
        drawView = findViewById(R.id.drawView);
        whiteboardToolbar = findViewById(R.id.whiteboardToolbar);
        btnUndoStroke = findViewById(R.id.btnUndoStroke);
        btnToggleEraser = findViewById(R.id.btnToggleEraser);
        btnClearCanvas = findViewById(R.id.btnClearCanvas);
        btnExportCanvas = findViewById(R.id.btnExportCanvas);
        seekBarThickness = findViewById(R.id.seekBarThickness);
        tvBrushSize = findViewById(R.id.tvBrushSize);

        whiteboardToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPaletteAndTools() {
        // Undo
        btnUndoStroke.setOnClickListener(v -> drawView.undo());

        // Eraser Toggle
        btnToggleEraser.setOnClickListener(v -> {
            boolean isEraser = !drawView.isEraserMode();
            drawView.setEraserMode(isEraser);
            btnToggleEraser.setColorFilter(isEraser ?
                    ContextCompat.getColor(this, R.color.accent) :
                    ContextCompat.getColor(this, R.color.text_primary));
            Toast.makeText(this, isEraser ? "Eraser Mode" : "Brush Mode", Toast.LENGTH_SHORT).show();
        });

        // Clear Canvas
        btnClearCanvas.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear Canvas")
                    .setMessage("Are you sure you want to clear the entire drawing?")
                    .setPositiveButton("Clear", (dialog, which) -> drawView.clearCanvas())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Export Canvas
        btnExportCanvas.setOnClickListener(v -> showExportOptionsDialog());

        // Thickness Slider
        seekBarThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float thickness = Math.max(3f, (float) progress);
                drawView.setBrushThickness(thickness);
                tvBrushSize.setText("Size: " + (int) thickness + "px");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Palette Colors
        setupColorClick(R.id.colorBlack, R.color.palette_black);
        setupColorClick(R.id.colorRed, R.color.palette_red);
        setupColorClick(R.id.colorBlue, R.color.palette_blue);
        setupColorClick(R.id.colorGreen, R.color.palette_green);
        setupColorClick(R.id.colorYellow, R.color.palette_yellow);
        setupColorClick(R.id.colorOrange, R.color.palette_orange);
        setupColorClick(R.id.colorPurple, R.color.palette_purple);
    }

    private void setupColorClick(int viewId, int colorRes) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                int color = ContextCompat.getColor(WhiteboardActivity.this, colorRes);
                drawView.setStrokeColor(color);
                drawView.setEraserMode(false);
                btnToggleEraser.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
            });
        }
    }

    private void showExportOptionsDialog() {
        String[] options = {"Save to Device Gallery", "Share Drawing", "Upload to Cloud (Firebase)"};
        new AlertDialog.Builder(this)
                .setTitle("Export Whiteboard")
                .setItems(options, (dialog, which) -> {
                    Bitmap bitmap = drawView.exportBitmap();
                    if (which == 0) {
                        saveBitmapToGallery(bitmap);
                    } else if (which == 1) {
                        shareBitmap(bitmap);
                    } else if (which == 2) {
                        uploadBitmapToFirebase(bitmap);
                    }
                })
                .show();
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        String filename = "whiteboard_" + System.currentTimeMillis() + ".png";
        OutputStream fos = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VirtualTuition");

                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    fos = getContentResolver().openOutputStream(imageUri);
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VirtualTuition");
                if (!dir.exists()) dir.mkdirs();
                File imageFile = new File(dir, filename);
                fos = new FileOutputStream(imageFile);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Whiteboard saved to Pictures/VirtualTuition", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save drawing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBitmap(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File stream = new File(cachePath, "whiteboard_export.png");
            FileOutputStream fos = new FileOutputStream(stream);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", stream);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Whiteboard"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadBitmapToFirebase(Bitmap bitmap) {
        if (firebaseUser == null) return;
        Toast.makeText(this, "Uploading drawing to cloud...", Toast.LENGTH_SHORT).show();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        String fileName = System.currentTimeMillis() + ".png";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("whiteboards")
                .child(firebaseUser.getUid())
                .child(fileName);

        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(WhiteboardActivity.this, "Drawing uploaded to cloud storage!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WhiteboardActivity.this, "Cloud upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
