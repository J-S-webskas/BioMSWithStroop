package com.example.bioms1;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestResult {
    public enum TestType {
        BASELINE, DUAL_TASK, SENSORY_ATAXIA
    }

    private TestType testType;
    private Date timestamp;
    private float speed;
    private Map<String, Float> stiffnessScores; // joint -> stiffness
    private Map<String, Float> asymmetryScores; // jointType -> asymmetry

    // Constructor
    public TestResult(TestType testType, float speed,
                      Map<String, Float> stiffnessScores,
                      Map<String, Float> asymmetryScores) {
        this.testType = testType;
        this.timestamp = new Date();
        this.speed = speed;
        this.stiffnessScores = stiffnessScores != null ? stiffnessScores : new HashMap<>();
        this.asymmetryScores = asymmetryScores != null ? asymmetryScores : new HashMap<>();
    }

    // Getters and setters
    public TestType getTestType() { return testType; }
    public Date getTimestamp() { return timestamp; }
    public float getSpeed() { return speed; }
    public Map<String, Float> getStiffnessScores() { return stiffnessScores; }
    public Map<String, Float> getAsymmetryScores() { return asymmetryScores; }

}