package com.example.biomsAfterWirelessConnectedVersion;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import java.util.Locale;

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
        NestedScrollView scrollView = findViewById(R.id.nested_scroll_view); // Add this
        TextView previewModeNext = findViewById(R.id.previewModeNext);
        CheckBox walk = findViewById(R.id.walk);
        TextView sensoryAtaxia = findViewById(R.id.SensoryAtaxia);
        TextView dualTask = findViewById(R.id.DualTask);
        TextView swaAndBalance = findViewById(R.id.SwayAreaAndBalance);

        if (!seeInstructionsOnly) {
            swaAndBalance.setVisibility(GONE);
            if (assessmentType.equals("SwayAreaAndBalance")) {
                sensoryAtaxia.setVisibility(GONE);
                dualTask.setVisibility(GONE);
            } else if (assessmentType.equals("SensoryAtaxia")) {
                dualTask.setVisibility(GONE);
                sensoryAtaxia.setText("Stand feet together, hands clamped to the sides of your body, eyes closed, facing the camera.");
            } else if (assessmentType.equals("DualTask")) {
                sensoryAtaxia.setVisibility(GONE);
                dualTask.setText("Stand feet together, hands clamped to the sides of your body, eyes open, saying the ink colors out loud.");
            }
        }


        if (seeInstructionsOnly) {
            next.setVisibility(GONE);
            previewModeNext.setText("Only check off the boxes after you've completed them.");
            Log.d("Entering if", "if entered");
        }

        ImageView propRecord = findViewById(R.id.propRecord);

        if (someoneRecord) {
            propRecording.setVisibility(GONE);
            surfaceBody.setVisibility(GONE);
            propRecord.setVisibility(GONE);
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

            if ("DualTask".equals(assessmentType)
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_CODE_AUDIO_PERM);
                return;
            }

            // Before the camera (and its countdown) starts: confirm the
            // camera distance, then the secure-surface setup.
            showDistancePrompt(assessmentType, someoneRecord, scrollView, fragmentContainer);
        });

        // Re-enable scrolling when returning from fragment
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (scrollView != null) {
                    scrollView.setVisibility(VISIBLE);
                }
            }
        });

        back.setOnClickListener(v -> {
            finish();
        });
    }

    // ------------------------------------------------------------------
    // Pre-test prompts (run after Next, before the camera/countdown).
    // ------------------------------------------------------------------

    private float getRecordedDistance() {
        SharedPreferences p = getSharedPreferences(
                PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE);
        return p.getFloat(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M, 0f);
    }

    /** Prompt 1: is the camera distance correct for this test? Blocks the
     *  flow until a distance exists. Yes -> secure-surface prompt;
     *  Edit -> distance dialog, then back here to confirm the new value. */
    private void showDistancePrompt(String assessmentType, boolean someoneRecord,
                                    NestedScrollView scrollView, FrameLayout fragmentContainer) {
        float d = getRecordedDistance();
        if (d <= 0f) {
            new AlertDialog.Builder(this)
                    .setTitle("Camera Distance Required")
                    .setMessage("No camera distance has been entered yet.\n\n"
                            + "\u26A0 Camera distance directly influences accuracy.\n\n"
                            + "Measure the distance from the camera to the spot where "
                            + "you will stand, then enter it to continue.")
                    .setPositiveButton("Enter Distance", (dlg, w) ->
                            showDistanceEditDialog(() -> showDistancePrompt(
                                    assessmentType, someoneRecord, scrollView, fragmentContainer)))
                    .setNegativeButton("Not Now", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Camera Distance")
                .setMessage(String.format(Locale.US,
                        "Is this camera distance correct for this test?\n\n%.2f meters\n\n"
                                + "\u26A0 Camera distance directly influences accuracy.", d))
                .setPositiveButton("Yes", (dlg, w) ->
                        showSecureSurfacePrompt(assessmentType, someoneRecord, scrollView, fragmentContainer))
                .setNegativeButton("Edit", (dlg, w) ->
                        showDistanceEditDialog(() -> showDistancePrompt(
                                assessmentType, someoneRecord, scrollView, fragmentContainer)))
                .show();
    }

    private void showDistanceEditDialog(Runnable onSaved) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_trial_distance, null);
        EditText distanceInput = dialogView.findViewById(R.id.distanceInput);
        float current = getRecordedDistance();
        if (current > 0) {
            distanceInput.setText(String.format(Locale.US, "%.2f", current));
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Camera Distance")
                .setMessage("Tape-measured distance from the camera to where you will stand, in meters.")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String s = distanceInput.getText().toString().trim();
                    try {
                        double val = Double.parseDouble(s);
                        if (val < 0.5 || val > 10) {
                            distanceInput.setError("Enter a distance between 0.5 and 10 m");
                            return;
                        }
                        getSharedPreferences(PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE)
                                .edit().putFloat(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M,
                                        (float) val).apply();
                        dialog.dismiss();
                        if (onSaved != null) onSaved.run();
                    } catch (NumberFormatException e) {
                        distanceInput.setError("Invalid number format");
                    }
                }));
        dialog.show();
    }

    /** Prompt 2: CRITICAL — the phone must be on a secure surface, not
     *  held. Skipped when someone else records (the recorder holds and
     *  operates the phone by design in that mode). Yes -> countdown. */
    private void showSecureSurfacePrompt(String assessmentType, boolean someoneRecord,
                                         NestedScrollView scrollView, FrameLayout fragmentContainer) {
        if (someoneRecord) {
            launchCameraPreview(assessmentType, someoneRecord, scrollView, fragmentContainer);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("\u26A0 CRITICAL")
                .setMessage("Is the phone on A SECURE SURFACE AND NOT BEING HELD?\n\n"
                        + "If the phone moves, its movement is recorded as body sway "
                        + "and the results will be wrong.")
                .setPositiveButton("Yes", (dlg, w) ->
                        launchCameraPreview(assessmentType, someoneRecord, scrollView, fragmentContainer))
                .setNegativeButton("No", null)
                .show();
    }

    /** The camera launch that used to run directly from Next. */
    private void launchCameraPreview(String assessmentType, boolean someoneRecord,
                                     NestedScrollView scrollView, FrameLayout fragmentContainer) {
        if (scrollView != null) {
            scrollView.setVisibility(GONE);
        }

        Bundle bundle = new Bundle();
        bundle.putString("assessmentType", assessmentType);
        bundle.putBoolean("someoneRecord", someoneRecord);
        bundle.putBoolean("preview", preview);

        fragmentContainer.setVisibility(VISIBLE);

        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .addToBackStack("camera")
                .commit();
    }

    private static final int REQUEST_CODE_AUDIO_PERM = 300;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_AUDIO_PERM) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Microphone Required")
                        .setMessage("The Dual Task test needs microphone access to detect your spoken answers. "
                                + "Without it, the test cannot run.")
                        .setPositiveButton("Try Again", (d, w) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    REQUEST_CODE_AUDIO_PERM);
                        })
                        .setNegativeButton("Cancel", (d, w) -> { /* stay on this screen */ })
                        .setCancelable(false)
                        .show();
            }
        }
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
