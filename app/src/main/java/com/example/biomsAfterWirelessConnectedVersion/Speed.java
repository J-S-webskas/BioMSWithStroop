package com.example.biomsAfterWirelessConnectedVersion;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Java File for distance & speed calculations

public class Speed extends AppCompatActivity implements LocationListener {

    private TextView localityTV, speedTV, averageSpeedTV, maxSpeedTV, distanceTV, timerTV;
    private static final int SPEED_TEST_REQUEST = 100;
    private LocationManager locationManager;
    private final Handler handler = new Handler();
    private Runnable resetSpeedRunnable;
    //    private PointerSpeedometer pointerSpeedometer;
    private float maxSpeed = 0.0f;
    static float totalDistance = 0.0f;
    static float currentSpeed = 0.0f;
    private Location previousLocation;
    private static final long TIMER_DURATION = 3 * 60 * 1000; // 3 minutes in milliseconds
    private long timeRemaining = TIMER_DURATION;
    private final ArrayList<Float> speedList = new ArrayList<>();

    // Timer variables
    private CountDownTimer countDownTimer;
    private int testDurationSeconds = 0;
    private boolean isTestActive = true;
    private boolean testCompleted = false;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean o) {
            if (o) {
                startLocationUpdates();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        localityTV = findViewById(R.id.localityTV);
        speedTV = findViewById(R.id.speedTV);
        averageSpeedTV = findViewById(R.id.avgSpeedTV);
        maxSpeedTV = findViewById(R.id.maxSpeedTV);
        distanceTV = findViewById(R.id.distanceTV);
        Button btn_cancel = findViewById(R.id.btn_cancel);
        timerTV = findViewById(R.id.timerTV);

        if (btn_cancel != null) {
            btn_cancel.setOnClickListener(v -> {cancelTest();});
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        showDurationInputDialog();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationUpdates();
        }

        resetSpeedRunnable = () -> {
            speedTV.setText((String.format(Locale.getDefault(), "Speed: %.2f km/h", 0.0f)));
//            pointerSpeedometer.speedTo(0);
        };

    }
    private void showDurationInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_timer_input, null);
        EditText etTimerSeconds = dialogView.findViewById(R.id.et_timer_seconds);
        etTimerSeconds.setText("30"); // Default 30 seconds

        builder.setTitle("⏱️ Set Walking Test Duration")
                .setView(dialogView)
                .setPositiveButton("Start Test", (dialog, which) -> {
                    String secondsStr = etTimerSeconds.getText().toString();
                    if (!secondsStr.isEmpty()) {
                        testDurationSeconds = Integer.parseInt(secondsStr);
                        if (testDurationSeconds >= 5 && testDurationSeconds <= 300) {
                            startTest();
                        } else {
                            Toast.makeText(this, "Please enter a duration between 5-300 seconds", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Please enter a duration", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Test cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void startTest() {
        // Request location permissions if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationUpdates();
        }
        startCountdownTimer();
    }

    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(testDurationSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isTestActive) return;

                long secondsRemaining = millisUntilFinished / 1000;
                int minutes = (int) (secondsRemaining / 60);
                int seconds = (int) (secondsRemaining % 60);
                String timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timerTV.setText(timeText);
            }

            @Override
            public void onFinish() {
                if (isTestActive) {
                    timerTV.setText("00:00");
                    testCompleted = true;
                    Toast.makeText(Speed.this, "Test completed! Saving data...", Toast.LENGTH_LONG).show();
                    saveAndFinish();
                }
            }
        }.start();
    }

    private void cancelTest() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Test?")
                .setMessage("Are you sure you want to cancel this walking test? Data will not be saved.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    isTestActive = false;
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    if (locationManager != null) {
                        locationManager.removeUpdates(this);
                    }
                    Toast.makeText(this, "Test cancelled. No data saved.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void saveAndFinish() {
        if (!testCompleted) return;

        // Stop location updates
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        // Save the results
        Intent resultIntent = new Intent();
        resultIntent.putExtra("currentSpeed", currentSpeed);
        resultIntent.putExtra("totalDistance", totalDistance);
        resultIntent.putExtra("maxSpeed", maxSpeed);
        resultIntent.putExtra("testDuration", testDurationSeconds);
        resultIntent.putExtra("testCompleted", true);
        setResult(RESULT_OK, resultIntent);

        finish();
    }
    private void startLocationUpdates() {
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
        } catch (Exception e) {
            Toast.makeText(this, "There was an error while listening for location updates", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private float calculateAverageSpeed() {
        float sum = 0.0f;
        for (float s : speedList) {
            sum += s;
        }
        return sum / speedList.size();
    }

    private String getLocalityFromLcation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        float speed = location.getSpeed() * 3.6f; // km/h
        speedList.add(speed);
//        pointerSpeedometer.speedTo(speed);

        if (speed > maxSpeed) {
            maxSpeed = speed;
        }

        float averageSpeed = calculateAverageSpeed();
        currentSpeed = averageSpeed;

        if (previousLocation == null) {
            // First location - just store it and show speed
            previousLocation = location;

            // Still show speed even on first update!
            speedTV.setText(String.format(Locale.getDefault(), "Speed: %.2f m/s", speed));
            distanceTV.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));
            averageSpeedTV.setText(String.format(Locale.getDefault(), "%.2f", averageSpeed));
            maxSpeedTV.setText(String.format(Locale.getDefault(), "%.2f", maxSpeed));

            String locality = getLocalityFromLcation(location);
            localityTV.setText(String.format(Locale.getDefault(), "%s", locality));
        } else {
            // Subsequent locations - calculate distance too
            float distance = previousLocation.distanceTo(location);
            totalDistance += distance / 1000.0f;

            previousLocation = location;

            speedTV.setText(String.format(Locale.getDefault(), "Speed: %.2f km/h", speed));
            distanceTV.setText(String.format(Locale.getDefault(), "%.2f", totalDistance));
            averageSpeedTV.setText(String.format(Locale.getDefault(), "%.2f", averageSpeed));
            maxSpeedTV.setText(String.format(Locale.getDefault(), "%.2f", maxSpeed));

            String locality = getLocalityFromLcation(location);
            localityTV.setText(String.format(Locale.getDefault(), "%s", locality));
        }

        // Reset timer
        handler.removeCallbacks(resetSpeedRunnable);
        handler.postDelayed(resetSpeedRunnable, 10000);

    }

}