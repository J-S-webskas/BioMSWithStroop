package com.example.bioms1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    TextView date, ARJTodayRight, ARJTodayLeft, ARJTrendLeft, ARJTrendRight,
            balanceToday, balanceTrend,
            speedToday, speedTrend,
            enduranceToday, enduranceTrend,
            sensoryAtaxiaARJLeftToday, sensoryAtaxiaARJRightToday, sensoryAtaxiaBalanceToday, sensoryAtaxiaTrend,
            dualTaskARJLeftToday, dualTaskARJRightToday, dualTaskBalanceToday, dualTaskTrend;

    private Button shareReportButton;
    private String reportDate;
    private float speedValue, speedTrendValue;
    private float arjRightValue, arjTrendRightValue;
    private float arjLeftValue, arjTrendLeftValue;
    private float balanceValue, balanceTrendValue;
    private float enduranceValue, enduranceTrendValue;
    private float sensoryAtaxiaARJLeftValue, sensoryAtaxiaARJRightValue, sensoryAtaxiaBalanceValue, sensoryAtaxiaTrendValue;
    private float dualTaskARJLeftValue, dualTaskARJRightValue, dualTaskBalanceValue, dualTaskTrendValue;

    private File reportFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        // Initialize all views
        initializeViews();

        // Get the report data from Intent
        getReportData();

        // Display the report data
        displayReportDetails();

        shareReportButton.setOnClickListener(v -> {
            createReportFile(); // Create the file first
            sendReportViaEmail(); // Then send it
        });
    }

    private void getReportData() {

        Intent intent = getIntent();  // Gets the intent that started this activity (the card views in report fragment)

        // Extract data from intent/report fragment and store them in class variables
        reportDate = intent.getStringExtra("date");
        speedValue = intent.getFloatExtra("speed", 0.0f);
        speedTrendValue = intent.getFloatExtra("speedTrend", 0.0f);
        arjRightValue = intent.getFloatExtra("arjRight", 0.0f);
        arjTrendRightValue = intent.getFloatExtra("arjRightTrend", 0.0f);
        arjLeftValue = intent.getFloatExtra("arjLeft", 0.0f);
        arjTrendLeftValue = intent.getFloatExtra("arjLeftTrend", 0.0f);
        balanceValue = intent.getFloatExtra("balance", 0.0f);
        balanceTrendValue = intent.getFloatExtra("balanceTrend", 0.0f);
        enduranceValue = intent.getFloatExtra("endurance", 0.0f);
        enduranceTrendValue = intent.getFloatExtra("enduranceTrend", 0.0f);
        sensoryAtaxiaARJLeftValue = intent.getFloatExtra("sensoryAtaxiaARJLeft", 0.0f);
        sensoryAtaxiaARJRightValue = intent.getFloatExtra("sensoryAtaxiaARJRight", 0.0f);
        sensoryAtaxiaBalanceValue = intent.getFloatExtra("sensoryAtaxiaBalance", 0.0f);
        sensoryAtaxiaTrendValue = intent.getFloatExtra("sensoryAtaxiaTrend", 0.0f);
        dualTaskARJLeftValue = intent.getFloatExtra("dualTaskARJLeft", 0.0f);
        dualTaskARJRightValue = intent.getFloatExtra("dualTaskARJRight", 0.0f);
        dualTaskBalanceValue = intent.getFloatExtra("dualTaskBalance", 0.0f);
        dualTaskTrendValue = intent.getFloatExtra("dualTaskTrend", 0.0f);

        // todo: need last months data!!!
    }

    private void displayReportDetails() {
        date.setText(reportDate);

        // Speed section
        speedToday.setText("• Today's value: " + String.format("%.1f km/h", speedValue));
        speedTrend.setText("• This month's trend: " + String.format("%.1f", speedTrendValue));

        // ARJ section
        ARJTodayRight.setText("• Today's value (right knee): " + String.format("%.2f", arjRightValue));
        ARJTodayLeft.setText("• Today's value (left knee): " + String.format("%.2f", arjLeftValue));
        ARJTrendRight.setText("• This month's trend (right knee): " + String.format("%.2f", arjTrendRightValue));
        ARJTrendLeft.setText("• This month's trend (left knee): " + String.format("%.2f", arjTrendLeftValue));

        // Balance section
        balanceToday.setText("• Today's value: " + String.format("%.1f", balanceValue));
        balanceTrend.setText("This month's trend: " + String.format("%.1f", balanceTrendValue));

        // Endurance section
        enduranceToday.setText("• Today's value: " + String.format("%.1f", enduranceValue));
        enduranceTrend.setText("• This month's trend: " + String.format("%.1f", enduranceTrendValue));

        // Sensory Ataxia section
        sensoryAtaxiaARJLeftToday.setText("• Sensory ataxia's value (ARJ of left knee): " + String.format("%.1f", sensoryAtaxiaARJLeftValue));
        sensoryAtaxiaARJRightToday.setText("• Sensory ataxia's value (ARJ of right knee): " + String.format("%.1f", sensoryAtaxiaARJRightValue));
        sensoryAtaxiaBalanceToday.setText("• Sensory ataxia's value (balance) :" + String.format("%.1f", sensoryAtaxiaBalanceValue));
        sensoryAtaxiaTrend.setText("• This month's trend: " + String.format("%.1f", sensoryAtaxiaTrendValue));

        // Dual Task section
        dualTaskARJLeftToday.setText("• Dual task's value (ARJ of left knee): " + String.format("%.1f", dualTaskARJLeftValue));
        dualTaskARJRightToday.setText("• Dual task's value (ARJ of right knee): " + String.format("%.1f", dualTaskARJRightValue));
        dualTaskBalanceToday.setText("• Dual task's value (balance): " + String.format("%.1f", dualTaskBalanceValue));
        dualTaskTrend.setText("• This month's trend: " + String.format("%.1f", dualTaskTrendValue));
    }

    private void setTrends() {


    }

    private String getTrendSummary(float currentValue, float previousValue) {
        if (previousValue == 0) {
            return "First measurement";
        }

        float percentChange = ((currentValue - previousValue) / previousValue) * 100;

        if (Math.abs(percentChange) < 3.0f) {
            return "Stable";
        } else if (percentChange > 0) {
            if (percentChange > 20) {
                return "Significantly improving ↑";
            } else {
                return "Improving ↑";
            }
        } else {
            if (percentChange < -20) {
                return "Significantly declining ↓";
            } else {
                return "Declining ↓";
            }
        }
    }

    private void initializeViews() {
        date = findViewById(R.id.date);
        ARJTodayRight = findViewById(R.id.ARJTodayRight);
        ARJTodayLeft = findViewById(R.id.ARJTodayLeft);
        ARJTrendLeft = findViewById(R.id.ARJTrendLeft);
        ARJTrendRight = findViewById(R.id.ARJTrendRight);
        balanceToday = findViewById(R.id.balanceToday);
        balanceTrend = findViewById(R.id.balanceTrend);
        speedToday = findViewById(R.id.speedToday);
        speedTrend = findViewById(R.id.speedTrend);
        enduranceToday = findViewById(R.id.enduranceToday);
        enduranceTrend = findViewById(R.id.enduranceTrend);
        sensoryAtaxiaARJLeftToday = findViewById(R.id.sensoryAtaxiaARJLeftToday);
        sensoryAtaxiaARJRightToday = findViewById(R.id.sensoryAtaxiaARJRightToday);
        sensoryAtaxiaBalanceToday = findViewById(R.id.sensoryAtaxiaBalanceToday);
        sensoryAtaxiaTrend = findViewById(R.id.sensoryAtaxiaTrend);
        dualTaskARJLeftToday = findViewById(R.id.dualTaskARJLeftToday);
        dualTaskARJRightToday = findViewById(R.id.dualTaskARJRightToday);
        dualTaskBalanceToday = findViewById(R.id.dualTaskBalanceToday);
        dualTaskTrend = findViewById(R.id.dualTaskTrend);
        shareReportButton = findViewById(R.id.shareReportButton);
    }

    // todo: allowing gmail sending
    private void sendReportViaEmail() {
        // Create the intent with ACTION_SEND
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        // Set the MIME type - "message/rfc822" is the standard for email
        emailIntent.setType("message/rfc822");

        // Add email details
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{}); // To address
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "BioMS: Your Symptom Report");
        emailIntent.putExtra(Intent.EXTRA_TEXT, buildReportContent()); // Email body

        // If you want to attach a file (like a PDF report)
        if (reportFile != null && reportFile.exists()) {
            // For Android 7.0+, use FileProvider to get a content URI
            Uri fileUri = FileProvider.getUriForFile(this,
                    this.getPackageName() + ".fileprovider",
                    reportFile);

            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        // Start the activity with a chooser
        try {
            startActivity(Intent.createChooser(emailIntent, "Send report via..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this,
                    "No email apps installed on this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void createReportFile() {
        try {
            // Create filename with timestamp
            String fileName = "symptom_report_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".txt";

            // Get the app's external files directory
            File reportsDir = new File(this.getExternalFilesDir(null), "reports");
            if (!reportsDir.exists()) {
                reportsDir.mkdirs();
            }

            reportFile = new File(reportsDir, fileName);

            // Write the report content to the file
            FileOutputStream fos = new FileOutputStream(reportFile);
            fos.write(buildReportContent().getBytes());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating report file", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildReportContent() {
        StringBuilder content = new StringBuilder();

        // Header with personalized greeting
        content.append("Hello, ").append(getUserName()).append("!\n");
        content.append("Here are your BioMS Insights compared for these two periods:\n");
        content.append("1 / 3\n\n");

        // Report Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        content.append("Report Date: ").append(dateFormat.format(new Date())).append("\n\n");

        // Time periods (you'll need to calculate these based on your data)
        String recentPeriod = getRecentPeriodDateRange();
        String previousPeriod = getPreviousPeriodDateRange();
        content.append("Recent period: ").append(recentPeriod).append(" | ");
        content.append("Previous period: ").append(previousPeriod).append("\n\n");

        // Section 1: Symptom Log
        content.append("Symptom Log\n");
        content.append("DATA RECORDED FROM SYMPTOM LOGS, DAILY CHECK-INS AND NOTES\n\n");

        // Add symptom insights based on data
        if (hasSignificantSymptoms()) {
            content.append(getSymptomInsights());
        } else {
            content.append("Great job! You've been managing your symptoms well this period. ");
            content.append("Continue logging any symptoms you experience - this information helps ");
            content.append("paint a clearer picture of your MS journey.\n\n");
        }

        content.append("This summary was generated to help you track your MS progression.*\n\n");

        // Symptom stats table
        content.append("SYMPTOM LOGS | RECENT PERIOD | PREV PERIOD\n");
        content.append(String.format("Fatigue: %d | %d\n", getFatigueCount(), getPrevFatigueCount()));
        content.append(String.format("Numbness: %d | %d\n", getNumbnessCount(), getPrevNumbnessCount()));
        content.append(String.format("Balance Issues: %d | %d\n", getBalanceIssueCount(), getPrevBalanceIssueCount()));
        content.append(String.format("Vision Problems: %d | %d\n\n", getVisionCount(), getPrevVisionCount()));

        content.append("Monitoring your symptoms is essential for effectively managing Multiple Sclerosis. ");
        content.append("Continue logging to help your healthcare team better understand your condition.\n\n");

        // Symptom intensity
        content.append("Most reported symptom intensity\n");
        content.append("Recent period: ").append(getMostCommonIntensity()).append("\n");
        content.append("Previous period: ").append(getPrevMostCommonIntensity()).append("\n\n");

        content.append("LOGS: RECENT | PREVIOUS\n");
        content.append(String.format("Severe: %dd | %dd\n", getSevereDays(), getPrevSevereDays()));
        content.append(String.format("Moderate: %dd | %dd\n", getModerateDays(), getPrevModerateDays()));
        content.append(String.format("Mild: %dd | %dd\n", getMildDays(), getPrevMildDays()));
        content.append(String.format("Not Present: %dd | %dd\n\n", getNoSymptomDays(), getPrevNoSymptomDays()));

        content.append("2 / 3\n\n");

        // Section 2: Assessment Results
        content.append("From Your Assessments\n");
        content.append("DATA RECORDED FROM BIOMS ASSESSMENTS\n\n");

        // ARJ (Range of Motion) section
        if (hasARJData()) {
            content.append("Active Range of Motion (ARJ)\n");
            content.append(String.format("• Right Knee: %.1f° (%.1f%% vs previous period)\n",
                    arjRightValue, arjTrendRightValue));
            content.append(String.format("• Left Knee: %.1f° (%.1f%% vs previous period)\n\n",
                    arjLeftValue, arjTrendLeftValue));
        } else {
            content.append("No range of motion data is available for recent period.\n");
            content.append("Complete the ARJ assessment in the app to track your joint mobility. ");
            content.append("Regular assessment helps monitor changes in your range of motion.\n\n");
        }

        // Balance section
        if (hasBalanceData()) {
            content.append("Balance Assessment\n");
            content.append(String.format("• Balance Score: %.1f (%.1f%% vs previous period)\n\n",
                    balanceValue, balanceTrendValue));
        } else {
            content.append("No balance data is available for recent period.\n");
            content.append("Complete the balance assessment to track your stability over time. ");
            content.append("This is important for fall prevention and monitoring disease progression.\n\n");
        }

        // Walking Speed & Endurance
        if (hasWalkingData()) {
            content.append("Mobility & Endurance\n");
            content.append(String.format("• Walking Speed: %.1f km/h (%.1f%% vs previous period)\n",
                    speedValue, speedTrendValue));
            content.append(String.format("• Endurance: %.1f seconds (%.1f%% vs previous period)\n\n",
                    enduranceValue, enduranceTrendValue));
        } else {
            content.append("No mobility data is available for recent period.\n");
            content.append("Complete the walking assessments to track your mobility. ");
            content.append("For best results, perform assessments at the same time of day.\n\n");
        }

        // Sensory Ataxia section
        if (hasSensoryAtaxiaData()) {
            content.append("Sensory Ataxia Assessment\n");
            content.append(String.format("• ARJ Left Knee Change: %.1f%%\n", sensoryAtaxiaARJLeftValue));
            content.append(String.format("• ARJ Right Knee Change: %.1f%%\n", sensoryAtaxiaARJRightValue));
            content.append(String.format("• Balance Change: %.1f%%\n", sensoryAtaxiaBalanceValue));
            content.append(String.format("• Overall Trend: %.1f%%\n\n", sensoryAtaxiaTrendValue));
        }

        // Dual Task section
        if (hasDualTaskData()) {
            content.append("Dual Task Assessment\n");
            content.append(String.format("• ARJ Left Knee Change: %.1f%%\n", dualTaskARJLeftValue));
            content.append(String.format("• ARJ Right Knee Change: %.1f%%\n", dualTaskARJRightValue));
            content.append(String.format("• Balance Change: %.1f%%\n", dualTaskBalanceValue));
            content.append(String.format("• Overall Trend: %.1f%%\n\n", dualTaskTrendValue));
        }

        content.append("3 / 3\n\n");

        // Section 3: Recommendations and Tips
        content.append("We're improving your insights in BioMS!\n\n");
        content.append("This is a comprehensive summary of your data captured by the BioMS app. ");
        content.append("For optimal results:\n");
        content.append("• Complete assessments at consistent times of day\n");
        content.append("• Log your symptoms regularly\n");
        content.append("• Perform assessments in the same environment each time\n");
        content.append("• Share this report with your healthcare team\n\n");

        content.append("To obtain valuable insights, continue engaging with the app and complete ");
        content.append("assessments regularly before your next report. Insights become more valuable ");
        content.append("with consistent usage over time.\n\n");

        content.append("*This report is generated based on your assessment data and symptom logs ");
        content.append("within the BioMS app.\n\n");

        // Footer with disclaimer
        content.append("Confidential: This report is for informational purposes only and should ");
        content.append("only be shared with the user's immediate care team. Please consult your ");
        content.append("healthcare provider for any concerns or questions related to your medical ");
        content.append("condition and treatment plan.\n\n");

        content.append("If you are having a medical emergency, seek immediate medical care.\n\n");

        return content.toString();
    }

    // Helper methods you'll need to implement based on your data structure
    private String getUserName() {
        // Retrieve user name from SharedPreferences or your data store
        return getSharedPreferences("BioMS_Prefs", MODE_PRIVATE)
                .getString("user_name", "there");
    }

    private String getRecentPeriodDateRange() {
        // Calculate based on your data - example: "Nov 03, 2025 - Nov 30, 2025"
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        // You'll need to calculate these dates based on your actual data
        return "MMM dd, yyyy - MMM dd, yyyy";
    }

    private String getPreviousPeriodDateRange() {
        // Similar to above for previous period
        return "MMM dd, yyyy - MMM dd, yyyy";
    }

    private boolean hasSignificantSymptoms() {
        // Check if there are symptoms worth highlighting
        return false;
    }

    private String getSymptomInsights() {
        return "We noticed you logged [specific symptom] this period. ";
        // Add personalized insights based on symptoms
    }

    // Data count methods - implement based on your data structure
    private int getFatigueCount() { return 0; }
    private int getPrevFatigueCount() { return 0; }
    private int getNumbnessCount() { return 0; }
    private int getPrevNumbnessCount() { return 0; }
    private int getBalanceIssueCount() { return 0; }
    private int getPrevBalanceIssueCount() { return 0; }
    private int getVisionCount() { return 0; }
    private int getPrevVisionCount() { return 0; }

    private String getMostCommonIntensity() { return "Mild"; }
    private String getPrevMostCommonIntensity() { return "Mild"; }

    private int getSevereDays() { return 0; }
    private int getPrevSevereDays() { return 0; }
    private int getModerateDays() { return 0; }
    private int getPrevModerateDays() { return 0; }
    private int getMildDays() { return 0; }
    private int getPrevMildDays() { return 0; }
    private int getNoSymptomDays() { return 0; }
    private int getPrevNoSymptomDays() { return 0; }

    private boolean hasARJData() { return arjRightValue > 0 || arjLeftValue > 0; }
    private boolean hasBalanceData() { return balanceValue > 0; }
    private boolean hasWalkingData() { return speedValue > 0 || enduranceValue > 0; }
    private boolean hasSensoryAtaxiaData() {
        return sensoryAtaxiaARJLeftValue > 0 || sensoryAtaxiaARJRightValue > 0;
    }
    private boolean hasDualTaskData() {
        return dualTaskARJLeftValue > 0 || dualTaskARJRightValue > 0;
    }
}
