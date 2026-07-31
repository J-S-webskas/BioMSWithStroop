package com.example.biomsAfterWirelessConnectedVersion;

import java.util.Map;

public class TestComparison {
    private TestResult.TestType testType;
    private float duration; // seconds
    private float averageSpeed;
    private float speedChangePercent;
    private Map<String, Float> averageStiffness;
    private Map<String, Float> stiffnessChangesPercent;
    private Map<String, Float> averageAsymmetry;
    private Map<String, Float> asymmetryChangesPercent;
    private float speedImpact;
    private float stiffnessImpact;
    private float asymmetryImpact;

    public TestComparison(TestResult.TestType testType, float duration,
                          float averageSpeed, float speedChangePercent,
                          Map<String, Float> averageStiffness,
                          Map<String, Float> stiffnessChangesPercent,
                          Map<String, Float> averageAsymmetry,
                          Map<String, Float> asymmetryChangesPercent,
                          float speedImpact, float stiffnessImpact,
                          float asymmetryImpact) {
        this.testType = testType;
        this.duration = duration;
        this.averageSpeed = averageSpeed;
        this.speedChangePercent = speedChangePercent;
        this.averageStiffness = averageStiffness;
        this.stiffnessChangesPercent = stiffnessChangesPercent;
        this.averageAsymmetry = averageAsymmetry;
        this.asymmetryChangesPercent = asymmetryChangesPercent;
        this.speedImpact = speedImpact;
        this.stiffnessImpact = stiffnessImpact;
        this.asymmetryImpact = asymmetryImpact;
    }

    // The numbers here are in percentage
    // TODO: Only speed in here for now. Need balance, endurance, and spasticity
    public String getSpeedComparison() {
        StringBuilder summary = new StringBuilder();

        switch (testType) {
            case DUAL_TASK:
                if (speedImpact > 50) {
                    summary.append("Large speed reduction (").append(String.format("%.1f", speedImpact))
                            .append("%) during counting");
                } else if (speedImpact > 20) {
                    summary.append("Moderate speed reduction (").append(String.format("%.1f", speedImpact))
                            .append("%) during counting");
                } else {
                    summary.append("Minimal speed change (").append(String.format("%.1f", speedImpact))
                            .append("%) during counting");
                }
                break;

            case SENSORY_ATAXIA:
                if (speedImpact > 60) {
                    summary.append("Large reliance on vision (").append(String.format("%.1f", speedImpact))
                            .append("% speed reduction)");
                } else if (speedImpact > 30) {
                    summary.append("Moderate reliance on vision (").append(String.format("%.1f", speedImpact))
                            .append("% speed reduction)");
                } else {
                    summary.append("Good speed without vision (").append(String.format("%.1f", speedImpact))
                            .append("% speed change)");
                }
                break;
        }

        return summary.toString();
    }
}