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

        boolean swabEverCompleted = hasEverCompletedSwayAreaAndBalance();

        // If Sway Area & balance was never recorded, disable Dual Task and Sensory Ataxia
        if (!swabEverCompleted) {
            dualTaskRecordARJ.setVisibility(VISIBLE);
            dualTaskText.setTextColor(Color.GRAY);
            sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
            sensoryAtaxiaText.setTextColor(Color.GRAY);
        }

        SwayAreaAndBalance.setOnClickListener(v -> {
            assessmentType = "SwayAreaAndBalance";
            Intent intent = new Intent(this, HowWillYouRecord.class);
            intent.putExtra("assessmentType", assessmentType);
            startActivity(intent);
        });

        DualTask.setOnClickListener(v -> {
            if (!swabEverCompleted && !hasEverCompletedSwayAreaAndBalance()) {
                dualTaskRecordARJ.setVisibility(VISIBLE);
                dualTaskText.setTextColor(Color.GRAY);
                return;
            }
            assessmentType = "DualTask";
            Intent intent = new Intent(this, MarkDistanceAndCamera.class);
            intent.putExtra("assessmentType", assessmentType);
            intent.putExtra("someoneRecord", false);
            startActivity(intent);
        });

        sensoryAtaxia.setOnClickListener(v -> {
            if (!swabEverCompleted && !hasEverCompletedSwayAreaAndBalance()) {
                sensoryAtaxiaRecordARJ.setVisibility(VISIBLE);
                sensoryAtaxiaText.setTextColor(Color.GRAY);
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

    private boolean hasEverCompletedSwayAreaAndBalance() {
        List<AssessmentData> allAssessment = DataStorageManager.loadAllAssessments(this);
        for (AssessmentData assessment : allAssessment) {
            if (assessment.getSwayAreaAndBalanceTestDate() != null ||
                (assessment.getBaseline() != null && assessment.getBaseline().getBalanceScore() > 0)) {
                return true;
            }
        }
        return false;
    }

}