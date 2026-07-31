package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class ChooseYourWeight extends AppCompatActivity {
    private boolean weightSaved = false;

    EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_your_weight);

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

        Button next = findViewById(R.id.next);
        CardView weightCard = findViewById(R.id.weightCard);
        editText = findViewById(R.id.editText);
        ImageButton back = findViewById(R.id.back);

        weightCard.setOnClickListener(v -> {
            saveWeight();
        });

        next.setOnClickListener(v -> {
            if (saveWeight()) {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        back.setOnClickListener(v -> {
            finish();
        });
    }
    private void loadCurrentWeight() {
        double currentWeight = DataStorageManager.getMass(this);
        if (currentWeight > 0) {
            editText.setText(String.valueOf(currentWeight));
            editText.setSelection(editText.getText().length());
            weightSaved = true;
        } else {
            editText.setText("");
            editText.setHint("Enter your weight in kg");
            weightSaved = false;
        }
    }
    private boolean saveWeight() {
        String weightStr = editText.getText().toString().trim();

        if (weightStr.isEmpty()) {
            editText.setError("Please enter a weight");
            editText.requestFocus();
            return false;
        }

        try {
            double newWeight = Double.parseDouble(weightStr);
            if (newWeight <= 0) {
                editText.setError("Weight must be positive");
                editText.requestFocus();
                return false;
            }

            // Save the weight
            boolean success = DataStorageManager.setMass(this, newWeight);

            if (success) {
                editText.clearFocus();
                weightSaved = true;

                // Verify it was saved
                double verifyWeight = DataStorageManager.getMass(this);
                if (verifyWeight == newWeight) {
                    return true;
                } else {
                    Toast.makeText(this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "Failed to save weight. Please try again.", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (NumberFormatException e) {
            editText.setError("Invalid number format");
            editText.requestFocus();
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
