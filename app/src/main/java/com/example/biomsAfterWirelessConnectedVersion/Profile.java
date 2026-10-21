package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

public class Profile extends AppCompatActivity {

    private TextView currentWeight;
    private TextView currentGender;
    private TextView currentTrialDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

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

        CardView weight = findViewById(R.id.weight);
        CardView gender = findViewById(R.id.gender);
        currentGender = findViewById(R.id.currentGender);
        currentWeight = findViewById(R.id.currentWeight);

        weight.setOnClickListener(v -> {
            showWeightDialog();
        });

        gender.setOnClickListener(v -> {
            showSexDialog();
        });

        String sex = DataStorageManager.getSex(this);
        currentGender.setText(sex);

        double mass = DataStorageManager.getMass(this);
        currentWeight.setText("" + mass);

        // The anonymous ID that identifies this patient in reports and
        // exported data — never their name.
        TextView currentPatientId = findViewById(R.id.currentPatientId);
        currentPatientId.setText(AnonymousIdManager.getId(this));

        // Camera distance for the COM anchor: the tape-measured distance
        // the subject stands from the camera. When set, it anchors the
        // sway scale; when unset, the app calibrates from the camera.
        CardView trialDistance = findViewById(R.id.trialDistance);
        currentTrialDistance = findViewById(R.id.currentTrialDistance);
        trialDistance.setOnClickListener(v -> showTrialDistanceDialog());
        refreshTrialDistanceLabel();
    }

    private void refreshTrialDistanceLabel() {
        float d = getSharedPreferences(PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE)
                .getFloat(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M, 0f);
        if (currentTrialDistance != null) {
            currentTrialDistance.setText(d > 0
                    ? String.format(java.util.Locale.US, "%.2f m", d)
                    : "Camera (auto)");
        }
    }

    private void showTrialDistanceDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Camera Distance")
                .setMessage("Tape-measured distance from the camera to where the subject stands, in meters. Leave empty to let the app calibrate from the camera instead.")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_trial_distance, null);
        EditText distanceInput = dialogView.findViewById(R.id.distanceInput);
        float current = getSharedPreferences(PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE)
                .getFloat(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M, 0f);
        if (current > 0) {
            distanceInput.setText(String.format(java.util.Locale.US, "%.2f", current));
        }
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String s = distanceInput.getText().toString().trim();
                if (s.isEmpty()) {
                    getSharedPreferences(PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE)
                            .edit().remove(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M).apply();
                    Toast.makeText(this, "Camera distance cleared -- camera calibration will be used", Toast.LENGTH_SHORT).show();
                    refreshTrialDistanceLabel();
                    dialog.dismiss();
                    return;
                }
                try {
                    double parsed = Double.parseDouble(s);
                    if (parsed < 0.5 || parsed > 10) {
                        distanceInput.setError("Enter a distance between 0.5 and 10 m");
                        return;
                    }
                    getSharedPreferences(PoseLandmarkerHelper.TRIAL_SETUP_PREFS, MODE_PRIVATE)
                            .edit().putFloat(PoseLandmarkerHelper.KEY_MANUAL_DISTANCE_M, (float) parsed).apply();
                    Toast.makeText(this, "Camera distance saved: " + parsed + " m", Toast.LENGTH_SHORT).show();
                    refreshTrialDistanceLabel();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    distanceInput.setError("Invalid number format");
                }
            });
        });
        dialog.show();
    }

    private void showWeightDialog() {
        // Create the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Update Weight")
                .setMessage("Enter your current weight")
                .setPositiveButton("Save", null) // Set null initially
                .setNegativeButton("Cancel", null);

        // Inflate custom view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_weight, null);
        EditText weightInput = dialogView.findViewById(R.id.weightInput);

        // Set current weight
        double currentWeight = DataStorageManager.getMass(this);
        weightInput.setText(String.valueOf(currentWeight));

        builder.setView(dialogView);

        // Create dialog to set custom button behavior
        AlertDialog dialog = builder.create();

        // Override positive button behavior to validate input
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String weightStr = weightInput.getText().toString();
                if (weightStr.isEmpty()) {
                    weightInput.setError("Please enter a weight");
                    return;
                }
                try {
                    double newWeight = Double.parseDouble(weightStr);
                    if (newWeight <= 0) {
                        weightInput.setError("Weight must be positive");
                        return;
                    }
                    DataStorageManager.setMass(this, newWeight);
                    Toast.makeText(this, "Weight saved: " + newWeight + " kg", Toast.LENGTH_SHORT).show();
                    loadUserData();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    weightInput.setError("Invalid number format");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
        dialog.show();
    }

    private void showSexDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sex, null);

        RadioGroup sexRadioGroup = dialogView.findViewById(R.id.sexRadioGroup);
        RadioButton maleRadio = dialogView.findViewById(R.id.maleRadio);
        RadioButton femaleRadio = dialogView.findViewById(R.id.femaleRadio);
        RadioButton otherRadio = dialogView.findViewById(R.id.otherRadio);

        // Set current selection
        String currentSex = DataStorageManager.getSex(this); // Your method
        if ("Male".equalsIgnoreCase(currentSex)) {
            maleRadio.setChecked(true);
        } else if ("Female".equalsIgnoreCase(currentSex)) {
            femaleRadio.setChecked(true);
        } else if ("Other".equalsIgnoreCase(currentSex)) {
            otherRadio.setChecked(true);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Gender")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selectedId = sexRadioGroup.getCheckedRadioButtonId();
                    String selectedSex = "";
                    if (selectedId == R.id.maleRadio) {
                        selectedSex = "Male";
                    } else if (selectedId == R.id.femaleRadio) {
                        selectedSex = "Female";
                    } else if (selectedId == R.id.otherRadio) {
                        selectedSex = "Other";
                    }

                    if (!selectedSex.isEmpty()) {
                        try {
                            DataStorageManager.setSex(this, selectedSex); // Your save method
                            loadUserData();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

    private void loadUserData() {
        // Load and display sex
        String sex = DataStorageManager.getSex(this);
        if (sex != null && !sex.isEmpty() && !sex.equals("Not Set")) {
            currentGender.setText(sex);
        } else {
            currentGender.setText("Not Set");
        }

        // Load and display mass
        double mass = DataStorageManager.getMass(this);
        if (mass > 0) {
            currentWeight.setText(String.valueOf(mass));
        } else {
            currentWeight.setText("Not Set");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning to this activity
        loadUserData();
    }
}
