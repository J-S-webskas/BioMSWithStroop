package com.example.biomsAfterWirelessConnectedVersion;

import static android.content.ContentValues.TAG;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
        CardView ARJAndBalance = findViewById(R.id.ARJAndBalance);
        CardView DualTask = findViewById(R.id.dualTask);
        CardView sensoryAtaxia = findViewById(R.id.sensoryAtaxia);
        CardView speedAndEndurance = findViewById(R.id.speedAndEndurance);
        TextView sensoryAtaxiaRecordARJ = findViewById(R.id.sensoryAtaxiaRecordARJ);
        TextView dualTaskRecordARJ = findViewById(R.id.dualTaskRecordARJ);
        TextView dualTaskText = findViewById(R.id.dualTaskText);
        TextView sensoryAtaxiaText = findViewById(R.id.sensoryAtaxiaText);
        ImageButton back = findViewById(R.id.back);
        TextView ARJCompleted = findViewById(R.id.ARJCompleted);
        TextView ARJText= findViewById(R.id.ARJText);
        TextView WSCompleted = findViewById(R.id.WSCompleted);
        TextView WSText = findViewById(R.id.WSText);

        boolean SACompleted = testCompleted("SensoryAtaxia");
        boolean arjCompleted = testCompleted("ARJAndBalance");
        boolean DTCompleted = testCompleted("DualTask");
        boolean wsCompleted = testCompleted("WSAndEndurance");

        // If ARJ and balance is not recorded, return
        if (!arjCompleted) {
            dualTaskRecordARJ.setVisibility(VISIBLE);
            dualTaskText.setTextColor(Color.GRAY);
            sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
            sensoryAtaxiaText.setTextColor(Color.GRAY);
        }
        if (SACompleted) {
            sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
            sensoryAtaxiaRecordARJ.setText("Completed. Next test is tomorrow");
            sensoryAtaxiaText.setTextColor(Color.GRAY);
        }
        if (DTCompleted) {
            dualTaskRecordARJ.setVisibility(VISIBLE);
            dualTaskRecordARJ.setText("Completed. Next test is tomorrow");
            dualTaskText.setTextColor(Color.GRAY);
        }
        if (arjCompleted) {
            ARJCompleted.setVisibility(VISIBLE);
            ARJText.setTextColor(Color.GRAY);
        }
        if (wsCompleted) {
            WSCompleted.setVisibility(VISIBLE);
            WSText.setTextColor(Color.GRAY);
        }

        ARJAndBalance.setOnClickListener(v -> {
            // If ARJ and balance is not recorded, return
            if (arjCompleted) {
                ARJCompleted.setVisibility(VISIBLE);
                ARJText.setTextColor(Color.GRAY);
                return;
            }
            assessmentType = "ARJAndBalance";
            // Navigate to how to record
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });

//        if (1==1) {
//            dualTaskRecordARJ.setVisibility(VISIBLE);
//            dualTaskText.setTextColor(Color.GRAY);
//        }

        DualTask.setOnClickListener(v -> {
            // If ARJ and balance is not recorded, return
            if (!arjCompleted) {
                dualTaskRecordARJ.setVisibility(VISIBLE);
                dualTaskText.setTextColor(Color.GRAY);
                return;
            }
            if (DTCompleted) {
                dualTaskRecordARJ.setVisibility(VISIBLE);
                dualTaskRecordARJ.setText("Completed. Next test is tomorrow");
                dualTaskText.setTextColor(Color.GRAY);
                return;
            }
            assessmentType = "DualTask";
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });


        sensoryAtaxia.setOnClickListener(v -> {
            // If ARJ and balance is not recorded, return
            if (!arjCompleted) {
                sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
                sensoryAtaxiaText.setTextColor(Color.GRAY);
                return;
            }
            if (SACompleted) {
                sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
                sensoryAtaxiaRecordARJ.setText("Completed. Next test is tomorrow");
                sensoryAtaxiaText.setTextColor(Color.GRAY);
                return;
            }
            assessmentType = "SensoryAtaxia";
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
            // Handle Sensory Ataxia
            // If ARJ and balance is not recorded, return
            // Other-wise, call camera
            // Set test = sa
        });

        speedAndEndurance.setOnClickListener(v -> {
            // If ARJ and balance is not recorded, return
            if (wsCompleted) {
                WSCompleted.setVisibility(VISIBLE);
                WSText.setTextColor(Color.GRAY);
                return;
            }
            assessmentType = "SpeedAndEndurance";
            Intent intent = new Intent(this, HowWillYouTime.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
            // Handle Speed and Endurance
        });

        back.setOnClickListener(v -> {
            finish();// This closes the Activity and returns to whatever was before that Activity.
        });
    }

    private boolean testCompleted(String testType) {
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(this);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String todayStr = sdf.format(new Date());
        boolean arjDoneToday = false;
        boolean wsDoneToday = false;
        boolean dualTaskDoneToday = false;
        boolean sensoryDoneToday = false;

        for (AssessmentData assessment : allAssessment) {
            String assessmentDate = sdf.format(assessment.getTimestamp());
            // Only check today's assessments
            if (assessmentDate.equals(todayStr)) {
                // Check INDIVIDUAL test dates instead of combined status
                if (assessment.getARJAndBalanceTestDate() != null) {
                    arjDoneToday = true;
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
        if (testType.equals("ARJAndBalance") && arjDoneToday) {
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