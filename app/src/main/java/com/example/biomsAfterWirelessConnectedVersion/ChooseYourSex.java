package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

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

        MaterialCardView femaleCard = findViewById(R.id.femaleCard);
        MaterialCardView maleCard = findViewById(R.id.maleCard);
        MaterialCardView otherCard = findViewById(R.id.otherCard);
        ImageButton back = findViewById(R.id.back);

        MaterialCardView Card2 = (MaterialCardView) femaleCard;
        Card2.setStrokeColor(Color.WHITE);
        Card2.setStrokeWidth(1);
        MaterialCardView Card1 = (MaterialCardView) maleCard;
        Card1.setStrokeColor(Color.WHITE);
        Card1.setStrokeWidth(1);
        MaterialCardView Card = (MaterialCardView) otherCard;
        Card.setStrokeColor(Color.WHITE);
        Card.setStrokeWidth(1);

        femaleCard.setOnClickListener(v -> {
            MaterialCardView currentCard = (MaterialCardView) femaleCard;
            currentCard.setStrokeColor(Color.RED);
            currentCard.setStrokeWidth(1);
            MaterialCardView othCard = (MaterialCardView) otherCard;
            othCard.setStrokeColor(Color.WHITE);
            othCard.setStrokeWidth(1);
            MaterialCardView malCard = (MaterialCardView) maleCard;
            malCard.setStrokeColor(Color.WHITE);
            malCard.setStrokeWidth(1);
            try {
                DataStorageManager.setSex(ChooseYourSex.this, "female");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(this, ChooseYourWeight.class);
            startActivity(intent);
        });

        maleCard.setOnClickListener(v -> {
            MaterialCardView currentCard = (MaterialCardView) maleCard;
            currentCard.setStrokeColor(Color.RED);
            currentCard.setStrokeWidth(1);
            MaterialCardView femCard = (MaterialCardView) femaleCard;
            femCard.setStrokeColor(Color.WHITE);
            femCard.setStrokeWidth(1);
            MaterialCardView othCard = (MaterialCardView) otherCard;
            othCard.setStrokeColor(Color.WHITE);
            othCard.setStrokeWidth(1);
            try {
                DataStorageManager.setSex(this, "male");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(this, ChooseYourWeight.class);
            startActivity(intent);
        });

        otherCard.setOnClickListener(v -> {
            MaterialCardView currentCard = (MaterialCardView) otherCard;
            currentCard.setStrokeColor(Color.RED);
            currentCard.setStrokeWidth(1);
            MaterialCardView femCard = (MaterialCardView) femaleCard;
            femCard.setStrokeColor(Color.WHITE);
            femCard.setStrokeWidth(1);
            MaterialCardView malCard = (MaterialCardView) maleCard;
            malCard.setStrokeColor(Color.WHITE);
            malCard.setStrokeWidth(1);
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
