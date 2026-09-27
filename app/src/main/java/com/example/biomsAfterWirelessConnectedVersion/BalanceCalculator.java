package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class BalanceCalculator {

    private float swayArea = 0;

    // Calculates 95% confidence ellipse area (sway area) with low-pass Butterworth filtering
    private void calculateSwayArea(List<Point2D> COMHistories, Context context) {
        if (COMHistories == null || COMHistories.size() < 3) {
            swayArea = 0;
            return;
        }

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

        swayArea = area * 1000000f;  // Convert m² to mm²
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

    public float getSwayArea(List<Point2D> COMHistories, Context context) {
        calculateSwayArea(COMHistories, context);
        return swayArea;
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