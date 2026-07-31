package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class HowWillYouRecord extends AppCompatActivity {

    private static boolean someoneRecord = false;
    private static boolean seeInstructionsOnly = false;

    // In on Create view, if users click the "I will prop the camera up", automatically set the camera to front facing (for preview & test)
    // Show the 2nd title, body and images
    // Otherwise, set it to back camera
    // Pass a notice into the markDistance activity to not show the 2nd title, body, and images

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_record);
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
        setupTestControls(assessmentType);

        // set the visibility of the dualTaskRecordARJ & sensoryAtaxiaRecordARJ
        // Grey out Dual Task textview if necessary
    }

    private void setupTestControls(String assessmentType) {
        CardView someoneElseRecord = findViewById(R.id.someoneRecord);
        CardView propRecord = findViewById(R.id.propRecord);
        ImageButton back = findViewById(R.id.back);

        Intent incomingIntent = getIntent();
        seeInstructionsOnly = incomingIntent.getBooleanExtra("seeInstructionsOnly", false);
        Log.d("HowWillYouRecord recieving", "seeInstructions seeInstructionsOnly recieved as " + seeInstructionsOnly);

        someoneElseRecord.setOnClickListener(v -> {
            HowWillYouRecord.someoneRecord = true;
            Intent intent = new Intent(this, MarkDistanceAndCamera.class);
            intent.putExtra("assessmentType", assessmentType);
            intent.putExtra("someoneRecord", someoneRecord);
            if (seeInstructionsOnly) {
                intent.putExtra("seeInstructionsOnly", true);
            }
            startActivity(intent);
        });

        propRecord.setOnClickListener(v -> {
            HowWillYouRecord.someoneRecord = false;
            Intent intent = new Intent(this, MarkDistanceAndCamera.class);
            intent.putExtra("assessmentType", assessmentType);
            intent.putExtra("someoneRecord", someoneRecord);
            if (seeInstructionsOnly) {
                intent.putExtra("seeInstructionsOnly", true);
            }
            startActivity(intent);
        });

        back.setOnClickListener(v -> {
            finish(); // This closes the Activity and returns to whatever was before that Activity.
        });
    }
}
