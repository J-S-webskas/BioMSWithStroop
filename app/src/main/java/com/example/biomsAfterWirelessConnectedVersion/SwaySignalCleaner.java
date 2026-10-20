package com.example.biomsAfterWirelessConnectedVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Signal cleaning for the COM trajectory, applied in BalanceCalculator in
 * this order:
 *
 *   1. despike()         – remove one-frame pose-estimation glitches
 *   2. resampleUniform() – interpolate onto an exactly uniform time grid
 *   3. zero-phase low-pass (ButterworthFilter.filterZeroPhase)
 *
 * The raw series is NOT uniformly sampled: frames with no detected pose are
 * dropped, and the camera frame rate floats with lighting and device load.
 * Filtering or differentiating such a series as if it were uniform warps
 * time and corrupts every rate-dependent metric (velocity above all).
 */
public class SwaySignalCleaner {

    private SwaySignalCleaner() { /* static helpers only */ }

    // ------------------------------------------------------------------
    // 1. Despiking (Hampel-style, rolling median)
    // ------------------------------------------------------------------

    /**
     * Replaces samples that deviate from the rolling median by more than
     * {@code threshold} (same units as the series, meters for COM) with that
     * median. A pose glitch is typically a single frame that jumps several
     * centimeters and snaps back; genuine sway cannot move that far within
     * one frame interval, so a fixed physical threshold is the honest test.
     * The first/last {@code windowRadius} samples are compared against a
     * shrunk window at the edges.
     */
    public static List<Float> despike(List<Float> input, int windowRadius, float threshold) {
        int n = input.size();
        List<Float> out = new ArrayList<>(n);
        if (n == 0) return out;
        float[] window = new float[2 * windowRadius + 1];
        for (int i = 0; i < n; i++) {
            int lo = Math.max(0, i - windowRadius);
            int hi = Math.min(n - 1, i + windowRadius);
            int m = hi - lo + 1;
            for (int j = 0; j < m; j++) window[j] = input.get(lo + j);
            float[] sorted = Arrays.copyOf(window, m);
            Arrays.sort(sorted);
            float median = sorted[m / 2];
            float v = input.get(i);
            out.add(Math.abs(v - median) > threshold ? median : v);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // 2. Resampling onto a uniform grid
    // ------------------------------------------------------------------

    /** Result of resampling one trial onto a uniform time grid. */
    public static class UniformSeries {
        public List<Float> x = new ArrayList<>();      // uniformly sampled X (m)
        public List<Float> z = new ArrayList<>();      // uniformly sampled Z (m)
        public float sampleRateHz = 0f;                // grid rate (Hz)
        public float durationS = 0f;                   // (n-1)/rate — grid span
        public float validFraction = 0f;               // real samples / grid points
        public float meanRawRateHz = 0f;               // real samples / real duration
        public int interpolatedCount = 0;              // grid points filled across a dropout gap
    }

    /**
     * Linearly interpolates the timestamped samples onto a uniform grid at
     * {@code targetRateHz}, from the first sample's timestamp to the last.
     * Gaps (dropped poses) are bridged by the interpolation — acceptable for
     * the sub-second gaps phone recording produces, and the returned
     * {@code validFraction} records how much of the trial was real data so
     * downstream analysis can exclude over-interpolated trials.
     *
     * Duplicate or out-of-order timestamps are handled defensively: the
     * series is treated as sorted as given, and zero-length intervals never
     * divide by zero.
     */
    public static UniformSeries resampleUniform(List<COMSample> samples, float targetRateHz) {
        UniformSeries r = new UniformSeries();
        int n = samples.size();
        if (n == 0 || targetRateHz <= 0) return r;

        long t0 = samples.get(0).timestampMs;
        long t1 = samples.get(n - 1).timestampMs;
        double spanMs = Math.max(0, t1 - t0);
        double stepMs = 1000.0 / targetRateHz;
        int gridN = (int) Math.floor(spanMs / stepMs) + 1;
        if (gridN < 1) gridN = 1;

        // Median raw interval: the yardstick for "this grid point was filled
        // in across a dropout gap" vs ordinary between-frame interpolation.
        double medianIntervalMs = stepMs;
        if (n > 2) {
            double[] diffs = new double[n - 1];
            for (int i = 0; i < n - 1; i++) {
                diffs[i] = Math.max(0, samples.get(i + 1).timestampMs - samples.get(i).timestampMs);
            }
            Arrays.sort(diffs);
            medianIntervalMs = diffs[diffs.length / 2];
            if (medianIntervalMs <= 0) medianIntervalMs = stepMs;
        }

        int seg = 0;
        for (int g = 0; g < gridN; g++) {
            double tg = t0 + g * stepMs;
            while (seg < n - 2 && samples.get(seg + 1).timestampMs <= tg) seg++;
            COMSample a = samples.get(seg);
            COMSample b = samples.get(Math.min(seg + 1, n - 1));
            double dt = b.timestampMs - a.timestampMs;
            if (dt > 1.5 * medianIntervalMs) r.interpolatedCount++;
            double frac = dt > 0 ? (tg - a.timestampMs) / dt : 0.0;
            if (frac < 0) frac = 0;
            if (frac > 1) frac = 1;
            r.x.add((float) (a.x + frac * (b.x - a.x)));
            r.z.add((float) (a.z + frac * (b.z - a.z)));
        }

        r.sampleRateHz = targetRateHz;
        r.durationS = (float) ((gridN - 1) / (double) targetRateHz);
        r.validFraction = gridN > 0 ? Math.min(1f, n / (float) gridN) : 0f;
        r.meanRawRateHz = spanMs > 0 ? (float) (n / (spanMs / 1000.0)) : targetRateHz;
        return r;
    }
}
