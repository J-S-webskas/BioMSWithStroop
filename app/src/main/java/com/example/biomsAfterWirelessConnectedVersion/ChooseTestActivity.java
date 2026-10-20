package com.example.biomsAfterWirelessConnectedVersion;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChooseTestActivity extends AppCompatActivity {

    private static String assessmentType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_test);
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
        String source = intent.getStringExtra("SOURCE");
        setupTestControls(source);

        // set the visibility of the dualTaskRecordARJ & sensoryAtaxiaRecordARJ
        // Grey out Dual Task textview if necessary
    }

    private void setupTestControls(String source) {
        CardView SwayAreaAndBalance = findViewById(R.id.SwayAreaAndBalance);
        CardView DualTask = findViewById(R.id.dualTask);
        CardView sensoryAtaxia = findViewById(R.id.sensoryAtaxia);
        CardView speedAndEndurance = findViewById(R.id.speedAndEndurance);
        TextView sensoryAtaxiaRecordARJ = findViewById(R.id.sensoryAtaxiaRecordARJ);
        TextView dualTaskRecordARJ = findViewById(R.id.dualTaskRecordARJ);
        TextView dualTaskText = findViewById(R.id.dualTaskText);
        TextView sensoryAtaxiaText = findViewById(R.id.sensoryAtaxiaText);
        ImageButton back = findViewById(R.id.back);
        TextView ARJCompleted = findViewById(R.id.SWABCompleted);
        TextView ARJText= findViewById(R.id.SWABText);
        TextView WSCompleted = findViewById(R.id.WSCompleted);
        TextView WSText = findViewById(R.id.WSText);

        boolean swabCompleted = testCompleted("SwayAreaAndBalance");

        // If Sway Area & balance was never recorded, disable Dual Task and Sensory Ataxia
        if (!swabCompleted) {
            dualTaskRecordARJ.setVisibility(VISIBLE);
            dualTaskText.setTextColor(Color.GRAY);
            sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
            sensoryAtaxiaText.setTextColor(Color.GRAY);
            dualTaskRecordARJ.setText("Record Sway Area and Balance first");
            sensoryAtaxiaRecordARJ.setText("Record Sway Area and Balance first");
        }

        SwayAreaAndBalance.setOnClickListener(v -> {
            assessmentType = "SwayAreaAndBalance";
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });

        DualTask.setOnClickListener(v -> {
            if (!swabCompleted) {
                dualTaskRecordARJ.setVisibility(VISIBLE);
                dualTaskText.setTextColor(Color.GRAY);
                dualTaskRecordARJ.setText("Record Sway Area and Balance first");
                return;
            }
            assessmentType = "DualTask";
            Intent intent = new Intent(this, MarkDistanceAndCamera.class);
            intent.putExtra("assessmentType", assessmentType);
            intent.putExtra("someoneRecord", false);
            startActivity(intent);
        });


        sensoryAtaxia.setOnClickListener(v -> {
            if (!swabCompleted) {
                sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
                sensoryAtaxiaText.setTextColor(Color.GRAY);
                sensoryAtaxiaRecordARJ.setText("Record Sway Area and Balance first");
                return;
            }
            assessmentType = "SensoryAtaxia";
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });

        speedAndEndurance.setOnClickListener(v -> {
            assessmentType = "SpeedAndEndurance";
            Intent intent = new Intent(this, HowWillYouTime.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });

        back.setOnClickListener(v -> {
            finish();// This closes the Activity and returns to whatever was before that Activity.
        });
    }

    private boolean testCompleted(String testType) {
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(this);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean swabDoneToday = false;
        boolean wsDoneToday = false;
        boolean dualTaskDoneToday = false;
        boolean sensoryDoneToday = false;

        for (AssessmentData assessment : allAssessment) {
            String assessmentDate = sdf.format(assessment.getTimestamp());
            // Only check today's assessments
            if (assessmentDate.equals(todayStr)) {
                // Check INDIVIDUAL test dates instead of combined status
                if (assessment.getSwayAreaAndBalanceTestDate() != null) {
                    swabDoneToday = true;
                }
                if (assessment.getWSAndEnduranceTestDate() != null) {
                    wsDoneToday = true;
                }
                if (assessment.getDualTaskTestDate() != null) {
                    dualTaskDoneToday = true;
                }
                if (assessment.getSensoryAtaxiaTestDate() != null) {
                    sensoryDoneToday = true;
                }
            }
        }
        if (testType.equals("SwayAreaAndBalance") && swabDoneToday) {
            return true;
        }
        if (testType.equals("SensoryAtaxia") && sensoryDoneToday) {
            return true;
        }
        if (testType.equals("DualTask") && dualTaskDoneToday) {
            return true;
        }
        if (testType.equals("WSAndEndurance") && wsDoneToday) {
            return true;
        }
        return false;
    }
}