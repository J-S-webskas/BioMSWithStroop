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

    /** One judged answer, kept so a report can show the test trial by trial
     *  instead of only the aggregate score. */
    public static class StroopAnswer implements java.io.Serializable {
        public final String wordText;     // the word that was printed
        public final String inkColorName; // the ink color (the correct answer)
        public final String spokenText;   // the color the user was heard saying
        public final boolean correct;
        public final long reactionTimeMs;

        public StroopAnswer(String wordText, String inkColorName, String spokenText,
                            boolean correct, long reactionTimeMs) {
            this.wordText = wordText;
            this.inkColorName = inkColorName;
            this.spokenText = spokenText;
            this.correct = correct;
            this.reactionTimeMs = reactionTimeMs;
        }
    }

    public static class VoiceStroopResult {
        public final int totalTrials;
        public final int correctCount;
        public final float accuracyPercent;
        public final float avgReactionTimeMs;
        /** Every judged answer, in order (empty if none were recorded). */
        public final List<StroopAnswer> answers;

        public VoiceStroopResult(int totalTrials, int correctCount, float accuracyPercent, float avgReactionTimeMs) {
            this(totalTrials, correctCount, accuracyPercent, avgReactionTimeMs, new ArrayList<>());
        }

        public VoiceStroopResult(int totalTrials, int correctCount, float accuracyPercent,
                                 float avgReactionTimeMs, List<StroopAnswer> answers) {
            this.totalTrials = totalTrials;
            this.correctCount = correctCount;
            this.accuracyPercent = accuracyPercent;
            this.avgReactionTimeMs = avgReactionTimeMs;
            this.answers = answers != null ? answers : new ArrayList<>();
        }
    }

    private static final String TAG = "StroopVoiceManager";
    private static final String[] COLOR_NAMES = {"RED", "BLUE", "GREEN", "YELLOW"};
    private static final int[] COLOR_VALUES = {
            Color.RED,
            Color.BLUE,
            Color.parseColor("#008000"), // Green
            Color.parseColor("#FFB300")  // Yellow
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
    private final List<StroopAnswer> answers = new ArrayList<>();
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
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 300L);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 300L);
    }

    public void startTest() {
        totalTrials = 0;
        correctCount = 0;
        reactionTimes.clear();
        answers.clear();
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

        // Pick a color index that is NOT EQUAL to textIndex (word != color) and NOT EQUAL to lastColorIndex (color changes each time)
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

    public static String findMatchingColor(String spokenText) {
        if (spokenText == null || spokenText.trim().isEmpty()) {
            return null;
        }

        String upperSpoken = spokenText.toUpperCase(Locale.US);

        for (String color : COLOR_NAMES) {
            if (upperSpoken.contains(color)) {
                return color;
            }
        }

        String[] words = upperSpoken.split("[^A-Z]+");

        for (String word : words) {
            if (word.isEmpty()) continue;

            for (String color : COLOR_NAMES) {
                if (word.equals(color)) {
                    return color;
                }
            }

            // Check aliases/homophones
            if (isRedAlias(word)) return "RED";
            if (isBlueAlias(word)) return "BLUE";
            if (isGreenAlias(word)) return "GREEN";
            if (isYellowAlias(word)) return "YELLOW";

            // Check fuzzy Levenshtein distance for close matches
            for (String color : COLOR_NAMES) {
                int maxAllowedDistance = (color.length() <= 4) ? 1 : 2;
                if (Math.abs(word.length() - color.length()) <= maxAllowedDistance) {
                    if (getLevenshteinDistance(word, color) <= maxAllowedDistance) {
                        return color;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isRedAlias(String word) {
        return word.equals("READ") || word.equals("RAD") || word.equals("LED") ||
                word.equals("REDD") || word.equals("RADD");
    }

    private static boolean isBlueAlias(String word) {
        return word.equals("BLEW") || word.equals("BLU") || word.equals("BLOO") ||
                word.equals("BLOW");
    }

    private static boolean isGreenAlias(String word) {
        return word.equals("GREENE") || word.equals("GRIN") || word.equals("GRAIN") ||
                word.equals("GREAN") || word.equals("GRAN");
    }

    private static boolean isYellowAlias(String word) {
        return word.equals("YELLO") || word.equals("YALLOW") || word.equals("YELLA");
    }

    public static int getLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[len1][len2];
    }

    private void processSpokenText(String spokenText) {
        if (spokenText != null) {
            List<String> list = new ArrayList<>();
            list.add(spokenText);
            processSpokenMatches(list);
        }
    }

    private void processSpokenMatches(List<String> matches) {
        if (!isListeningActive || currentTrial == null || matches == null) return;

        String matchedColor = null;
        for (String candidate : matches) {
            matchedColor = findMatchingColor(candidate);
            if (matchedColor != null) {
                break;
            }
        }

        if (matchedColor != null) {
            long rt = SystemClock.elapsedRealtime() - trialStartTimeMs;
            boolean isCorrect = matchedColor.equalsIgnoreCase(currentTrial.colorName);

            totalTrials++;
            if (isCorrect) {
                correctCount++;
                reactionTimes.add(rt);
            }
            answers.add(new StroopAnswer(currentTrial.wordText, currentTrial.colorName,
                    matchedColor, isCorrect, rt));

            if (listener != null) {
                listener.onSpokenResult(matchedColor, isCorrect);
            }

            // go to next trial
            nextTrial();
        }

        // keep speech recognizer listening continuously
        startListening();
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

        return new VoiceStroopResult(totalTrials, correctCount, accuracy, avgRt,
                new ArrayList<>(answers));
    }

    // Speechrecognizer callbacks

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            processSpokenMatches(matches);
        } else {
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            processSpokenMatches(matches);
        }
    }

    @Override
    public void onError(int error) {
        // automatically restart speech listening if no speech detected
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
