package com.example.biomsAfterWirelessConnectedVersion;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

public class CancellingTest extends AppCompatActivity {

    private TextView cancellingTest;
    private TextView dataNotSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelling_test);

        cancellingTest = findViewById(R.id.cancellingTest);
        dataNotSaved = findViewById(R.id.dataNotSaved);

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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Set the activity to not be removable from recent apps
            // Note: This may not work on all devices/Android versions
            setTaskDescription(new android.app.ActivityManager.TaskDescription(
                    getString(R.string.app_name),
                    null,
                    getResources().getColor(android.R.color.holo_blue_dark)
            ));
        }

        Intent intent = getIntent();
        boolean saving = intent.getBooleanExtra("Saving", false);
        boolean finished = intent.getBooleanExtra("finished", false);
        boolean simplyNavigatingToMainPage = intent.getBooleanExtra("simplyNavigatingToMainPage", false);
        boolean cancelledMidway = intent.getBooleanExtra("cancelledMidway", false);
        if (cancelledMidway) {
            // User cancelled mid-test. The discard path never saves, so
            // there is no save to undo: show the message and navigate
            // away after 1.5 seconds.
            cancellingTest.setText("Test Cancelled.");
            dataNotSaved.setText("Results not saved.");
            postDelayedNavigate(intent, saving);
            return;
        }
        if (intent.getBooleanExtra("testSaved", false)) {
            // A trial was just saved: show the saved message for 1.5 s, then go.
            handleTestSaved(intent);
            return;
        }
        if (intent.getBooleanExtra("noPoseDetected", false)) {
            // The test ran but the user never appeared on camera:
            // nothing was saved, so present it like a cancellation.
            handleNoPoseDetected(intent, saving);
            return;
        }
        if (saving) {
            dataNotSaved.setText("If this is a mistake, you can always delete the test");
            cancellingTest.setText("Saving Test");
        }
        if (simplyNavigatingToMainPage) {
            dataNotSaved.setText("Will ye not navigate out of the rainbow load page mid-way? :)");
            cancellingTest.setText("Simply navigating to main page");
        }
        if (finished || simplyNavigatingToMainPage) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Set result to indicate cancellation
            Intent resultIntent = new Intent();
            resultIntent.putExtra("cancelled", true);
            resultIntent.putExtra("Saving", saving);
            setResult(RESULT_OK, resultIntent);
            boolean cancelled = intent.getBooleanExtra("cancelled", false);
            if (cancelled || finished || saving) {
                Intent intent1 = new Intent(this, MainActivity.class);
                intent1.putExtra("navigate_to_assessment", true);
                // IMPORTANT! Must reuse the old main activity in order for bottom nav to work
                intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent1);
                // Must stop previous activities!!!! IMPORTANT
                finish();
            }
            }, 1500); // Show the UI for 1.5 seconds
        }
    }

    /** A trial was just saved (the caller passes testSaved=true, and only
     *  after the save has completed). Like every other finish path, this
     *  screen simply shows the saved message for 1.5 s and then goes away:
     *  no buttons, no waiting for a tap. Exporting a saved trial stays
     *  available at any time from the Report tab's Export CSV. */
    private void handleTestSaved(Intent intent) {
        cancellingTest.setText("Test saved.");
        dataNotSaved.setText("You can always delete this test if it is inaccurate");
        boolean saving = intent.getBooleanExtra("Saving", false);
        postDelayedNavigate(intent, saving);
    }

    /** The test ran to completion but no pose was ever detected (the
     *  user let it run without stepping into the camera's view), so
     *  nothing was saved. Present it exactly like a cancellation: a
     *  "not saved" message and the same 1.5 s auto-exit. */
    private void handleNoPoseDetected(Intent intent, boolean saving) {
        cancellingTest.setText("Test Not Saved.");
        dataNotSaved.setText("No pose detected. Results not saved.");
        postDelayedNavigate(intent, saving);
    }

    /** Auto-navigate back to MainActivity after 1.5 s — used by the
     *  cancelled-midway path (nothing saved, nothing to export). */
    private void postDelayedNavigate(Intent source, boolean saving) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("cancelled", true);
            resultIntent.putExtra("Saving", saving);
            setResult(RESULT_OK, resultIntent);
            Intent intent1 = new Intent(this, MainActivity.class);
            intent1.putExtra("navigate_to_assessment", true);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent1);
            finish();
        }, 1500);
    }

    @SuppressLint("MissingSuperCall") // Specifically suppress "call super" warning
    @Override
    public void onBackPressed() {
        // Do NOT call super.onBackPressed() - intentionally disabled
        Toast.makeText(this, "Please wait, test is processing", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AssessmentFragment assessmentActivity = new AssessmentFragment();

        boolean saving = intent.getBooleanExtra("Saving", false);
        boolean finished = intent.getBooleanExtra("finished", false);
        boolean cancelledMidway = intent.getBooleanExtra("cancelledMidway", false);
        if (cancelledMidway) {
            cancellingTest.setText("Test Cancelled.");
            dataNotSaved.setText("Results not saved.");
            postDelayedNavigate(intent, saving);
            return;
        }
        if (intent.getBooleanExtra("testSaved", false)) {
            // A trial was just saved: show the saved message for 1.5 s, then go.
            handleTestSaved(intent);
            return;
        }
        if (intent.getBooleanExtra("noPoseDetected", false)) {
            // The test ran but the user never appeared on camera:
            // nothing was saved, so present it like a cancellation.
            handleNoPoseDetected(intent, saving);
            return;
        }
        if (saving) {
            dataNotSaved.setText("If this is a mistake, you can always delete the test");
            cancellingTest.setText("Saving Test");
        }
        if (finished) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Set result to indicate cancellation
                Intent resultIntent = new Intent();
                resultIntent.putExtra("cancelled", true);
                resultIntent.putExtra("Saving", saving);
                setResult(RESULT_OK, resultIntent);
                boolean cancelled = intent.getBooleanExtra("cancelled", false);
                if (cancelled || finished || saving) {
                    Intent intent1 = new Intent(this, MainActivity.class);
                    intent1.putExtra("navigate_to_assessment", true);
                    // IMPORTANT! Must reuse the old main activity in order for bottom nav to work
                    intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent1);
                    // Must stop previous activities!!!! IMPORTANT
                    finish();
                }
            }, 1500);
        }
    }
}
