package com.example.biomsAfterWirelessConnectedVersion;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** The Sensory Ataxia beep (Part 4), shared by the preview trial beep
 *  (CameraFragment) and the test-end beep (RecordFragment).
 *
 *  No audio file and no new dependency: the tone is a sine wave
 *  generated in code and played through AudioTrack on the music
 *  stream, so it follows the device media volume — the preview popup
 *  tells the user to turn the volume up.
 *
 *  Why not ToneGenerator: the old TONE_PROP_BEEP is defined by AOSP
 *  (3GPP TS 31.111) as "400Hz+1200Hz, 35ms ON" — a 35-millisecond
 *  blip. startTone(type, durationMs) plays at most the tone's
 *  defined duration, so asking for 2000 ms still sounded only
 *  35 ms. The AudioTrack tone below is one continuous high note —
 *  a 2000 Hz sine at full amplitude — for the full
 *  {@link #BEEP_DURATION_MS}. play() is non-blocking; the optional
 *  onFinished callback runs on the main thread once the beep has
 *  fully finished, and the player releases its AudioTrack then. If
 *  track creation fails, onFinished still runs after the same delay,
 *  so a caller waiting on it can never be stuck silent.
 */
public final class BeepPlayer {

    /** Beep length: 2 seconds (the preview trial beep must last at least 2 s). */
    public static final int BEEP_DURATION_MS = 2000;

    /** One continuous high note: 2000 Hz sine, full amplitude. */
    private static final int BEEP_FREQUENCY_HZ = 2000;
    private static final int SAMPLE_RATE_HZ = 44100;

    /** The 2 s of sine samples, built once: 44100 samples/s x 2 s. */
    private static final short[] TONE_SAMPLES = buildToneSamples();

    private static final String TAG = "BeepPlayer";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioTrack audioTrack;
    private Runnable finishRunnable;
    private boolean playing;

    private static short[] buildToneSamples() {
        int count = SAMPLE_RATE_HZ * BEEP_DURATION_MS / 1000;
        short[] samples = new short[count];
        for (int i = 0; i < count; i++) {
            double phase = 2.0 * Math.PI * BEEP_FREQUENCY_HZ * i / SAMPLE_RATE_HZ;
            samples[i] = (short) (Math.sin(phase) * Short.MAX_VALUE);
        }
        return samples;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void play(Runnable onFinished) {
        stop();
        playing = true;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(TONE_SAMPLES.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            int written = audioTrack.write(TONE_SAMPLES, 0, TONE_SAMPLES.length);
            if (written != TONE_SAMPLES.length) {
                Log.e(TAG, "AudioTrack write incomplete: " + written);
            }
            audioTrack.setVolume(1.0f);
            audioTrack.play();
        } catch (Exception e) {
            Log.e(TAG, "AudioTrack beep failed: " + e.getMessage());
            releaseTrack();
        }
        finishRunnable = () -> {
            playing = false;
            releaseTrack();
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
        releaseTrack();
    }

    public void release() {
        stop();
    }

    private void releaseTrack() {
        if (audioTrack != null) {
            try {
                audioTrack.stop();
            } catch (Exception ignored) {
                // Already stopped.
            }
            try {
                audioTrack.release();
            } catch (Exception ignored) {
                // Already released.
            }
            audioTrack = null;
        }
    }
}
