package com.virtualtuition.app.live;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.virtualtuition.app.R;
import com.virtualtuition.app.models.UserProfile;
import com.virtualtuition.app.payment.AccessControlManager;
import com.virtualtuition.app.payment.PaymentActivity;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import java.net.MalformedURLException;
import java.net.URL;

public class VideoClassActivity extends AppCompatActivity {

    private Toolbar videoToolbar;
    private CardView cardVideoLocked, cardVideoActive;
    private TextView tvVideoRoomTitle, tvRoomNameDisplay;
    private SwitchMaterial switchStartMutedAudio, switchStartMutedVideo;
    private MaterialButton btnLaunchJitsiMeeting, btnUnlockVideoPay;

    private String roomId = "Campus_Main_Room";
    private String roomTitle = "Live Tuition Class";
    private UserProfile currentUserProfile;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_class);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        String passedRoomId = getIntent().getStringExtra("roomId");
        if (passedRoomId != null && !passedRoomId.trim().isEmpty()) {
            roomId = passedRoomId.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        }
        String passedTitle = getIntent().getStringExtra("roomTitle");
        if (passedTitle != null && !passedTitle.trim().isEmpty()) {
            roomTitle = passedTitle;
        }

        initViews();
        setupListeners();
        verifyVideoAccess();
    }

    private void initViews() {
        videoToolbar = findViewById(R.id.videoToolbar);
        cardVideoLocked = findViewById(R.id.cardVideoLocked);
        cardVideoActive = findViewById(R.id.cardVideoActive);
        tvVideoRoomTitle = findViewById(R.id.tvVideoRoomTitle);
        tvRoomNameDisplay = findViewById(R.id.tvRoomNameDisplay);
        switchStartMutedAudio = findViewById(R.id.switchStartMutedAudio);
        switchStartMutedVideo = findViewById(R.id.switchStartMutedVideo);
        btnLaunchJitsiMeeting = findViewById(R.id.btnLaunchJitsiMeeting);
        btnUnlockVideoPay = findViewById(R.id.btnUnlockVideoPay);

        videoToolbar.setNavigationOnClickListener(v -> finish());
        tvVideoRoomTitle.setText(roomTitle);
        tvRoomNameDisplay.setText("Dynamic Room: " + roomId);
    }

    private void setupListeners() {
        btnLaunchJitsiMeeting.setOnClickListener(v -> launchJitsiMeetRoom());

        btnUnlockVideoPay.setOnClickListener(v -> {
            Intent intent = new Intent(VideoClassActivity.this, PaymentActivity.class);
            startActivity(intent);
        });
    }

    private void verifyVideoAccess() {
        if (firebaseUser == null) return;
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserProfile = documentSnapshot.toObject(UserProfile.class);
                        if (currentUserProfile != null) {
                            currentUserProfile.setUid(documentSnapshot.getId());
                            boolean allowed = AccessControlManager.isLiveVideoAllowed(currentUserProfile);
                            if (!allowed) {
                                cardVideoLocked.setVisibility(View.VISIBLE);
                                cardVideoActive.setVisibility(View.GONE);
                            } else {
                                cardVideoLocked.setVisibility(View.GONE);
                                cardVideoActive.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
    }

    private void launchJitsiMeetRoom() {
        try {
            URL serverURL = new URL("https://meet.jit.si");

            JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
            if (currentUserProfile != null) {
                userInfo.setDisplayName(currentUserProfile.getName());
                userInfo.setEmail(currentUserProfile.getEmail());
            } else if (firebaseUser != null) {
                userInfo.setDisplayName(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Tuition Student");
                userInfo.setEmail(firebaseUser.getEmail());
            }

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setRoom("VirtualTuition_" + roomId)
                    .setAudioMuted(switchStartMutedAudio.isChecked())
                    .setVideoMuted(switchStartMutedVideo.isChecked())
                    .setUserInfo(userInfo)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .build();

            JitsiMeetActivity.launch(this, options);
        } catch (MalformedURLException e) {
            Toast.makeText(this, "Invalid conference server URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch meeting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
