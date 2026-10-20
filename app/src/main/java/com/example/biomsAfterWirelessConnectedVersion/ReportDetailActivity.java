package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;

public class ReportDetailActivity extends AppCompatActivity {

    TextView date, ARJTodayRight, ARJTodayLeft, ARJTrendLeft, ARJTrendRight,
            balanceTrend,
            speedTrend,
            enduranceTrend,
            sensoryAtaxiaARJLeftToday, sensoryAtaxiaARJRightToday, sensoryAtaxiaTrend,
            dualTaskARJLeftToday, dualTaskARJRightToday, dualTaskTrend,
            dtcARJLeftSA, dtcARJRightSA, dtcARJLeftDT, dtcARJRightDT, dtcBalanceDT, dtcBalanceSA,
            dtcSwayVelocityDT, dtcSwayPathDT, dtcSwayVelocitySA, dtcSwayPathSA,
            ARJRightSD, ARJRightCV, ARJLeftSD, ARJLeftCV, BalanceSD, BalanceCV, WSSD, WSCV, EnduranceSD, EnduranceCV,
            balanceTrialsTextView, wsTrialsTextView, enduranceTrialsTextView, dualTaskBalanceTrialsTextView, sensoryAtaxiaBalanceTrialsTextView,
            swayPathLengthTrialsTextView, swayVelocityTrialsTextView, swayRmsAPTrialsTextView, swayRmsMLTrialsTextView,
            dualTaskSwayPathLengthTrialsTextView, dualTaskSwayVelocityTrialsTextView, dualTaskSwayRmsAPTrialsTextView, dualTaskSwayRmsMLTrialsTextView,
            sensoryAtaxiaSwayPathLengthTrialsTextView, sensoryAtaxiaSwayVelocityTrialsTextView, sensoryAtaxiaSwayRmsAPTrialsTextView, sensoryAtaxiaSwayRmsMLTrialsTextView;

    private Button shareReportButton;
    private String reportDate;
    private float speedValue, speedTrendValue;
    private float arjRightValue, arjTrendRightValue;
    private float arjLeftValue, arjTrendLeftValue;
    private float balanceValue, balanceTrendValue;
    private float swayVelocityValue, swayPathLengthValue, swayRmsAPValue, swayRmsMLValue;
    private float sensoryAtaxiaSwayVelocityValue, sensoryAtaxiaSwayPathLengthValue, sensoryAtaxiaSwayRmsAPValue, sensoryAtaxiaSwayRmsMLValue;
    private float dualTaskSwayVelocityValue, dualTaskSwayPathLengthValue, dualTaskSwayRmsAPValue, dualTaskSwayRmsMLValue;
    private float DTCSwayVelocityDT, DTCSwayPathDT, DTCSwayVelocitySA, DTCSwayPathSA;
    private float enduranceValue, enduranceTrendValue;
    private float sensoryAtaxiaARJLeftValue, sensoryAtaxiaARJRightValue, sensoryAtaxiaBalanceValue, sensoryAtaxiaTrendValue;
    private float dualTaskARJLeftValue, dualTaskARJRightValue, dualTaskBalanceValue, dualTaskTrendValue;
    private float DTCARJLeftSA, DTCARJRightSA, DTCARJLeftDT, DTCARJRightDT, DTCBalanceDT, DTCBalanceSA;
    private float ARJSDRightValue, ARJCVRightValue, ARJSDLeftValue, ARJCVLeftValue, BalanceSDValue, BalanceCVValue, WSSDValue, WSCVValue, EnduranceSDValue, EnduranceCVValue, SASDValue, SACVValue, DTSDValue, DTCVValue;
    private ReportModel reportModel;
    private File reportFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Initialize all views
        initializeViews();
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

        // Get the report data from Intent
        getReportData();

        // Display the report data
        displayReportDetails();

