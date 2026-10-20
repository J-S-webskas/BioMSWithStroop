package com.example.biomsAfterWirelessConnectedVersion;

import java.util.ArrayList;
import java.util.List;

public class BalanceCalculator {

    /** Low-pass cutoff (Hz). Real COM sway power sits below ~2 Hz; the
     *  force-plate literature low-passes at 5–10 Hz. */
    private static final float LOWPASS_CUTOFF_HZ = 6.0f;

    /** Every trial is resampled onto a uniform grid at this rate before
     *  filtering, so the filter's design rate is always exactly true. */
    private static final float GRID_RATE_HZ = 30.0f;

    /** Despike: a sample >5 cm from its 5-sample rolling median is a pose
     *  glitch, not physiology (sway cannot move that far in one frame). */
    private static final int DESPIKE_RADIUS = 2;
    private static final float DESPIKE_THRESHOLD_M = 0.05f;

    /** Minimum evidence for a measurement: at least 10 real frames spanning
     *  at least 1 second of uniform grid. Below that the trial is flagged
     *  invalid instead of reporting zeros that look like perfect balance. */
    private static final int MIN_REAL_SAMPLES = 10;
    private static final int MIN_GRID_POINTS = 30;

    /**
     * Full sway pipeline:
     *   raw samples -> despike -> resample to uniform 30 Hz grid
     *   -> zero-phase 6 Hz Butterworth -> metrics.
     *
     * The metrics are computed on the uniformly sampled, zero-phase filtered
     * series, so path length and velocity are no longer inflated by camera
     * jitter or warped by dropped frames.
     */
    public SwayMetrics computeSwayMetrics(List<COMSample> samples) {
        SwayMetrics m = new SwayMetrics();
        if (samples == null || samples.size() < MIN_REAL_SAMPLES) {
            m.valid = false;
            m.sampleCount = samples == null ? 0 : samples.size();
            return m;
        }

        // 1. Despike each axis on the raw (still timestamped) series.
        List<Float> rawX = new ArrayList<>(samples.size());
        List<Float> rawZ = new ArrayList<>(samples.size());
        for (COMSample s : samples) {
            rawX.add((float) s.x);
            rawZ.add((float) s.z);
        }
        List<Float> cleanX = SwaySignalCleaner.despike(rawX, DESPIKE_RADIUS, DESPIKE_THRESHOLD_M);
        List<Float> cleanZ = SwaySignalCleaner.despike(rawZ, DESPIKE_RADIUS, DESPIKE_THRESHOLD_M);
        List<COMSample> despiked = new ArrayList<>(samples.size());
        for (int i = 0; i < samples.size(); i++) {
            despiked.add(new COMSample(cleanX.get(i), cleanZ.get(i), samples.get(i).timestampMs));
        }

        // 2. Resample onto a uniform grid using the frame timestamps.
        SwaySignalCleaner.UniformSeries u = SwaySignalCleaner.resampleUniform(despiked, GRID_RATE_HZ);
        if (u.x.size() < MIN_GRID_POINTS) {
            m.valid = false;
            m.sampleCount = samples.size();
            return m;
        }

        // 3. Zero-phase low-pass at the grid's exact rate (no phase lag,
        //    doubled-order jitter rejection — the posturography standard).
        ButterworthFilter filter = new ButterworthFilter(LOWPASS_CUTOFF_HZ, GRID_RATE_HZ);
        List<Float> fx = filter.filterZeroPhase(u.x);
        List<Float> fz = filter.filterZeroPhase(u.z);

        List<Point2D> filtered = new ArrayList<>(fx.size());
        for (int i = 0; i < fx.size(); i++) {
            filtered.add(new Point2D(fx.get(i), fz.get(i)));
        }

        // 4. Metrics on the cleaned series.
        int n = filtered.size();
        m.valid = true;
        m.sampleCount = n;
        m.frameRate = u.meanRawRateHz;
        m.validFraction = u.validFraction;
        m.effectiveDurationS = u.durationS;
        m.swayAreaMm2 = ellipseArea(filtered);

        double path = 0.0;
        for (int i = 1; i < n; i++) {
            double dx = filtered.get(i).getX() - filtered.get(i - 1).getX();
            double dz = filtered.get(i).getZ() - filtered.get(i - 1).getZ();
            path += Math.sqrt(dx * dx + dz * dz);
        }
        m.pathLengthMm = (float) (path * 1000.0);
        if (u.durationS > 0) {
            m.meanVelocityMmS = (float) (m.pathLengthMm / u.durationS);
        }

        double meanX = 0, meanZ = 0;
        for (Point2D p : filtered) {
            meanX += p.getX();
            meanZ += p.getZ();
            m.trajX.add((float) p.getX());
            m.trajZ.add((float) p.getZ());
        }
        meanX /= n;
        meanZ /= n;
        double sx = 0, sz = 0;
        for (Point2D p : filtered) {
            double dx = p.getX() - meanX;
            double dz = p.getZ() - meanZ;
            sx += dx * dx;
            sz += dz * dz;
        }
        m.rmsMLMm = (float) (Math.sqrt(sx / n) * 1000.0);  // x: mediolateral
        m.rmsAPMm = (float) (Math.sqrt(sz / n) * 1000.0);  // z: anteroposterior

        return m;
    }

