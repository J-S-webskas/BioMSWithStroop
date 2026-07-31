package com.example.biomsAfterWirelessConnectedVersion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class WSAndEndurance extends AppCompatActivity {

    private Chronometer timer;
    private Button startTimerButton;
    private Button resetButton;
    private Button finish;
    private boolean isRunning = false;
    private long pauseOffset = 0;
    private int distance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ws_and_endurance);
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
        float meterType = intent.getFloatExtra("meterType", 0);
        if (meterType == 20) {
            distance = 20;
        } else {
            distance = 10;
        }
        timer = findViewById(R.id.timer);
        startTimerButton = findViewById(R.id.startTimer);
        resetButton = findViewById(R.id.reset_button);
        finish = findViewById(R.id.finish);
        startTimerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    // Start the timer
                    timer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                    timer.start();
                    isRunning = true;
                    startTimerButton.setText("Pause");
                    startTimerButton.setBackgroundTintList(ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#03DAC6")
                    ));
                } else {
                    // Pause the timer
                    timer.stop();
                    pauseOffset = SystemClock.elapsedRealtime() - timer.getBase();
                    isRunning = false;
                    startTimerButton.setText("Resume");
                    startTimerButton.setBackgroundTintList(ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#F6504F")
                    ));
               }
            }
        });

        // Reset button click listener
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.stop();
                timer.setBase(SystemClock.elapsedRealtime());
                pauseOffset = 0;
                isRunning = false;
                startTimerButton.setText("Start");
                startTimerButton.setBackgroundTintList(ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F6504F")
                ));
            }
        });

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.stop();
                if (isRunning) {
                    timer.stop();
                    pauseOffset = SystemClock.elapsedRealtime() - timer.getBase();
                }
                isRunning = false;
                startTimerButton.setText("Resume");
                startTimerButton.setBackgroundTintList(ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F6504F")
                ));
                // Result: "05:30" for 5 minutes and 30 seconds
//                String timeString = formatTimeToMinutesSeconds(pauseOffset);
                if (pauseOffset > 0) {
                    saveAndFinish();
                } else {
                    Toast.makeText(getApplicationContext(), "Timer not started", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveAndFinish() {
        // Save the results
        Intent resultIntent = getIntent();
        float distance = resultIntent.getFloatExtra("meterType", 0);
        // Create a NEW Intent for the result
        Intent intent = new Intent();
//        intent.putExtra("totalDistance", distance);
        intent.putExtra("testDuration", pauseOffset);
//        intent.putExtra("meterType", String.valueOf(distance));
        setResult(RESULT_OK, intent);
        finish();
    }
    private String formatTimeToMinutesSeconds(long millis) {
        int totalSeconds = (int) (millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