        shareReportButton.setOnClickListener(v -> {
            createAndSharePdf();
        });
    }

    private void getReportData() {
        Intent intent = getIntent();  // Gets the intent that started this activity (the card views in report fragment)

        if (intent.hasExtra("reportModel")) {
            reportModel = (ReportModel) intent.getSerializableExtra("reportModel");
        }

        // Extract data from intent/report fragment and store them in class variables
        reportDate = intent.getStringExtra("date");
        speedValue = intent.getFloatExtra("speed", 0.0f);
        balanceValue = intent.getFloatExtra("balance", 0.0f);
        swayVelocityValue = intent.getFloatExtra("swayVelocity", 0.0f);
        swayPathLengthValue = intent.getFloatExtra("swayPathLength", 0.0f);
        swayRmsAPValue = intent.getFloatExtra("swayRmsAP", 0.0f);
        swayRmsMLValue = intent.getFloatExtra("swayRmsML", 0.0f);
        enduranceValue = intent.getFloatExtra("endurance", 0.0f);
        sensoryAtaxiaBalanceValue = intent.getFloatExtra("sensoryAtaxiaBalance", 0.0f);
        sensoryAtaxiaSwayVelocityValue = intent.getFloatExtra("sensoryAtaxiaSwayVelocity", 0.0f);
        sensoryAtaxiaSwayPathLengthValue = intent.getFloatExtra("sensoryAtaxiaSwayPathLength", 0.0f);
        sensoryAtaxiaSwayRmsAPValue = intent.getFloatExtra("sensoryAtaxiaSwayRmsAP", 0.0f);
        sensoryAtaxiaSwayRmsMLValue = intent.getFloatExtra("sensoryAtaxiaSwayRmsML", 0.0f);
        dualTaskBalanceValue = intent.getFloatExtra("dualTaskBalance", 0.0f);
        dualTaskSwayVelocityValue = intent.getFloatExtra("dualTaskSwayVelocity", 0.0f);
        dualTaskSwayPathLengthValue = intent.getFloatExtra("dualTaskSwayPathLength", 0.0f);
        dualTaskSwayRmsAPValue = intent.getFloatExtra("dualTaskSwayRmsAP", 0.0f);
        dualTaskSwayRmsMLValue = intent.getFloatExtra("dualTaskSwayRmsML", 0.0f);
        // SD & CV
        BalanceSDValue = intent.getFloatExtra("BalanceSD", 0.0f);
        BalanceCVValue = intent.getFloatExtra("BalanceCV", 0.0f);
        EnduranceSDValue = intent.getFloatExtra("EnduranceSD", 0.0f);
        EnduranceCVValue = intent.getFloatExtra("EnduranceCV", 0.0f);
        WSSDValue = intent.getFloatExtra("WalkingSD", 0.0f);
        WSCVValue = intent.getFloatExtra("WalkingCV", 0.0f);
        // DTC
        DTCBalanceSA = intent.getFloatExtra("sensoryAtaxiaBalanceDTC", 0.0f);
        DTCSwayVelocitySA = intent.getFloatExtra("sensoryAtaxiaSwayVelocityDTC", 0.0f);
        DTCSwayPathSA = intent.getFloatExtra("sensoryAtaxiaSwayPathLengthDTC", 0.0f);
        DTCBalanceDT = intent.getFloatExtra("dualTaskBalanceDTC", 0.0f);
        DTCSwayVelocityDT = intent.getFloatExtra("dualTaskSwayVelocityDTC", 0.0f);
        DTCSwayPathDT = intent.getFloatExtra("dualTaskSwayPathLengthDTC", 0.0f);
    }

    private void displayReportDetails() {
        date.setText(reportDate);

        // SD & CV section
//        ARJRightSD.setText(String.format("%f", ARJSDRightValue));
//        ARJRightCV.setText(String.format("%f %%", ARJCVRightValue));
//        ARJLeftSD.setText(String.format("%f", ARJSDLeftValue));
//        ARJLeftCV.setText(String.format("%f %%", ARJCVLeftValue));
        BalanceSD.setText(String.format("%f ", BalanceSDValue));
        BalanceCV.setText(String.format("%f %%", BalanceCVValue));
        WSSD.setText(String.format("%f", WSSDValue));
        WSCV.setText(String.format("%f %%", WSCVValue));
        EnduranceSD.setText(String.format("%f", EnduranceSDValue));
        EnduranceCV.setText(String.format("%f %%", EnduranceCVValue));

        // DTC section
//        dtcARJLeftDT.setText(String.format("%f %%", DTCARJLeftDT));
//        dtcARJRightDT.setText(String.format("%f %%", DTCARJRightDT));
        dtcBalanceDT.setText(String.format("%f %%", DTCBalanceDT));
        dtcSwayVelocityDT.setText(String.format("%f %%", DTCSwayVelocityDT));
        dtcSwayPathDT.setText(String.format("%f %%", DTCSwayPathDT));
//        dtcARJLeftSA.setText(String.format("%f %%", DTCARJLeftSA));
//        dtcARJRightSA.setText(String.format("%f %%", DTCARJRightSA));
        dtcBalanceSA.setText(String.format("%f %%", DTCBalanceSA));
        dtcSwayVelocitySA.setText(String.format("%f %%", DTCSwayVelocitySA));
        dtcSwayPathSA.setText(String.format("%f %%", DTCSwayPathSA));

        // Speed section
//        speedTrend.setText(String.format("%f", speedTrendValue));

        // ARJ section
//        ARJTodayRight.setText(String.format("%f m/s³", arjRightValue));
//        ARJTodayLeft.setText(String.format("%f m/s³", arjLeftValue));
//        ARJTrendRight.setText(String.format("%.2f", arjTrendRightValue));
//        ARJTrendLeft.setText(String.format("%.2f", arjTrendLeftValue));

        // Balance section
//        balanceTrend.setText(String.format("%f", balanceTrendValue));

        // Trials display
        if (reportModel != null) {
            if (balanceTrialsTextView != null) {
                balanceTrialsTextView.setText(formatTrials(reportModel.getBalanceTrials(), "mm²"));
            }
            if (wsTrialsTextView != null) {
                wsTrialsTextView.setText(formatTrials(reportModel.getSpeedTrials(), "m/s"));
            }
            if (enduranceTrialsTextView != null) {
                enduranceTrialsTextView.setText(formatTrials(reportModel.getEnduranceTrials(), "s"));
            }
            if (dualTaskBalanceTrialsTextView != null) {
                dualTaskBalanceTrialsTextView.setText(formatTrials(reportModel.getDualTaskBalanceTrials(), "mm²"));
            }
            if (sensoryAtaxiaBalanceTrialsTextView != null) {
                sensoryAtaxiaBalanceTrialsTextView.setText(formatTrials(reportModel.getSensoryAtaxiaBalanceTrials(), "mm²"));
            }
            // Sway metric family trials
            if (swayPathLengthTrialsTextView != null) {
                swayPathLengthTrialsTextView.setText(formatTrials(reportModel.getSwayPathLengthTrials(), "mm"));
            }
            if (swayVelocityTrialsTextView != null) {
                swayVelocityTrialsTextView.setText(formatTrials(reportModel.getSwayVelocityTrials(), "mm/s"));
            }
            if (swayRmsAPTrialsTextView != null) {
                swayRmsAPTrialsTextView.setText(formatTrials(reportModel.getSwayRmsAPTrials(), "mm"));
            }
            if (swayRmsMLTrialsTextView != null) {
                swayRmsMLTrialsTextView.setText(formatTrials(reportModel.getSwayRmsMLTrials(), "mm"));
            }
            if (dualTaskSwayPathLengthTrialsTextView != null) {
                dualTaskSwayPathLengthTrialsTextView.setText(formatTrials(reportModel.getDualTaskSwayPathLengthTrials(), "mm"));
            }
            if (dualTaskSwayVelocityTrialsTextView != null) {
                dualTaskSwayVelocityTrialsTextView.setText(formatTrials(reportModel.getDualTaskSwayVelocityTrials(), "mm/s"));
            }
            if (dualTaskSwayRmsAPTrialsTextView != null) {
                dualTaskSwayRmsAPTrialsTextView.setText(formatTrials(reportModel.getDualTaskSwayRmsAPTrials(), "mm"));
            }
            if (dualTaskSwayRmsMLTrialsTextView != null) {
                dualTaskSwayRmsMLTrialsTextView.setText(formatTrials(reportModel.getDualTaskSwayRmsMLTrials(), "mm"));
            }
            if (sensoryAtaxiaSwayPathLengthTrialsTextView != null) {
                sensoryAtaxiaSwayPathLengthTrialsTextView.setText(formatTrials(reportModel.getSensoryAtaxiaSwayPathLengthTrials(), "mm"));
            }
            if (sensoryAtaxiaSwayVelocityTrialsTextView != null) {
                sensoryAtaxiaSwayVelocityTrialsTextView.setText(formatTrials(reportModel.getSensoryAtaxiaSwayVelocityTrials(), "mm/s"));
            }
            if (sensoryAtaxiaSwayRmsAPTrialsTextView != null) {
                sensoryAtaxiaSwayRmsAPTrialsTextView.setText(formatTrials(reportModel.getSensoryAtaxiaSwayRmsAPTrials(), "mm"));
            }
            if (sensoryAtaxiaSwayRmsMLTrialsTextView != null) {
                sensoryAtaxiaSwayRmsMLTrialsTextView.setText(formatTrials(reportModel.getSensoryAtaxiaSwayRmsMLTrials(), "mm"));
            }
        }
    }

    private String formatTrials(java.util.List<Float> trials, String unit) {
        if (trials == null || trials.isEmpty()) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        double sum = 0;
        for (int i = 0; i < trials.size(); i++) {
            // Trials flow horizontally and wrap as whole units (non-breaking
            // spaces inside a trial) so the cell expands right before it
            // ever grows taller.
            if (i > 0) {
                sb.append("   ");
            }
            sb.append("T").append(i + 1).append(":\u00A0")
                    .append(String.format(Locale.US, "%.2f", trials.get(i)))
                    .append("\u00A0").append(unit);
            sum += trials.get(i);
        }
        if (trials.size() >= 2) {
            sb.append("\n").append("Avg: ")
                    .append(String.format(Locale.US, "%.2f", sum / trials.size()))
                    .append(" ").append(unit);
        }
        return sb.toString();
    }
    private void createAndSharePdf() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout);

        if (rootLayout == null) {
            // If no ID, get the content view
            rootLayout = (LinearLayout) findViewById(android.R.id.content);
        }

        // Measure and layout the view
        rootLayout.measure(
                View.MeasureSpec.makeMeasureSpec(rootLayout.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        rootLayout.layout(0, 0, rootLayout.getMeasuredWidth(), rootLayout.getMeasuredHeight());

        // Create PDF document
        PdfDocument document = new PrintedPdfDocument(this,
                new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build());

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                rootLayout.getMeasuredWidth(),
                rootLayout.getMeasuredHeight(),
                1
        ).create();

        PdfDocument.Page page = document.startPage(pageInfo);

        // Draw the layout onto the PDF
        Canvas canvas = page.getCanvas();
        rootLayout.draw(canvas);

        document.finishPage(page);

        // Save PDF to file
        try {
            String fileName = "report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".pdf";
            File pdfFile = new File(getExternalFilesDir(null), fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Share via Gmail
            sharePdfViaEmail(pdfFile);

        } catch (IOException e) {
            Log.e("ReportDetailActivity", "Error creating PDF: " + e.getMessage());
            Toast.makeText(this, "Error creating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdfViaEmail(File pdfFile) {
        // Get file URI using FileProvider
        Uri pdfUri = FileProvider.getUriForFile(this,
                this.getPackageName() + ".fileprovider",
                pdfFile);

        // Create email intent
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{}); // Leave empty for user to fill
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "BioMS Report - " +
                new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(new Date()));
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Here is your BioMS report.\n\n" +
                "Generated on: " + new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(new Date()));
        emailIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start email chooser
        try {
            startActivity(Intent.createChooser(emailIntent, "Send report via Gmail"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email apps installed", Toast.LENGTH_SHORT).show();
        }
    }
    private void initializeViews() {
        date = findViewById(R.id.date);
        shareReportButton = findViewById(R.id.shareReportButton);
        shareReportButton = findViewById(R.id.shareReportButton);
        // SD & CV
        BalanceSD = findViewById(R.id.BalanceSD);
        BalanceCV = findViewById(R.id.BalanceCV);
        WSSD = findViewById(R.id.WSSD);
        WSCV = findViewById(R.id.WSCV);
        EnduranceSD = findViewById(R.id.EnduranceSD);
        EnduranceCV = findViewById(R.id.EnduranceCV);
        // DTC
        dtcBalanceDT = findViewById(R.id.BalanceDTDTCToday);
        dtcSwayVelocityDT = findViewById(R.id.dtcSwayVelocityDT);
        dtcSwayPathDT = findViewById(R.id.dtcSwayPathDT);
        dtcBalanceSA = findViewById(R.id.BalanceSADTCToday);
        dtcSwayVelocitySA = findViewById(R.id.dtcSwayVelocitySA);
        dtcSwayPathSA = findViewById(R.id.dtcSwayPathSA);
        // Trials
        balanceTrialsTextView = findViewById(R.id.BalanceTrials);
        wsTrialsTextView = findViewById(R.id.WSTrials);
        enduranceTrialsTextView = findViewById(R.id.EnduranceTrials);
        dualTaskBalanceTrialsTextView = findViewById(R.id.DualTaskBalanceTrials);
        sensoryAtaxiaBalanceTrialsTextView = findViewById(R.id.SensoryAtaxiaBalanceTrials);
        swayPathLengthTrialsTextView = findViewById(R.id.swayPathLengthTrials);
        swayVelocityTrialsTextView = findViewById(R.id.swayVelocityTrials);
        swayRmsAPTrialsTextView = findViewById(R.id.swayRmsAPTrials);
        swayRmsMLTrialsTextView = findViewById(R.id.swayRmsMLTrials);
        dualTaskSwayPathLengthTrialsTextView = findViewById(R.id.dtSwayPathLengthTrials);
        dualTaskSwayVelocityTrialsTextView = findViewById(R.id.dtSwayVelocityTrials);
        dualTaskSwayRmsAPTrialsTextView = findViewById(R.id.dtSwayRmsAPTrials);
        dualTaskSwayRmsMLTrialsTextView = findViewById(R.id.dtSwayRmsMLTrials);
        sensoryAtaxiaSwayPathLengthTrialsTextView = findViewById(R.id.saSwayPathLengthTrials);
        sensoryAtaxiaSwayVelocityTrialsTextView = findViewById(R.id.saSwayVelocityTrials);
        sensoryAtaxiaSwayRmsAPTrialsTextView = findViewById(R.id.saSwayRmsAPTrials);
        sensoryAtaxiaSwayRmsMLTrialsTextView = findViewById(R.id.saSwayRmsMLTrials);
    }
}
