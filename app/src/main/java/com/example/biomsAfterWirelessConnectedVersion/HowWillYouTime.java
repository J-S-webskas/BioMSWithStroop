package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;

public class HowWillYouTime extends AppCompatActivity  {

    private boolean selectedDistance = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_will_you_time);
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
        CardView twenty = findViewById(R.id.Twenty);
        CardView ten = findViewById(R.id.Ten);
        Button next = findViewById(R.id.next);
        ImageButton back = findViewById(R.id.back);

        MaterialCardView Card2 = (MaterialCardView) twenty;
        Card2.setStrokeColor(Color.WHITE);
        Card2.setStrokeWidth(1);
        MaterialCardView Card1 = (MaterialCardView) ten;
        Card1.setStrokeColor(Color.WHITE);
        Card1.setStrokeWidth(1);

        Bundle bundle = new Bundle();

        twenty.setOnClickListener(v -> {
            MaterialCardView currentCard = (MaterialCardView) twenty;
            currentCard.setStrokeColor(Color.RED);
            currentCard.setStrokeWidth(1);
            MaterialCardView prevCard = (MaterialCardView) ten;
            prevCard.setStrokeColor(Color.WHITE);
            prevCard.setStrokeWidth(1);
            selectedDistance = true;
            bundle.putFloat("meterType", 20);
        });

        ten.setOnClickListener(v -> {
            MaterialCardView currentCard = (MaterialCardView) ten;
            currentCard.setStrokeColor(Color.RED);
            currentCard.setStrokeWidth(1);
            MaterialCardView prevCard = (MaterialCardView) twenty;
            prevCard.setStrokeColor(Color.WHITE);
            prevCard.setStrokeWidth(1);
            selectedDistance = true;
            bundle.putFloat("meterType", 10);
        });

        next.setOnClickListener(v1 -> {
            if (selectedDistance) {
                RecordFragment recordFragment = new RecordFragment();
                recordFragment.setArguments(bundle);
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction()
                        .add(recordFragment, "RecordFragment")
                        .commitNow();
                recordFragment.startTest("SpeedAndEndurance", this);
            } else {
                Toast.makeText(this, "Select distance", Toast.LENGTH_SHORT).show();
            }
        });

        back.setOnClickListener(v -> {
            finish(); // This closes the Activity and returns to whatever was before that Activity.
        });
    }
}
