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
        TextView logOut = findViewById(R.id.logOut);

        logOut.setOnClickListener(v -> {
            UserManager.logout(this);
            Intent intent = new Intent(Profile.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

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
