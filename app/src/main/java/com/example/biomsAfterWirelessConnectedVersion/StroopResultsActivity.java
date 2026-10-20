package com.example.biomsAfterWirelessConnectedVersion;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stroop test results for one report day: one card per dual-task trial
 * (the voice test runs while the dual-task sway recording runs). Tapping
 * a card expands it to show every answer the patient gave — the word
 * shown, the ink color, what was heard, right/wrong, and reaction time.
 *
 * Opened from the "Stroop Test" section of the report detail screen,
 * which presents these results as their own showcase, separate from the
 * dual-task sway table.
 */
public class StroopResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stroop_results);

        TextView tvDate = findViewById(R.id.stroopResultsDate);
        TextView tvEmpty = findViewById(R.id.stroopResultsEmpty);
        LinearLayout container = findViewById(R.id.stroopCardsContainer);

        String date = getIntent().getStringExtra("date");
        tvDate.setText(date != null ? date : "");

        List<ReportModel.StroopTrialData> trials = null;
        Object extra = getIntent().getSerializableExtra("stroopTrials");
        if (extra instanceof List) {
            @SuppressWarnings("unchecked")
            List<ReportModel.StroopTrialData> cast = (List<ReportModel.StroopTrialData>) extra;
            trials = cast;
        }
        if (trials == null) trials = new ArrayList<>();

        if (trials.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        Typeface fontRegular = ResourcesCompat.getFont(this, R.font.profile_pro);
        Typeface fontBold = ResourcesCompat.getFont(this, R.font.profile_pro_bold);

        for (int i = 0; i < trials.size(); i++) {
            container.addView(buildTrialCard(trials.get(i), i + 1, fontRegular, fontBold));
        }
    }

    private CardView buildTrialCard(ReportModel.StroopTrialData trial, int trialNumber,
                                    Typeface fontRegular, Typeface fontBold) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 16, 0, 0);
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(6f);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(36, 28, 36, 28);
        card.addView(inner);

        TextView header = new TextView(this);
        header.setTypeface(fontBold);
        header.setTextSize(18);
        header.setText(String.format(Locale.US,
                "Trial %d — %d/%d correct · %.0f%% · avg %.0f ms  ▾",
                trialNumber, trial.correctCount, trial.totalTrials,
                trial.accuracyPercent, trial.avgReactionMs));
        inner.addView(header);

        final LinearLayout answersBox = new LinearLayout(this);
        answersBox.setOrientation(LinearLayout.VERTICAL);
        answersBox.setVisibility(View.GONE);
        answersBox.setPadding(0, 20, 0, 0);
        inner.addView(answersBox);

        if (trial.answers == null || trial.answers.isEmpty()) {
            TextView none = new TextView(this);
            none.setTypeface(fontRegular);
            none.setTextSize(15);
            none.setText("No per-answer detail was recorded for this trial.");
            answersBox.addView(none);
        } else {
            int n = 1;
            for (StroopAnswerRecord a : trial.answers) {
                TextView row = new TextView(this);
                row.setTypeface(fontRegular);
                row.setTextSize(15);
                row.setPadding(0, 6, 0, 6);
                row.setText(String.format(Locale.US,
                        "%d. %s (ink: %s) — said “%s” %s · %,d ms",
                        n++, a.getWordText(), a.getInkColor(), a.getSpokenText(),
                        a.isCorrect() ? "✓" : "✗", a.getReactionTimeMs()));
                answersBox.addView(row);
            }
        }

        card.setOnClickListener(v -> {
            boolean show = answersBox.getVisibility() != View.VISIBLE;
            answersBox.setVisibility(show ? View.VISIBLE : View.GONE);
            header.setText(header.getText().toString().replace(show ? "▾" : "▴", show ? "▴" : "▾"));
        });

        return card;
    }
}
