package com.example.biomsAfterWirelessConnectedVersion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Standalone Stroop color-word test, opened from the Report screen.
 *
 * Shows a color word printed in a conflicting ink color; the user speaks
 * the INK color aloud and {@link StroopVoiceManager} judges each answer.
 * This is the same voice test used by the dual-task standing assessment,
 * presented on its own page so it can be tried (and its limits seen)
 * outside a recording session.
 *
 * The page carries an accuracy disclaimer: the result depends on voice
 * recognition, which is not 100% accurate — especially from a few meters
 * away, which is exactly how this app is used.
 */
public class StroopTestActivity extends AppCompatActivity {

    private static final int REQ_RECORD_AUDIO = 2002;

    private TextView tvWord;
    private TextView tvStatus;
    private TextView tvScore;
    private TextView tvResult;
    private Button btnStartStop;

    private StroopVoiceManager voiceManager;
    private boolean testRunning = false;
    private int heard = 0;
    private int correct = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stroop_test);

        tvWord = findViewById(R.id.tvStroopWord);
        tvStatus = findViewById(R.id.tvStroopStatus);
        tvScore = findViewById(R.id.tvStroopScore);
        tvResult = findViewById(R.id.tvStroopResult);
        btnStartStop = findViewById(R.id.btnStroopStartStop);

        btnStartStop.setOnClickListener(v -> {
            if (testRunning) {
                finishTest();
            } else {
                startTest();
            }
        });
    }

    private void startTest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            tvStatus.setText("Microphone permission needed — grant it, then tap Start again.");
            return;
        }

        heard = 0;
        correct = 0;
        updateScore();
        tvResult.setVisibility(View.GONE);

        // A stopped manager has destroyed its recognizer, so each run
        // gets a fresh instance.
        voiceManager = new StroopVoiceManager(this, new StroopVoiceManager.StroopVoiceListener() {
            @Override
            public void onNewTrial(String wordText, int colorInt) {
                tvWord.setText(wordText);
                tvWord.setTextColor(colorInt);
                tvStatus.setText("🎤 Listening… say the ink color");
            }

            @Override
            public void onSpokenResult(String spokenWord, boolean isCorrect) {
                heard++;
                if (isCorrect) correct++;
                tvStatus.setText("Heard: " + spokenWord + (isCorrect ? " ✓" : " ✗"));
                updateScore();
            }

            @Override
            public void onError(String errorMessage) {
                tvStatus.setText(errorMessage);
                Toast.makeText(StroopTestActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        testRunning = true;
        btnStartStop.setText("Stop Test");
        voiceManager.startTest();
    }

    private void finishTest() {
        testRunning = false;
        btnStartStop.setText("Start Test");
        if (voiceManager != null) {
            StroopVoiceManager.VoiceStroopResult result = voiceManager.stopTest();
            voiceManager = null;
            tvResult.setText(String.format(Locale.US,
                    "Trials: %d · Correct: %d · Accuracy: %.0f%% · Avg reaction time: %.0f ms",
                    result.totalTrials, result.correctCount,
                    result.accuracyPercent, result.avgReactionTimeMs));
            tvResult.setVisibility(View.VISIBLE);
        }
        tvStatus.setText("Test stopped.");
    }

    private void updateScore() {
        tvScore.setText("Score: " + correct + " / " + heard);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Release the microphone/recognizer when leaving the page.
        if (testRunning) {
            finishTest();
        }
    }
}
