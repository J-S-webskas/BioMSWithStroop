package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
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

/**
 * Voice-driven Stroop test.
 *
 * The patient stands a few meters from the phone (tripod setup) and names
 * the ink color aloud, so this class is tuned for FAR-FIELD recognition,
 * which is much harder than talking into a handset:
 *
 *  - UNPROCESSED audio source (API 30+): the default recognition audio path
 *    applies handset telephony processing (AEC / noise suppression tuned
 *    for a near-field talker) that distorts speech arriving from 2-3 m.
 *    The raw microphone is the better far-field input in a quiet room.
 *  - Prefer-offline recognition: removes network round-trip latency and
 *    the dropped results that flaky clinic Wi-Fi causes.
 *  - One judgment per utterance (latching + blackout): the first accepted
 *    match judges the trial and latches; results arriving within a short
 *    blackout window after a judgment are ignored. Without this, a single
 *    utterance could advance TWO trials — its partial result judging
 *    trial N, then its late final result judging trial N+1 against the
 *    wrong color. Far-field latency made this likely, and it silently
 *    corrupted both accuracy and reaction times.
 *  - Partials are accepted only on an EXACT color word in the top-3
 *    hypotheses (high precision, low latency); final results get the full
 *    treatment: n-best ordered by confidence, then exact -> alias ->
 *    fuzzy matching.
 *  - An RMS watchdog restarts the recognizer if it wedges silently
 *    (no speech callbacks for several seconds while a trial is live).
 */
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
            Color.RED,
            Color.BLUE,
            Color.parseColor("#008000"), // Green
            Color.parseColor("#FFB300")  // Yellow
    };

    /** If no speech-related callback arrives for this long while a trial is
     *  live, assume the recognizer wedged and restart it. */
    private static final long WATCHDOG_SILENCE_MS = 8000L;
    private static final long WATCHDOG_PERIOD_MS = 4000L;

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

    // --- Far-field session management -------------------------------------
    // Timestamp of the last judgment. Results arriving within the blackout
    // window after a judgment are ignored: they belong to the utterance just
    // judged (a stale final trailing its accepted partial), not to the new
    // trial — a genuine new answer cannot physically be spoken and
    // recognized within this window anyway.
    private long lastJudgeMs = 0L;
    private static final long JUDGE_BLACKOUT_MS = 800L;
    // Coalesces multiple posted startListening() calls into one actual start.
    private boolean listenStartQueued = false;
    // Last time any speech callback fired (watchdog input).
    private long lastSpeechActivityMs = 0L;

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (!isListeningActive) return;
            if (SystemClock.elapsedRealtime() - lastSpeechActivityMs > WATCHDOG_SILENCE_MS) {
                Log.d(TAG, "Recognizer silent too long; restarting session.");
                startListening();
            }
            mainHandler.postDelayed(this, WATCHDOG_PERIOD_MS);
        }
    };

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
        // Far-field: keep recognition on-device so weak clinic Wi-Fi cannot
        // add latency or drop results (falls back to online if no offline
        // model is installed).
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        // Far-field: bypass the handset audio processing (AEC / noise
        // suppression tuned for near-field telephony) that distorts a voice
        // arriving from a few meters away.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE,
                    MediaRecorder.AudioSource.UNPROCESSED);
        }
    }

    public void startTest() {
        totalTrials = 0;
        correctCount = 0;
        reactionTimes.clear();
        lastTextIndex = -1;
        lastColorIndex = -1;
        isListeningActive = true;
        lastSpeechActivityMs = SystemClock.elapsedRealtime();

        nextTrial();
        startListening();
        mainHandler.postDelayed(watchdog, WATCHDOG_PERIOD_MS);
    }

    private void startListening() {
        if (!isListeningActive || speechRecognizer == null) return;
        if (listenStartQueued) return;
        listenStartQueued = true;
        mainHandler.post(() -> {
            listenStartQueued = false;
            if (!isListeningActive || speechRecognizer == null) return;
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

    /**
     * Judges one utterance against the current trial — at most once. The
     * latch + blackout guard is what stops a single utterance being judged
     * twice (partial now, stale final a few hundred ms later, against the
     * NEXT trial).
     */
    private void processSpokenMatches(List<String> matches) {
        if (!isListeningActive || currentTrial == null || matches == null) return;
        if (SystemClock.elapsedRealtime() - lastJudgeMs < JUDGE_BLACKOUT_MS) return;

        for (String candidate : matches) {
            String matchedColor = findMatchingColor(candidate);
            if (matchedColor != null) {
                judge(matchedColor);
                break;
            }
        }

        startListening();
    }

    /**
     * Partial results trade precision for speed: accept only an EXACT color
     * word appearing in one of the top-3 hypotheses. Anything weaker waits
     * for the final result's full matching (aliases + fuzzy).
     */
    private void processPartialMatches(List<String> matches) {
        if (!isListeningActive || currentTrial == null || matches == null) return;
        if (SystemClock.elapsedRealtime() - lastJudgeMs < JUDGE_BLACKOUT_MS) return;

        int topN = Math.min(3, matches.size());
        for (int i = 0; i < topN; i++) {
            String text = matches.get(i);
            if (text == null) continue;
            String upper = text.toUpperCase(Locale.US);
            for (String color : COLOR_NAMES) {
                if (upper.contains(color)) {
                    judge(color);
                    startListening();
                    return;
                }
            }
        }
        // No exact hit yet: keep listening, do NOT restart (a restart here
        // would chop the patient's utterance in half).
    }

    /** Records the verdict for the current trial and moves on. Latches so
     *  no second result from the same utterance can judge again. */
    private void judge(String matchedColor) {
        lastJudgeMs = SystemClock.elapsedRealtime();

        long rt = lastJudgeMs - trialStartTimeMs;
        boolean isCorrect = matchedColor.equalsIgnoreCase(currentTrial.colorName);

        totalTrials++;
        if (isCorrect) {
            correctCount++;
            reactionTimes.add(rt);
        }

        if (listener != null) {
            listener.onSpokenResult(matchedColor, isCorrect);
        }

        nextTrial();
    }

    /** Orders n-best hypotheses by confidence score (descending) when the
     *  engine provides scores; otherwise keeps the engine's own order. */
    private static List<String> orderByConfidence(List<String> matches, float[] scores) {
        if (scores == null || scores.length != matches.size()) {
            return matches;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) order.add(i);
        order.sort((a, b) -> Float.compare(scores[b], scores[a]));
        List<String> sorted = new ArrayList<>(matches.size());
        for (int i : order) sorted.add(matches.get(i));
        return sorted;
    }

    public VoiceStroopResult stopTest() {
        isListeningActive = false;
        mainHandler.removeCallbacks(watchdog);
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

    // SpeechRecognizer callbacks

    @Override
    public void onResults(Bundle results) {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            processSpokenMatches(orderByConfidence(matches, scores));
        } else {
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            processPartialMatches(matches);
        }
    }

    @Override
    public void onError(int error) {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
        if (isListeningActive) {
            startListening();
        }
    }

    @Override public void onReadyForSpeech(Bundle params) {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
    }
    @Override public void onBeginningOfSpeech() {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
    }
    @Override public void onRmsChanged(float rmsdB) {
        lastSpeechActivityMs = SystemClock.elapsedRealtime();
    }
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onEvent(int eventType, Bundle params) {}
}
