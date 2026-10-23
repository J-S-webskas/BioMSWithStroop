package com.example.biomsAfterWirelessConnectedVersion;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import java.io.File;

public class CancellingTest extends AppCompatActivity {

    private TextView cancellingTest;
    private TextView dataNotSaved;
    private LinearLayout exportActions;
    private File pendingExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancelling_test);

        cancellingTest = findViewById(R.id.cancellingTest);
        dataNotSaved = findViewById(R.id.dataNotSaved);
        exportActions = findViewById(R.id.exportActions);
        Button btnExportCsv = findViewById(R.id.btnExportCsv);
        Button btnContinueTest = findViewById(R.id.btnContinueTest);
        btnExportCsv.setOnClickListener(v -> shareExport());
        btnContinueTest.setOnClickListener(v -> navigateToMain(getIntent()));

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
            // there is nothing to export and no save to undo: show the
            // message and navigate away after 1.5 seconds.
            cancellingTest.setText("Test Cancelled.");
            dataNotSaved.setText("Results not saved.");
            exportActions.setVisibility(View.GONE);
            postDelayedNavigate(intent, saving);
            return;
        }
        if (intent.getBooleanExtra("testSaved", false)) {
            // A trial was just saved: no stopover on this screen.
            handleTestSaved(intent);
            return;
        }
        // A finished test with a CSV export package waits for the user:
        // they can tap "Export CSV" (or Continue) instead of being
        // auto-advanced past the button.
        boolean exportMode = finished && showExportIfAvailable();
        if (saving) {
            dataNotSaved.setText("If this is a mistake, you can always delete the test");
            cancellingTest.setText("Saving Test");
        }
        if (simplyNavigatingToMainPage) {
            dataNotSaved.setText("Will ye not navigate out of the rainbow load page mid-way? :)");
            cancellingTest.setText("Simply navigating to main page");
        }
        if ((finished || simplyNavigatingToMainPage) && !exportMode) {
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
     *  after the save has completed). There is no wait on this screen any
     *  more — no export-mode stopover waiting for a tap, no 1.5 s
     *  auto-advance: show the saved message and return to the main page
     *  immediately. The toast carries the same message across the
     *  transition so it stays readable once this screen is gone.
     *  Exporting a saved trial stays available at any time from the
     *  Report tab's Export CSV. */
    private void handleTestSaved(Intent intent) {
        cancellingTest.setText("Test saved.");
        dataNotSaved.setText("You can always delete this test if is inaccurate");
        exportActions.setVisibility(View.GONE);
        Toast.makeText(this,
                "Test saved. You can always delete this test if is inaccurate",
                Toast.LENGTH_LONG).show();
        navigateToMain(intent);
    }

    /** If the last finished test produced a CSV export package, switch this
     *  screen to export mode (Export CSV / Continue buttons, no auto-advance)
     *  and return true. */
    private boolean showExportIfAvailable() {
        pendingExport = TrialCsvExporter.getPendingExport(this);
        if (pendingExport == null) return false;
        cancellingTest.setText("Test Complete");
        dataNotSaved.setText("Your results are saved.\nExport the CSV data, or continue.");
        exportActions.setVisibility(View.VISIBLE);
        return true;
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

    /** Shares the export zip (e.g. via Gmail) through the system share sheet. */
    private void shareExport() {
        if (pendingExport == null) {
            Toast.makeText(this, "No export available", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", pendingExport);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/zip");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, pendingExport.getName());
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Export CSV data"));
    }

    /** The same return-to-main navigation the auto-advance handler performs. */
    private void navigateToMain(Intent source) {
        boolean saving = source != null && source.getBooleanExtra("Saving", false);
        Intent resultIntent = new Intent();
        resultIntent.putExtra("cancelled", true);
        resultIntent.putExtra("Saving", saving);
        setResult(RESULT_OK, resultIntent);
        Intent intent1 = new Intent(this, MainActivity.class);
        intent1.putExtra("navigate_to_assessment", true);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent1);
        finish();
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
            exportActions.setVisibility(View.GONE);
            postDelayedNavigate(intent, saving);
            return;
        }
        if (intent.getBooleanExtra("testSaved", false)) {
            // A trial was just saved: no stopover on this screen.
            handleTestSaved(intent);
            return;
        }
        boolean exportMode = finished && showExportIfAvailable();
        if (saving) {
            dataNotSaved.setText("If this is a mistake, you can always delete the test");
            cancellingTest.setText("Saving Test");
        }
        if (finished && !exportMode) {
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
