package com.example.bioms1;

import java.util.*;

public class DualTaskTest {
    private TestResult baseline;
    private List<TestResult> dualTestResults;
    private boolean isActive;
    private long startTime;

    public DualTaskTest(TestResult baseline) {
        this.baseline = baseline;
        this.dualTestResults = new ArrayList<>();
        this.isActive = false;
    }

    public void startTest() {
        this.isActive = true;
        this.startTime = System.currentTimeMillis();
        this.dualTestResults.clear();
    }

    public void addMeasurement(float currentSpeed,
                               Map<String, Float> currentStiffness,
                               Map<String, Float> currentAsymmetry) {
        if (isActive) {
            TestResult result = new TestResult(
                    TestResult.TestType.DUAL_TASK,
                    currentSpeed,
                    currentStiffness,
                    currentAsymmetry
            );
            dualTestResults.add(result);
        }
    }

    public TestComparison stopTest() {
        if (!isActive || dualTestResults.isEmpty()) {
            return null;
        }

        isActive = false;
        long duration = System.currentTimeMillis() - startTime;

        // Calculate averages during dual test
        float avgSpeed = calculateAverageSpeed();
        Map<String, Float> avgStiffness = calculateAverageStiffness();
        Map<String, Float> avgAsymmetry = calculateAverageAsymmetry();

        // Calculate percentage changes from baseline
        float speedChangePercent = calculatePercentageChange(
                avgSpeed, baseline.getSpeed());

        Map<String, Float> stiffnessChanges = new HashMap<>();
        for (Map.Entry<String, Float> entry : avgStiffness.entrySet()) {
            String joint = entry.getKey();
            Float baselineStiffness = baseline.getStiffnessScores().get(joint);
            if (baselineStiffness != null && baselineStiffness > 0) {
                float change = calculatePercentageChange(entry.getValue(), baselineStiffness);
                stiffnessChanges.put(joint, change);
            }
        }

        Map<String, Float> asymmetryChanges = new HashMap<>();
        for (Map.Entry<String, Float> entry : avgAsymmetry.entrySet()) {
            String jointType = entry.getKey();
            Float baselineAsymmetry = baseline.getAsymmetryScores().get(jointType);
            if (baselineAsymmetry != null && baselineAsymmetry > 0) {
                float change = calculatePercentageChange(entry.getValue(), baselineAsymmetry);
                asymmetryChanges.put(jointType, change);
            }
        }

        // Calculate impact scores
        float speedImpact = Math.abs(speedChangePercent);
        float stiffnessImpact = calculateAverageAbsoluteValue(stiffnessChanges);
        float asymmetryImpact = calculateAverageAbsoluteValue(asymmetryChanges);

        return new TestComparison(
                TestResult.TestType.DUAL_TASK,
                duration / 1000f, // Convert to seconds
                avgSpeed,
                speedChangePercent,
                avgStiffness,
                stiffnessChanges,
                avgAsymmetry,
                asymmetryChanges,
                speedImpact,
                stiffnessImpact,
                asymmetryImpact
        );
    }

    private float calculateAverageSpeed() {
        float sum = 0;
        for (TestResult result : dualTestResults) {
            sum += result.getSpeed();
        }
        return sum / dualTestResults.size();
    }

    private Map<String, Float> calculateAverageStiffness() {
        Map<String, Float> sumMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        for (TestResult result : dualTestResults) {
            for (Map.Entry<String, Float> entry : result.getStiffnessScores().entrySet()) {
                String joint = entry.getKey();
                float stiffness = entry.getValue();

                sumMap.put(joint, sumMap.getOrDefault(joint, 0f) + stiffness);
                countMap.put(joint, countMap.getOrDefault(joint, 0) + 1);
            }
        }

        Map<String, Float> averages = new HashMap<>();
        for (Map.Entry<String, Float> entry : sumMap.entrySet()) {
            String joint = entry.getKey();
            int count = countMap.get(joint);
            averages.put(joint, entry.getValue() / count);
        }

        return averages;
    }

    private Map<String, Float> calculateAverageAsymmetry() {
        // Similar to calculateAverageStiffness
        Map<String, Float> sumMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        for (TestResult result : dualTestResults) {
            for (Map.Entry<String, Float> entry : result.getAsymmetryScores().entrySet()) {
                String jointType = entry.getKey();
                float asymmetry = entry.getValue();

                sumMap.put(jointType, sumMap.getOrDefault(jointType, 0f) + asymmetry);
                countMap.put(jointType, countMap.getOrDefault(jointType, 0) + 1);
            }
        }

        Map<String, Float> averages = new HashMap<>();
        for (Map.Entry<String, Float> entry : sumMap.entrySet()) {
            String jointType = entry.getKey();
            int count = countMap.get(jointType);
            averages.put(jointType, entry.getValue() / count);
        }

        return averages;
    }

    private float calculatePercentageChange(float newValue, float baselineValue) {
        if (baselineValue == 0) return 0;
        return ((newValue - baselineValue) / baselineValue) * 100;
    }

    private float calculateAverageAbsoluteValue(Map<String, Float> values) {
        if (values.isEmpty()) return 0;

        float sum = 0;
        for (float value : values.values()) {
            sum += Math.abs(value);
        }
        return sum / values.size();
    }
}