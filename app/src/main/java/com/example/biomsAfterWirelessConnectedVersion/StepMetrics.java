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
 *   -> stillness-gated window (a >= 1.5 s still gap at each edge
 *      marks where pocketing ends and unpocketing begins; an edge
 *      without a gap falls back to the 1.0 s handling trim)
 *   -> intervals -> metrics.
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
 *
 * 3. Stillness gating exists because pocketing time cannot be
 *    predicted: a fixed trim fails the moment someone pockets slower
 *    than the trim, and MS patients with hand dexterity issues vary
 *    the most. So the window is defined by motion, not by the clock:
 *    the walk runs from the end of the first >= 1.5 s still gap to
 *    the start of the last >= 1.5 s still gap that is followed by
 *    >= 1.0 s of unpocketing motion (the motion requirement rejects
 *    a hold-still just before the Save tap, which is followed only
 *    by the tap spike). The instruction is therefore "stand still a
 *    moment" after pocketing and after stopping, not "pocket within
 *    N seconds". When an edge has no qualifying gap, that edge falls
 *    back to the 1.0 s trim, so a walk recorded without any pause
 *    behaves exactly as before. Mid-walk pauses are never gaps for
 *    this purpose: only the edges are gated, and a > 2 s interval
 *    inside the window is still reported through walkPaused.
 *    The stillness threshold (0.15 m/s^2 std of raw magnitude over a
 *    1.0 s window) is a default: standing sway and sensor noise sit
 *    well below it and walking/handling sit well above it, but it
 *    should be confirmed against a pocket-stand-walk-stand-unpocket
 *    trace from the target phone before being treated as final.
 */
public class StepMetrics {

    /** Minimum separation between two steps (caps cadence at 200 steps/min). */
    private static final long MIN_STEP_SEPARATION_NS = 300_000_000L;
    /** Handling trim: peaks in the first/last second are phone handling, not steps. */
    private static final double TRIM_S = 1.0;
    /** Stillness window: std of raw magnitude over this span defines "still". */
    private static final double STILLNESS_WINDOW_S = 1.0;
    /** A 1.0 s magnitude window counts as still below this std (m/s^2). */
    private static final float STILLNESS_STD_THRESHOLD = 0.15f;
    /** A still run must span at least this long to bound the walk. */
    private static final double STILLNESS_MIN_GAP_S = 1.5;
    /** A gated window shorter than this is nonsense: fall back to the trim. */
    private static final double STILLNESS_MIN_WINDOW_S = 3.0;
    /** Deviation from a gap's median magnitude that counts as motion (m/s^2). */
    private static final float STILLNESS_MOTION_DEV = 0.3f;
    /** Motion after a trailing gap must span at least this long (unpocketing). */
    private static final double STILLNESS_TRAIL_MOTION_S = 1.0;
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
        public boolean stillnessGated; // either edge came from a still gap
        public float gatedWindowS;     // analysis window duration (s)
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

        // 4. Analysis window: stillness gaps gate the edges when present;
        // each edge without a qualifying gap falls back to the 1.0 s trim,
        // so a walk recorded without any pause behaves exactly as before.
        Window window = gateWindow(timestampsNs, magnitudes);
        List<Integer> windowed = new ArrayList<>();
        for (int idx : separated) {
            long t = timestampsNs.get(idx);
            if (t >= window.startNs && t <= window.endNs) windowed.add(idx);
        }
        if (windowed.isEmpty()) return r;

