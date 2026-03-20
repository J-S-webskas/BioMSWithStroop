package com.example.bioms1;

import androidx.lifecycle.ViewModel;

/**
 * This ViewModel is used to store pose landmarker helper settings
 */
public class MainViewModel extends ViewModel {

    private int model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL;
    private int delegate = PoseLandmarkerHelper.DELEGATE_CPU;
    private float minPoseDetectionConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE;
    private float minPoseTrackingConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE;
    private float minPosePresenceConfidence =
            PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE;

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