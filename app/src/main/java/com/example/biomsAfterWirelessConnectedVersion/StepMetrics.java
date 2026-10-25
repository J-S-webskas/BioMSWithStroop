package com.example.biomsAfterWirelessConnectedVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Step metrics for the timed walk (walking speed and distance test).
 *
 * Pure Java, no Android imports, so the detector can be tested offline
 * with canned sample lists exactly like the sway math. The phone's
 * accelerometer is the only sensor: the timed walk has no camera
 * running, and a static tripod at ~2 m frames only ~2.3 m of a
 * 10-20 m walk, so camera step counting cannot cover the test.
 *
 * Pipeline (all timestamps are SensorEvent monotonic nanos, never
 * sample counts, because SENSOR_DELAY_GAME is a hint, not a rate):
 *   magnitude -> gravity removal (first-order low-pass, alpha 0.1)
 *   -> band-pass 0.8-3 Hz (two zero-phase Butterworth low-passes,
 *      the same filter class as the sway pipeline)
 *   -> peak pick (local maxima, tallest-first with 300 ms separation,
 *      adaptive prominence 0.3x the median kept-peak height)
 *   -> handling trim (first/last 1.0 s are pocketing and tapping,
 *      not steps) -> intervals -> metrics.
 *
 * Two deliberate deviations from the review spec, both so the spec's
 * own acceptance benches can pass:
 *   1. The activity hands over raw (timestamp, magnitude) samples and
 *       detection lives here, not in the activity. Otherwise the
 *       detector itself could not be unit-tested without a phone.
 *   2. Cadence is 60 / mean step interval, not stepCount / timer.
 *       The timer always includes ~2 s of standing still while the
 *       phone is pocketed and unpocketed, so count / timer understates
 *       a metronome walk by ~10% and would fail the +/-2 steps/min
 *       bench. The interval mean is immune to that standing time.
 *       Step count still excludes the handling seconds (handling
 *       peaks are not steps); only the first/last walking intervals
 *       are kept in the count, per the spec's "untrimmed" note.
 *
 * Display gates (applied by callers, not here): CV is reportable only
 * with stepTimeN >= MIN_INTERVALS_FOR_CV; zero detected peaks save as
 * unset fields (never zero), and stepCount > 0 is the has-data marker.
 */
public class StepMetrics {

    /** Minimum separation between two steps (caps cadence at 200 steps/min). */
    private static final long MIN_STEP_SEPARATION_NS = 300_000_000L;
    /** Handling trim: peaks in the first/last second are phone handling, not steps. */
    private static final double TRIM_S = 1.0;
    /** Adaptive prominence: a peak counts when it reaches this fraction of the median peak height. */
    private static final double PROMINENCE_FRACTION = 0.3;
    /** Band-pass for stepping: step frequency in MS is ~1.5-2.2 Hz. */
    private static final float BAND_LOW_HZ = 0.8f;
    private static final float BAND_HIGH_HZ = 3.0f;
    /** Gravity estimate: first-order low-pass alpha on the magnitude. */
    private static final float GRAVITY_ALPHA = 0.1f;
    /** Intervals longer than this are pauses, not steps. */
    private static final double MAX_STEP_INTERVAL_S = 2.0;
    /** Minimum samples for any measurement (also guards degenerate recordings). */
    private static final int MIN_SAMPLES = 30;
    /** Minimum sample rate for filtering to be meaningful. */
    private static final double MIN_RATE_HZ = 5.0;
    /** CV is only reportable with at least this many intervals (display gate). */
    public static final int MIN_INTERVALS_FOR_CV = 10;

    private static final float MIN_MEDIAN_PEAK_HEIGHT = 0.05f;

    public static class Result {
        public boolean hasData;
        public int stepCount;
        public float cadence;          // steps/min
        public float meanStepLengthM;  // m
        public float stepTimeMeanS;    // mean of the CV intervals (s)
        public float stepTimeSDS;      // sample SD of the CV intervals (s)
        public float stepTimeCVPct;    // %
        public int stepTimeN;          // intervals entering the CV
        public boolean walkPaused;     // a >2 s interval was excluded
    }

    public static Result compute(List<Long> timestampsNs, List<Float> magnitudes,
                                 double walkTimeS, double distanceM) {
        Result r = new Result();
        if (timestampsNs == null || magnitudes == null
                || timestampsNs.size() != magnitudes.size()
                || timestampsNs.size() < MIN_SAMPLES
                || walkTimeS <= 0 || distanceM <= 0) {
            return r;
        }
        final int n = timestampsNs.size();

        // Sample rate from the median interval (never assume a fixed rate).
        List<Long> dts = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            long dt = timestampsNs.get(i) - timestampsNs.get(i - 1);
            if (dt > 0) dts.add(dt);
        }
        if (dts.isEmpty()) return r;
        Collections.sort(dts);
        long medianDtNs = dts.get(dts.size() / 2);
        if (medianDtNs <= 0) return r;
        float rateHz = (float) (1_000_000_000.0 / medianDtNs);
        if (rateHz < MIN_RATE_HZ) return r;

        // 1. Gravity removal: magnitude minus its slow low-pass.
        List<Float> dyn = new ArrayList<>(n);
        float g = magnitudes.get(0);
        for (int i = 0; i < n; i++) {
            float m = magnitudes.get(i);
            g = g + GRAVITY_ALPHA * (m - g);
            dyn.add(m - g);
        }

