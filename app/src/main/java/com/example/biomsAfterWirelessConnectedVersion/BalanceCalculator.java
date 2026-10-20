package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class BalanceCalculator {

    public SwayMetrics computeSwayMetrics(List<Point2D> COMHistories, float frameRate) {
        SwayMetrics m = new SwayMetrics();
        if (COMHistories == null || COMHistories.size() < 3) {
            return m;
        }

        List<Point2D> filtered = getFilteredCOM(COMHistories);
        int n = filtered.size();
        m.sampleCount = n;
        m.frameRate = frameRate;

        // 1. Sway area: 95% confidence ellipse (mm^2)
        m.swayAreaMm2 = ellipseArea(filtered);

        // 2. Path length: total trajectory distance (m -> mm)
        double path = 0.0;
        for (int i = 1; i < n; i++) {
            double dx = filtered.get(i).getX() - filtered.get(i - 1).getX();
            double dz = filtered.get(i).getZ() - filtered.get(i - 1).getZ();
            path += Math.sqrt(dx * dx + dz * dz);
        }
        m.pathLengthMm = (float) (path * 1000.0);

        // 3. Mean velocity (mm/s)
        if (frameRate > 0) {
            double durationS = n / (double) frameRate;
            if (durationS > 0) {
                m.meanVelocityMmS = (float) (m.pathLengthMm / durationS);
            }
        }

        // 4. RMS displacement about the mean, per axis (m -> mm).
        //    Also captures the filtered trajectory for export.
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

    public float getSwayArea(List<Point2D> COMHistories, Context context) {
        return computeSwayMetrics(COMHistories, 30f).swayAreaMm2;
    }

    private List<Point2D> getFilteredCOM(List<Point2D> COMHistories) {
        // 1. Separate X and Z trajectories for filtering
        List<Float> rawX = new ArrayList<>(COMHistories.size());
        List<Float> rawZ = new ArrayList<>(COMHistories.size());
        for (Point2D p : COMHistories) {
            rawX.add((float) p.getX());
            rawZ.add((float) p.getZ());
        }

        // 2. Apply low-pass Butterworth filter (6.0 Hz cutoff for body sway) to reduce pose estimation noise
        ButterworthFilter filter = new ButterworthFilter(6.0f, 30.0f);
        List<Float> filteredX = filter.filter(rawX);
        List<Float> filteredZ = filter.filter(rawZ);

        // 3. Reconstruct filtered Point2D list
        List<Point2D> filteredCOM = new ArrayList<>(COMHistories.size());
        for (int i = 0; i < filteredX.size(); i++) {
            filteredCOM.add(new Point2D(filteredX.get(i), filteredZ.get(i)));
        }
        return filteredCOM;
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