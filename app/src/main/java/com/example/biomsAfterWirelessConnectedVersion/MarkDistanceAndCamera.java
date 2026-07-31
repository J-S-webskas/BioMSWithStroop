package com.example.biomsAfterWirelessConnectedVersion;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import java.net.URL;

public class MarkDistanceAndCamera extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static boolean preview = true;

    // In on Create view, if users click the "I will prop the camera up", automatically set the camera to front facing (for preview & test)
    // Show the 2nd title, body and images
    // Otherwise, set it to back camera
    // Pass a notice into the markDistance activity to not show the 2nd title, body, and images

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions_mark_distance);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

            // Make status bar icons dark (for better visibility on white background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        Intent intent = getIntent();
        String assessmentType = intent.getStringExtra("assessmentType");
        boolean someoneRecord = intent.getBooleanExtra("someoneRecord", false);
        boolean seeInstructionsOnly = intent.getBooleanExtra("seeInstructionsOnly", false);
        Log.d("MarkDistanceAndCamera recieving", "seeInstructions seeInstructionsOnly recieved as " + seeInstructionsOnly);
        setupTestControls(assessmentType, someoneRecord, seeInstructionsOnly);

        // set the visibility of the dualTaskRecordARJ & sensoryAtaxiaRecordARJ
        // Grey out Dual Task textview if necessary
    }

    private void setupTestControls(String assessmentType, boolean someoneRecord, boolean seeInstructionsOnly) {
        Button next = findViewById(R.id.next);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        ImageButton back = findViewById(R.id.back);
        CheckBox propRecording = findViewById(R.id.propRecording);
        TextView surfaceBody = findViewById(R.id.surfaceBody);
        VideoView someoneRecording = findViewById(R.id.someoneRecord);
        NestedScrollView scrollView = findViewById(R.id.nested_scroll_view); // Add this
        TextView previewModeNext = findViewById(R.id.previewModeNext);
        CheckBox walk = findViewById(R.id.walk);
        TextView sensoryAtaxia = findViewById(R.id.SensoryAtaxia);
        TextView dualTask = findViewById(R.id.DualTask);
        TextView arjAndBalance = findViewById(R.id.ARJAndBalance);

        if (!seeInstructionsOnly) {
            arjAndBalance.setVisibility(GONE);
            if (assessmentType.equals("ARJAndBalance")) {
                sensoryAtaxia.setVisibility(GONE);
                dualTask.setVisibility(GONE);
            } else if (assessmentType.equals("SensoryAtaxia")) {
                dualTask.setVisibility(GONE);
                sensoryAtaxia.setText("Walk straight towards camera with eyes closed, and, if possible, without assistance.");
            } else if (assessmentType.equals("DualTask")) {
                sensoryAtaxia.setVisibility(GONE);
                dualTask.setText("Walk straight towards camera while saying the alphabet backwards, eyes open.");
            }
        }


        if (seeInstructionsOnly) {
            next.setVisibility(GONE);
            previewModeNext.setText("Only check off the boxes after you've completed them.");
            Log.d("Entering if", "if entered");
        }

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.someone_false;
        Uri uri = Uri.parse(videoPath);
        VideoView propRecord = findViewById(R.id.propRecord);
        propRecord.setVideoURI(uri);
        propRecord.setOnPreparedListener(mp -> {
            propRecord.start();
            mp.setLooping(true);
        });

        String videoPathSomeone = "android.resource://" + getPackageName() + "/" + R.raw.someone_record;
        Uri uriSomeone = Uri.parse(videoPathSomeone);
        someoneRecording.setVideoURI(uriSomeone);
        someoneRecording.setOnPreparedListener(mp -> {
            // Video is ready to play - start it
            someoneRecording.start();
            // Optional: Loop the video if it's a demonstration
             mp.setLooping(true);
        });

        setupVideoView(someoneRecording, uriSomeone);
        setupVideoView(propRecord, uri);

        if (someoneRecord) {
            propRecording.setVisibility(GONE);
            surfaceBody.setVisibility(GONE);
            propRecord.setVisibility(GONE);
        } else {
            propRecord.start();
        }

        next.setOnClickListener(v -> {

            // Check if permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Directly request permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
                return;
            }

            if (scrollView != null) {
                scrollView.setVisibility(GONE);
            }

            Bundle bundle = new Bundle();
            bundle.putString("assessmentType", assessmentType);
            bundle.putBoolean("someoneRecord", someoneRecord);
            bundle.putBoolean("preview", preview);

            pauseAllVideos(someoneRecording, propRecord);

            ////block touch                blockTouchEvents(false);  // In onResume or after camera stops
            /// Need to show a prompt asking for camera permission
            fragmentContainer.setVisibility(VISIBLE);

            CameraFragment fragment = new CameraFragment();
            fragment.setArguments(bundle);

            this.getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .addToBackStack("camera")
                    .commit();

            // Set the button's text to Start Test for the preview
            // Set the button's text to Cancel Test
        });

        // Re-enable scrolling when returning from fragment
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (scrollView != null) {
                    scrollView.setVisibility(VISIBLE);
                }
                // Resume videos if needed
                resumeAllVideos(someoneRecording, propRecord);
            }
        });

        back.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupVideoView(VideoView videoView, Uri uri) {
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> {
            videoView.start();
            mp.setLooping(true);
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            // Log error and handle gracefully
            return true; // Return true to indicate error was handled
        });
    }

    private void pauseAllVideos(VideoView... videos) {
        for (VideoView v : videos) {
            if (v != null && v.isPlaying()) {
                v.pause();
            }
        }
    }

    private void resumeAllVideos(VideoView... videos) {
        for (VideoView v : videos) {
            if (v != null && !v.isPlaying()) {
                v.start();
            }
        }
    }
}