    // 95% confidence ellipse area from covariance eigenvalues (mm^2).
    private float ellipseArea(List<Point2D> filteredCOM) {
        Point2D point = getMean(filteredCOM);
        float meanX = (float) point.getX();
        float meanZ = (float) point.getZ();

        float sumX_diff_sq = 0, sumZ_diff_sq = 0, sumXZ_diff = 0;
        int n = filteredCOM.size();
        for (Point2D p : filteredCOM) {
            float diffX = (float) p.getX() - meanX;
            float diffZ = (float) p.getZ() - meanZ;
            sumX_diff_sq += diffX * diffX;      // Σ(x - x̄)²
            sumZ_diff_sq += diffZ * diffZ;      // Σ(z - z̄)²
            sumXZ_diff += diffX * diffZ;        // Σ(x - x̄)(z - z̄)
        }
        float variance_x = sumX_diff_sq / (n - 1);
        float variance_z = sumZ_diff_sq / (n - 1);
        float covariance_xz = sumXZ_diff / (n - 1);

        float[][] covarianceMatrix = {
                {variance_x, covariance_xz},
                {covariance_xz, variance_z}
        };

        float[] eigenvalues = calculateEigenvalues2x2(covarianceMatrix);
        float lambda1 = eigenvalues[0];
        float lambda2 = eigenvalues[1];

        float chiSquare = 5.991f;
        float area = (float) (Math.PI * chiSquare * Math.sqrt(Math.abs(lambda1 * lambda2)));

        return area * 1000000f;  // Convert m² to mm²
    }

    private float[] calculateEigenvalues2x2(float[][] matrix) {
        float a = matrix[0][0];
        float b = matrix[0][1];
        float d = matrix[1][1];

        // For 2x2 matrix, eigenvalues = (a+d)/2 ± sqrt(((a-d)/2)² + b²)
        float trace = a + d;
        float determinant = a * d - b * b;

        float discriminant = (trace * trace) / 4 - determinant;
        if (discriminant < 0) {
            discriminant = 0; // Handle numerical precision issues
        }

        float sqrtDisc = (float) Math.sqrt(discriminant);
        float lambda1 = trace / 2 + sqrtDisc;
        float lambda2 = trace / 2 - sqrtDisc;

        // Ensure lambda1 >= lambda2
        if (lambda1 < lambda2) {
            float temp = lambda1;
            lambda1 = lambda2;
            lambda2 = temp;
        }

        return new float[]{lambda1, lambda2};
    }

    private Point2D getMean(List<Point2D> COMHistories) {
        float meanX = 0;
        float meanZ = 0;
        for (Point2D point : COMHistories) {
            meanX += (float) point.getX();
            meanZ += (float) point.getZ();
        }
        meanX /= COMHistories.size();
        meanZ /= COMHistories.size();
        return new Point2D(meanX, meanZ);
    }
}
