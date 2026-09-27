package com.example.biomsAfterWirelessConnectedVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * 2nd-Order Low-Pass Butterworth Filter for biomechanical signal smoothing.
 * Filters out high-frequency pose landmarker noise prior to derivative or sway calculations.
 */
public class ButterworthFilter {

    private final float b0, b1, b2, a1, a2;

    /**
     * Constructs a 2nd order low-pass Butterworth filter.
     * @param cutoffFreq Cutoff frequency in Hz (e.g., 6.0 Hz for human gait/posture)
     * @param sampleRate Sampling rate in Hz (e.g., 30.0 Hz frame rate)
     */
    public ButterworthFilter(float cutoffFreq, float sampleRate) {
        if (sampleRate <= 0) sampleRate = 30.0f;
        if (cutoffFreq <= 0 || cutoffFreq >= sampleRate / 2.0f) {
            cutoffFreq = Math.min(6.0f, (sampleRate / 2.0f) * 0.8f);
        }

        float omega = (float) Math.tan(Math.PI * cutoffFreq / sampleRate);
        float c = omega * omega;
        float sqrt2 = (float) Math.sqrt(2.0);
        float norm = 1.0f / (1.0f + sqrt2 * omega + c);

        this.b0 = c * norm;
        this.b1 = 2.0f * this.b0;
        this.b2 = this.b0;
        this.a1 = 2.0f * (c - 1.0f) * norm;
        this.a2 = (1.0f - sqrt2 * omega + c) * norm;
    }

    /**
     * Filters a single 1D time series (e.g., joint angles over time).
     */
    public List<Float> filter(List<Float> input) {
        if (input == null || input.size() < 3) {
            return input != null ? new ArrayList<>(input) : new ArrayList<>();
        }

        List<Float> output = new ArrayList<>(input.size());

        // Forward pass
        float x1 = input.get(0), x2 = input.get(0);
        float y1 = input.get(0), y2 = input.get(0);

        for (int i = 0; i < input.size(); i++) {
            float x0 = input.get(i);
            float y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            output.add(y0);

            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }

        return output;
    }
}
