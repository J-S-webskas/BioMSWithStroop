package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class StroopVoiceManager implements RecognitionListener {

    public interface StroopVoiceListener {
        void onNewTrial(String wordText, int colorInt);
        void onSpokenResult(String spokenWord, boolean isCorrect);
        void onError(String errorMessage);
    }

    public static class StroopTrial {
        public final String wordText;
        public final int colorInt;
        public final String colorName;

        public StroopTrial(String wordText, int colorInt, String colorName) {
            this.wordText = wordText;
            this.colorInt = colorInt;
            this.colorName = colorName;
        }
    }

    public static class VoiceStroopResult {
        public final int totalTrials;
        public final int correctCount;
        public final float accuracyPercent;
        public final float avgReactionTimeMs;

        public VoiceStroopResult(int totalTrials, int correctCount, float accuracyPercent, float avgReactionTimeMs) {
            this.totalTrials = totalTrials;
            this.correctCount = correctCount;
            this.accuracyPercent = accuracyPercent;
            this.avgReactionTimeMs = avgReactionTimeMs;
        }
    }

    private static final String TAG = "StroopVoiceManager";
    private static final String[] COLOR_NAMES = {"RED", "BLUE", "GREEN", "YELLOW"};
    private static final int[] COLOR_VALUES = {
            0xFFFF0000, // Color.RED
            0xFF0000FF, // Color.BLUE
            0xFF008000, // Green
            0xFFFFB300  // Yellow
    };

    private final Context context;
    private final StroopVoiceListener listener;
    private final Random random = new Random();
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    private StroopTrial currentTrial;
    private long trialStartTimeMs;
    private boolean isListeningActive = false;

    private int lastTextIndex = -1;
    private int lastColorIndex = -1;

    private int totalTrials = 0;
    private int correctCount = 0;
    private final List<Long> reactionTimes = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StroopVoiceManager(Context context, StroopVoiceListener listener) {
        this.context = context;
        this.listener = listener;
        initSpeechRecognizer();
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech Recognition not supported on this device.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    public void startTest() {
        totalTrials = 0;
        correctCount = 0;
        reactionTimes.clear();
        lastTextIndex = -1;
        lastColorIndex = -1;
        isListeningActive = true;

        nextTrial();
        startListening();
    }

    private void startListening() {
        if (!isListeningActive || speechRecognizer == null) return;
        mainHandler.post(() -> {
            try {
                speechRecognizer.startListening(recognizerIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting speech recognizer: " + e.getMessage());
            }
        });
    }

    public void nextTrial() {
        if (!isListeningActive) return;

        int textIndex;
        int colorIndex;

        // Pick a random word text index that is different from last trial's word
        do {
            textIndex = random.nextInt(COLOR_NAMES.length);
        } while (COLOR_NAMES.length > 1 && textIndex == lastTextIndex);

        // Pick a color index that is NOT equal to textIndex (word != color) AND NOT equal to lastColorIndex (color changes each time)
        do {
            colorIndex = random.nextInt(COLOR_NAMES.length);
        } while (colorIndex == textIndex || (COLOR_NAMES.length > 2 && colorIndex == lastColorIndex));

        lastTextIndex = textIndex;
        lastColorIndex = colorIndex;

        currentTrial = new StroopTrial(
                COLOR_NAMES[textIndex],
                COLOR_VALUES[colorIndex],
                COLOR_NAMES[colorIndex]
        );

        trialStartTimeMs = SystemClock.elapsedRealtime();

        if (listener != null) {
            listener.onNewTrial(currentTrial.wordText, currentTrial.colorInt);
        }
    }

    public static String matchColorWord(String spokenText) {
        if (spokenText == null || spokenText.trim().isEmpty()) {
            return null;
        }
        String[] words = spokenText.toUpperCase(Locale.US).split("[^A-Z]+");
        for (String word : words) {
            for (String color : COLOR_NAMES) {
                if (word.equals(color)) {
                    return color;
                }
            }
        }
        return null;
    }

    public boolean processSpokenText(String spokenText) {
        if (!isListeningActive || currentTrial == null) return false;

        String matchedColor = matchColorWord(spokenText);

        if (matchedColor != null) {
            long rt = SystemClock.elapsedRealtime() - trialStartTimeMs;
            boolean isCorrect = matchedColor.equalsIgnoreCase(currentTrial.colorName);

            totalTrials++;
            if (isCorrect) {
                correctCount++;
                reactionTimes.add(rt);
            }

            if (listener != null) {
                listener.onSpokenResult(matchedColor, isCorrect);
            }

            // Immediately advance to next trial
            nextTrial();
            startListening();
            return true;
        }

        return false;
    }

    public StroopTrial getCurrentTrial() {
        return currentTrial;
    }

    public void setCurrentTrial(StroopTrial trial) {
        this.currentTrial = trial;
        this.trialStartTimeMs = SystemClock.elapsedRealtime();
    }

    public int getTotalTrials() {
        return totalTrials;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public List<Long> getReactionTimes() {
        return reactionTimes;
    }

    public boolean isListeningActive() {
        return isListeningActive;
    }

    public VoiceStroopResult stopTest() {
        isListeningActive = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        float accuracy = totalTrials > 0 ? ((float) correctCount / totalTrials) * 100f : 0f;
        long sumRt = 0;
        for (long rt : reactionTimes) sumRt += rt;
        float avgRt = reactionTimes.isEmpty() ? 0f : (float) sumRt / reactionTimes.size();

        return new VoiceStroopResult(totalTrials, correctCount, accuracy, avgRt);
    }

    // --- SpeechRecognizer RecognitionListener Callbacks ---

    @Override
    public void onResults(Bundle results) {
        if (results != null) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                for (String match : matches) {
                    if (processSpokenText(match)) {
                        return;
                    }
                }
            }
        }
        if (isListeningActive) {
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (partialResults == null) return;
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            for (String match : matches) {
                if (processSpokenText(match)) {
                    break;
                }
            }
        }
    }

    @Override
    public void onError(int error) {
        // Automatically restart speech listening if timeout or no speech detected
        if (isListeningActive) {
            startListening();
        }
    }

    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onEvent(int eventType, Bundle params) {}
}
