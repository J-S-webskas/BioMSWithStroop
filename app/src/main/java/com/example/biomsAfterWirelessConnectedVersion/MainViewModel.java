package com.example.biomsAfterWirelessConnectedVersion;

import android.view.View;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * This ViewModel is used to store pose landmarker helper settings
 */
public class MainViewModel extends ViewModel {

    private boolean shouldDiscard = false;
    private boolean isPreviewMode = true;
    private boolean isCooldownActive;
    private int model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL;
    private int delegate = PoseLandmarkerHelper.DELEGATE_CPU;
    private float minPoseDetectionConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE;
    private float minPoseTrackingConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE;
    private float minPosePresenceConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE;
    private List<View> testButtons = new ArrayList<>();
    private String assessmentType = "";

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }
    public String getAssessmentType() {
        return assessmentType;
    }
    public void registerTestButton(View button) {
        testButtons.add(button);
    }

    public void setShouldDiscard(boolean discard) {
        shouldDiscard = discard;
    }

    public boolean shouldDiscard() {
        return shouldDiscard;
    }

    public void setCooldownActive(boolean active) {
        isCooldownActive = active;

        // ← ITERATES through all stored buttons and disables them
        for (View button : testButtons) {
            button.setEnabled(!active);  // Disables button if active = true
        }
    }

    public boolean isCooldownActive() {
        return isCooldownActive;
    }

    public void setPreviewMode(boolean isPreview) {
        this.isPreviewMode = isPreview;
    }

    public boolean isPreviewMode() {
        return isPreviewMode;
    }

    // Getter methods
    public int getCurrentDelegate() {
        return delegate;
    }

    public int getCurrentModel() {
        return model;
    }

    public float getCurrentMinPoseDetectionConfidence() {
        return minPoseDetectionConfidence;
    }

    public float getCurrentMinPoseTrackingConfidence() {
        return minPoseTrackingConfidence;
    }

    public float getCurrentMinPosePresenceConfidence() {
        return minPosePresenceConfidence;
    }

    // Setter methods
    public void setDelegate(int delegate) {
        this.delegate = delegate;
    }

    public void setMinPoseDetectionConfidence(float confidence) {
        this.minPoseDetectionConfidence = confidence;
    }

    public void setMinPoseTrackingConfidence(float confidence) {
        this.minPoseTrackingConfidence = confidence;
    }

    public void setMinPosePresenceConfidence(float confidence) {
        this.minPosePresenceConfidence = confidence;
    }

    public void setModel(int model) {
        this.model = model;
    }

}