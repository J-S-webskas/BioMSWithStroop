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

    /**
     * Replaces samples that deviate from the rolling median by more than (same units as the series, meters for COM) with that median.
     *
     * A pose glitch is typically a single frame that jumps several centimeters and snaps back; genuine sway cannot move that far within
     * one frame interval, so a fixed physical threshold is the honest test. The first/last windowRadius samples are compared against a
     * shrunk window at the edges.
     *
     * Imagine there is a window of size windowRadius * 2 + 1 (+1 is inclusive of the current frame it centers on, which is i)
     * For every frame, take the outer windowRadius frames and calculate the median. WindowRadius is even (2), so the
     * window frame covers an odd number of values (giving a clear median). Compare the current frame's value in this window against the median
     * (resistant to extreme values (outliers to take out). Do NOT take mean). If the difference is greater than a threshold (ex. 5cm),
     * replace that value with the median. Then move on to the next i, and recalculate array.
     *
     * 3cm will pass under the 5cm threshold, avoiding false positives (taking out real motion). 5cm per ~1/30s means the person moves
     * 1.5m/s, which is the speed of falling, and not swaying while standing. Remove anything above. The 6Hz Butterworth filter will then smooth the signals
     * 0.3cm per one frame is around 0.9m/s, which is still plausible. But the algorithm is not trying to catch everything that is unusual
     * It first removes everything that is provably not sway. 3cm jump passes by design. COM cannot move 5cm in quiet standing.
     *
     * This is not used for walking gait.
     */
    public static List<Float> despike(List<Float> input, int windowRadius, float threshold) {
        int n = input.size();
        List<Float> out = new ArrayList<>(n);
        if (n == 0) return out;
        float[] window = new float[2 * windowRadius + 1];
        for (int i = 0; i < n; i++) {
            int lo = Math.max(0, i - windowRadius);
            int hi = Math.min(n - 1, i + windowRadius);
            int m = hi - lo + 1; // length of the values in window
            for (int j = 0; j < m; j++) window[j] = input.get(lo + j);
            float[] sorted = Arrays.copyOf(window, m);
            Arrays.sort(sorted);
            float median = sorted[m / 2];
            float v = input.get(i);
            out.add(Math.abs(v - median) > threshold ? median : v);
        }
        return out;
    }

    /** Result of resampling one trial onto a uniform time grid. */
    public static class UniformSeries {
        public List<Float> x = new ArrayList<>();      // uniformly sampled X (m)
        public List<Float> z = new ArrayList<>();      // uniformly sampled Z (m)
        public float sampleRateHz = 0f;                // grid rate (Hz)
        public float durationS = 0f;                   // (n-1)/rate - grid span
        public float validFraction = 0f;               // real samples / grid points
        public float meanRawRateHz = 0f;               // real samples / real duration
        public int interpolatedCount = 0;              // grid points filled across a dropout gap
    }

    /**
     * Linearly interpolates the timestamped samples onto a uniform grid at targetRateHz, from the first COM sample's timestamp to the last.
     * Gaps (dropped poses) are bridged by the interpolation - acceptable for the sub-second gaps phone recording produces, and the returned
     * validFraction records how much of the trial was real data so downstream analysis can exclude over-interpolated trials.
     *
     * Duplicate or out-of-order timestamps are handled defensively: the series is treated as sorted as given, and zero-length intervals never
     * divide by zero.
     *
     * Butterworth filtering expects uniform time rates. But frames with no post detected are dropped entirely, and frames
     * arrive at slightly different times due to lighting and device load, so consecutive timestamps have varying gaps (30ms, 33 ms, 34 ms, 37ms, etc)
     * The butterworth filter expects that the frame timestamps are uniform (designed for the ~30 frames/s). Sway velocity is change of position over chang ein time
     * If the change in time is different, then the same velocity in real life will produce different velocities in the calculation
     *
     * For duplicate measurements, take the first one. It's linearly interpolating COM to where it is supposed to be on the timestamp.
     *
     */
    public static UniformSeries resampleUniform(List<COMSample> samples, float targetRateHz) {
        UniformSeries r = new UniformSeries();
        int n = samples.size();
        if (n == 0 || targetRateHz <= 0) return r;

        long t0 = samples.get(0).timestampMs;
        long t1 = samples.get(n - 1).timestampMs;
        // Time stamp can't be neg (30s before 29s), which happened
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
                // Without the neg guard, the diff becomes negative, meaning medians could be low, filtering out ordinary values. This ensures te median is pos.
                diffs[i] = Math.max(0, samples.get(i + 1).timestampMs - samples.get(i).timestampMs);
            }
            Arrays.sort(diffs);
            medianIntervalMs = diffs[diffs.length / 2];
            // Since median cannot be 0
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
            // If there are duplicates, can't interpolate two at the same time. Just take the first one. If divide by the difference, it would be divide by 0
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
