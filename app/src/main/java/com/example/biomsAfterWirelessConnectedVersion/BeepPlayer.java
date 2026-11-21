package com.example.biomsAfterWirelessConnectedVersion;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** The Sensory Ataxia beep (Part 4), shared by the preview trial beep
 *  (CameraFragment) and the test-end beep (RecordFragment).
 *
 *  No audio file and no new dependency: the tone comes from
 *  ToneGenerator on the music stream, so it follows the device media
 *  volume — the preview popup tells the user to turn the volume up.
 *  One continuous beep, {@link #BEEP_DURATION_MS} long. play() is
 *  non-blocking; the optional onFinished callback runs on the main
 *  thread once the beep has fully finished, and the player releases
 *  its ToneGenerator then. If tone creation fails, onFinished still
 *  runs after the same delay, so a caller waiting on it can never be
 *  stuck silent.
 */
public final class BeepPlayer {

    /** Beep length: 2 seconds (the preview trial beep must last at least 2 s). */
    public static final int BEEP_DURATION_MS = 2000;

    private static final String TAG = "BeepPlayer";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ToneGenerator toneGenerator;
    private Runnable finishRunnable;
    private boolean playing;

    public boolean isPlaying() {
        return playing;
    }

    public void play(Runnable onFinished) {
        stop();
        playing = true;
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,
                    ToneGenerator.MAX_VOLUME);
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS);
        } catch (Exception e) {
            Log.e(TAG, "ToneGenerator failed: " + e.getMessage());
        }
        finishRunnable = () -> {
            playing = false;
            releaseGenerator();
            if (onFinished != null) onFinished.run();
        };
        handler.postDelayed(finishRunnable, BEEP_DURATION_MS);
    }

    public void stop() {
        if (finishRunnable != null) {
            handler.removeCallbacks(finishRunnable);
            finishRunnable = null;
        }
        playing = false;
        releaseGenerator();
    }

    public void release() {
        stop();
    }

    private void releaseGenerator() {
        if (toneGenerator != null) {
            try {
                toneGenerator.stopTone();
            } catch (Exception ignored) {
                // Already stopped.
            }
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