        // 2. Band-pass 0.8-3 Hz via two zero-phase low-passes (same filter as sway).
        List<Float> lpHigh = new ButterworthFilter(BAND_HIGH_HZ, rateHz).filterZeroPhase(dyn);
        List<Float> lpLow = new ButterworthFilter(BAND_LOW_HZ, rateHz).filterZeroPhase(dyn);
        List<Float> band = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            band.add(lpHigh.get(i) - lpLow.get(i));
        }

        // 3. Candidate peaks: local maxima, tallest-first with 300 ms separation.
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < n - 1; i++) {
            if (band.get(i) > band.get(i - 1) && band.get(i) >= band.get(i + 1) && band.get(i) > 0) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) return r;
        Collections.sort(candidates, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Float.compare(band.get(b), band.get(a));
            }
        });
        List<Integer> separated = new ArrayList<>();
        for (int idx : candidates) {
            boolean ok = true;
            for (int kept : separated) {
                if (Math.abs(timestampsNs.get(idx) - timestampsNs.get(kept)) < MIN_STEP_SEPARATION_NS) {
                    ok = false;
                    break;
                }
            }
            if (ok) separated.add(idx);
        }
        if (separated.isEmpty()) return r;

        // 4. Handling trim: drop peaks in the first/last second (pocketing, tapping).
        long startNs = timestampsNs.get(0);
        long endNs = timestampsNs.get(n - 1);
        long trimNs = (long) (TRIM_S * 1_000_000_000L);
        List<Integer> trimmed = new ArrayList<>();
        for (int idx : separated) {
            long t = timestampsNs.get(idx);
            if (t >= startNs + trimNs && t <= endNs - trimNs) trimmed.add(idx);
        }
        if (trimmed.isEmpty()) return r;

        // 5. Adaptive prominence: keep peaks >= 0.3x the median kept-peak height.
        // The median is taken after the trim so handling spikes cannot set the threshold.
        List<Float> heights = new ArrayList<>();
        for (int idx : trimmed) heights.add(band.get(idx));
        Collections.sort(heights);
        float medianHeight = heights.get(heights.size() / 2);
        if (medianHeight < MIN_MEDIAN_PEAK_HEIGHT) return r;
        float threshold = (float) (PROMINENCE_FRACTION * medianHeight);
        List<Integer> steps = new ArrayList<>();
        for (int idx : trimmed) {
            if (band.get(idx) >= threshold) steps.add(idx);
        }
        if (steps.isEmpty()) return r;
        // Steps must be chronological from here on.
        Collections.sort(steps, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Long.compare(timestampsNs.get(a), timestampsNs.get(b));
            }
        });

        r.hasData = true;
        r.stepCount = steps.size();
        r.meanStepLengthM = (float) (distanceM / r.stepCount);

        // 6. Intervals: pauses (>2 s) are stops, not steps, and are excluded
        // everywhere; the first/last surviving intervals (acceleration and
        // deceleration) are additionally dropped from the CV set only.
        List<Double> allIntervals = new ArrayList<>();
        for (int i = 1; i < steps.size(); i++) {
            allIntervals.add((timestampsNs.get(steps.get(i)) - timestampsNs.get(steps.get(i - 1))) / 1_000_000_000.0);
        }
        List<Double> walking = new ArrayList<>();
        for (double iv : allIntervals) {
            if (iv > MAX_STEP_INTERVAL_S) {
                r.walkPaused = true;
            } else {
                walking.add(iv);
            }
        }
        if (!walking.isEmpty()) {
            // Cadence from the mean step interval: immune to the standing
            // time inside the timer window (see class doc note 2).
            double sum = 0;
            for (double iv : walking) sum += iv;
            double meanInterval = sum / walking.size();
            if (meanInterval > 0) {
                r.cadence = (float) (60.0 / meanInterval);
            }
        }
        if (r.cadence == 0) {
            // Degenerate fallback (a single step has no interval): count over
            // the analysis window, i.e. the timer minus the handling trim.
            double effectiveTimeS = walkTimeS - 2.0 * TRIM_S;
            if (effectiveTimeS <= 0) effectiveTimeS = walkTimeS;
            r.cadence = (float) (r.stepCount / effectiveTimeS * 60.0);
        }

        List<Double> cvSet;
        if (walking.size() > 2) {
            cvSet = new ArrayList<>(walking.subList(1, walking.size() - 1));
        } else {
            cvSet = new ArrayList<>();
        }
        r.stepTimeN = cvSet.size();
        if (cvSet.size() >= 1) {
            double sum = 0;
            for (double iv : cvSet) sum += iv;
            r.stepTimeMeanS = (float) (sum / cvSet.size());
        }
        if (cvSet.size() >= 2) {
            double mean = r.stepTimeMeanS;
            double ss = 0;
            for (double iv : cvSet) {
                double d = iv - mean;
                ss += d * d;
            }
            r.stepTimeSDS = (float) Math.sqrt(ss / (cvSet.size() - 1));
            if (mean > 0) {
                r.stepTimeCVPct = (float) (r.stepTimeSDS / mean * 100.0);
            }
        }
        return r;
    }
}