        // 5. Adaptive prominence: keep peaks >= 0.3x the median kept-peak height.
        // The median is taken after the window so handling spikes cannot set the threshold.
        List<Float> heights = new ArrayList<>();
        for (int idx : windowed) heights.add(band.get(idx));
        Collections.sort(heights);
        float medianHeight = heights.get(heights.size() / 2);
        if (medianHeight < MIN_MEDIAN_PEAK_HEIGHT) return r;
        float threshold = (float) (PROMINENCE_FRACTION * medianHeight);
        List<Integer> steps = new ArrayList<>();
        for (int idx : windowed) {
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
        r.stillnessGated = window.gatedLeading || window.gatedTrailing;
        r.gatedWindowS = (float) ((window.endNs - window.startNs) / 1_000_000_000.0);

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
            // the analysis window (gated when gaps exist, else the sample
            // span minus the handling trim).
            double effectiveTimeS = (window.endNs - window.startNs) / 1_000_000_000.0;
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

    /** One still run: [startNs, endNs] with the median raw magnitude. */
    private static class Gap {
        final long startNs;
        final long endNs;
        final float medianMag;

        Gap(long startNs, long endNs, float medianMag) {
            this.startNs = startNs;
            this.endNs = endNs;
            this.medianMag = medianMag;
        }
    }

    /** Analysis window plus which edges came from still gaps. */
    private static class Window {
        final long startNs;
        final long endNs;
        final boolean gatedLeading;
        final boolean gatedTrailing;

        Window(long startNs, long endNs, boolean gatedLeading, boolean gatedTrailing) {
            this.startNs = startNs;
            this.endNs = endNs;
            this.gatedLeading = gatedLeading;
            this.gatedTrailing = gatedTrailing;
        }
    }

    /**
     * Still runs in the raw magnitude series. A 1.0 s window starting at
     * sample i is still when its magnitude std is below
     * STILLNESS_STD_THRESHOLD and it spans at least 0.9 s of samples;
     * a run of still windows spanning at least STILLNESS_MIN_GAP_S is
     * a gap. Magnitude (not a single axis) is rotation-invariant, so
     * the phone shifting in the pocket does not break stillness.
     */
    private static List<Gap> stillGaps(List<Long> timestampsNs, List<Float> magnitudes) {
        List<Gap> gaps = new ArrayList<>();
        final int n = timestampsNs.size();
        long winNs = (long) (STILLNESS_WINDOW_S * 1_000_000_000L);
        boolean[] still = new boolean[n];
        for (int i = 0; i < n; i++) {
            long t0 = timestampsNs.get(i);
            int j = i;
            while (j < n && timestampsNs.get(j) - t0 <= winNs) j++;
            double spanS = (j > i) ? (timestampsNs.get(j - 1) - t0) / 1_000_000_000.0 : 0.0;
            if (j - i < 2 || spanS < 0.9) {
                still[i] = false;
                continue;
            }
            double mean = 0;
            for (int k = i; k < j; k++) mean += magnitudes.get(k);
            mean /= (j - i);
            double var = 0;
            for (int k = i; k < j; k++) {
                double d = magnitudes.get(k) - mean;
                var += d * d;
            }
            var /= (j - i);
            still[i] = Math.sqrt(var) < STILLNESS_STD_THRESHOLD;
        }
        int i = 0;
        while (i < n) {
            if (!still[i]) {
                i++;
                continue;
            }
            int k = i;
            while (k + 1 < n && still[k + 1]) k++;
            long gapStart = timestampsNs.get(i);
            long gapEnd = Math.min(timestampsNs.get(k) + winNs, timestampsNs.get(n - 1));
            if ((gapEnd - gapStart) / 1_000_000_000.0 >= STILLNESS_MIN_GAP_S) {
                List<Float> seg = new ArrayList<>();
                for (int t = i; t <= k; t++) seg.add(magnitudes.get(t));
                Collections.sort(seg);
                gaps.add(new Gap(gapStart, gapEnd, seg.get(seg.size() / 2)));
            }
            i = k + 1;
        }
        return gaps;
    }

    /**
     * Analysis window from still gaps, each edge independent. Leading:
     * the first gap; the walk starts at its end. Trailing: the last gap
     * after whose end motion samples still span at least
     * STILLNESS_TRAIL_MOTION_S (unpocketing); a hold-still just before
     * the Save tap is followed only by the tap spike, so it does not
     * qualify. An edge without a qualifying gap keeps the 1.0 s trim.
     * A gated window shorter than STILLNESS_MIN_WINDOW_S falls back
     * to the trim on both edges rather than trusting a weird gate.
     */
    private static Window gateWindow(List<Long> timestampsNs, List<Float> magnitudes) {
        long startNs = timestampsNs.get(0);
        long endNs = timestampsNs.get(timestampsNs.size() - 1);
        long trimNs = (long) (TRIM_S * 1_000_000_000L);
        long winStart = startNs + trimNs;
        long winEnd = endNs - trimNs;
        boolean gatedLeading = false;
        boolean gatedTrailing = false;
        List<Gap> gaps = stillGaps(timestampsNs, magnitudes);
        if (!gaps.isEmpty()) {
            Gap first = gaps.get(0);
            if (first.endNs < endNs) {
                winStart = first.endNs;
                gatedLeading = true;
            }
            for (int g = gaps.size() - 1; g >= 0; g--) {
                Gap gap = gaps.get(g);
                long lastMotionNs = -1;
                for (int t = 0; t < timestampsNs.size(); t++) {
                    if (timestampsNs.get(t) > gap.endNs
                            && Math.abs(magnitudes.get(t) - gap.medianMag) > STILLNESS_MOTION_DEV) {
                        lastMotionNs = timestampsNs.get(t);
                    }
                }
                if (lastMotionNs >= 0
                        && (lastMotionNs - gap.endNs) / 1_000_000_000.0 >= STILLNESS_TRAIL_MOTION_S
                        && gap.startNs > winStart) {
                    winEnd = gap.startNs;
                    gatedTrailing = true;
                    break;
                }
            }
        }
        if (gatedLeading || gatedTrailing) {
            if ((winEnd - winStart) / 1_000_000_000.0 < STILLNESS_MIN_WINDOW_S
                    || winStart >= winEnd) {
                return new Window(startNs + trimNs, endNs - trimNs, false, false);
            }
        }
        return new Window(winStart, winEnd, gatedLeading, gatedTrailing);
    }
}