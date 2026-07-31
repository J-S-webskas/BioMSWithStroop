package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.List;
import java.util.TreeSet;

public class BalanceCalculator {

    private float swayArea = 0;

    /// todo: extract the normalize landmarks to form 2D COM data points
    /// todo: get the total mass of the individual
    /// todo: calculate COM based on segment data from de Leva table (adult only)
    /// todo: calculate & form the ellipse from COM data
    /// return the area

    // Calculates 95% confidence ellipse area (sway area)
    private void calculateSwayArea(List<Point2D> COMHistories, Context context) {
        if (COMHistories == null) {
            return;
        }

        Point2D point = getMean(COMHistories);
        float meanX = (float) point.getX();
        float meanZ = (float) point.getZ();

        // todo: iterate over and get treeset values pointx - mean x

        float sumX_diff_sq = 0, sumZ_diff_sq = 0, sumXZ_diff = 0;
        int n = COMHistories.size();
        for (Point2D p : COMHistories) {
            float diffX = (float) p.getX() - meanX;
            float diffZ = (float) p.getZ() - meanZ;
            sumX_diff_sq += diffX * diffX;      // Σ(x - x̄)²
            sumZ_diff_sq += diffZ * diffZ;      // Σ(z - z̄)²
            sumXZ_diff += diffX * diffZ;        // Σ(x - x̄)(z - z̄)
        }
        float variance_x = sumX_diff_sq / (n-1);
        float variance_z = sumZ_diff_sq / (n-1);
        float covariance_xz = sumXZ_diff / (n-1);

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

    public float getSwayArea(List<Point2D>COMHistories, Context context) {
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

    public static float calculateOffBalance(
            ArrayDeque<Float> hipAngles,
            ArrayDeque<Float> kneeAngles) {

        if (hipAngles == null || kneeAngles == null)
            return 0f;

        float minAngle = Float.MAX_VALUE;
        float maxAngle = Float.MIN_VALUE;

        int size = Math.min(hipAngles.size(), kneeAngles.size());

        Float[] hipArray = hipAngles.toArray(new Float[0]);
        Float[] kneeArray = kneeAngles.toArray(new Float[0]);

        for (int i = 0; i < size; i++) {

            float hip_angle = hipArray[i];
            float knee_angle = kneeArray[i];

            if (hip_angle > 95f || hip_angle < 90f) {

                if (minAngle > hip_angle)
                    minAngle = hip_angle;

                if (maxAngle < hip_angle)
                    maxAngle = hip_angle;

            } else if (knee_angle > 200f || knee_angle < 150f) {

                if (minAngle > knee_angle)
                    minAngle = knee_angle;

                if (maxAngle < knee_angle)
                    maxAngle = knee_angle;
            }
        }

        if (minAngle == Float.MAX_VALUE)
            return 0f; // balanced entire session

        return (maxAngle + minAngle) / 2f;
    }
}