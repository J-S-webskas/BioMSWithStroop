package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
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

import java.io.IOException;

public class ChooseYourSex extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_your_sex);

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

        CardView femaleCard = findViewById(R.id.femaleCard);
        CardView maleCard = findViewById(R.id.maleCard);
        CardView otherCard = findViewById(R.id.otherCard);
        ImageButton back = findViewById(R.id.back);

        femaleCard.setOnClickListener(v -> {
            try {

                DataStorageManager.setSex(ChooseYourSex.this, "female");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(this, ChooseYourWeight.class);
            startActivity(intent);
        });

        maleCard.setOnClickListener(v -> {
            try {
                DataStorageManager.setSex(this, "male");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(this, ChooseYourWeight.class);
            startActivity(intent);
        });

        otherCard.setOnClickListener(v -> {
            try {
                DataStorageManager.setSex(this, "other");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(this, ChooseYourWeight.class);
            startActivity(intent);
        });

        back.setOnClickListener(v -> {
            finish();
        });
    }
}
