package com.example.bioms1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

//import com.github.anastr.speedviewlib.PointerSpeedometer;
import com.example.bioms1.R;
import com.google.android.material.snackbar.Snackbar;

//import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
//import androidx.navigation.ui.AppBarConfiguration;
//import androidx.navigation.ui.NavigationUI;
//
//import com.example.myappication.databinding.ActivitySpeedBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Java File for distance & speed calculations

public class Speed extends AppCompatActivity implements LocationListener {

    private TextView localityTV, speedTV, averageSpeedTV, maxSpeedTV, distanceTV, timerTV;
    private Button btn_finish;
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
        super.onCreate((savedInstanceState));
//        EdgeToEdge.enable(this);
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
        btn_finish = findViewById(R.id.btn_finish);
        timerTV = findViewById(R.id.timerTV);

//        pointerSpeedometer = findViewById(R.id.speedometer);
//        pointerSpeedometer.setMaxSpeed(0);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            startLocationUpdates();
        }

        resetSpeedRunnable = () -> {
            speedTV.setText((String.format(Locale.getDefault(), "Speed: %.2f km/h", 0.0f)));
//            pointerSpeedometer.speedTo(0);
        };

        btn_finish.setOnClickListener(v -> {

            if (currentSpeed <= 0 || totalDistance <= 0) {
                Toast.makeText(this, "Please wait for GPS signal and walk for a few seconds", Toast.LENGTH_LONG).show();
                return;  // Don't finish yet!
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("currentSpeed", currentSpeed);
            resultIntent.putExtra("totalDistance", totalDistance);
            setResult(RESULT_OK, resultIntent);

            finish();
        });
        startCountdownTimer();
    }
    private void startCountdownTimer() {
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeRemaining > 0) {
                    timeRemaining -= 1000; // Decrease by 1 second
                    updateTimerDisplay();
                    handler.postDelayed(this, 1000);
                } else {
                    timerTV.setText("00:00");
                }
            }
        };

        handler.post(timerRunnable);
    }
    
    private void updateTimerDisplay() {
        int minutes = (int) (timeRemaining / 1000 / 60);
        int seconds = (int) (timeRemaining / 1000 % 60);

        String timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerTV.setText(timeText);
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
            speedTV.setText(String.format(Locale.getDefault(), "Speed: %.2f km/h", speed));
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