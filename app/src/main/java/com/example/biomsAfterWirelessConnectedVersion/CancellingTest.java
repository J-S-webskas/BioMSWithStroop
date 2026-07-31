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
