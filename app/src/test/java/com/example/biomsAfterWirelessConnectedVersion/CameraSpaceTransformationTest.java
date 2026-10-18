package com.example.biomsAfterWirelessConnectedVersion;

import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CameraSpaceTransformationTest {

    private PoseLandmarkerHelper poseLandmarkerHelper;

    @Before
    public void setUp() {
        poseLandmarkerHelper = new PoseLandmarkerHelper();
    }

    @Test
    public void testCalculateCameraSpaceHipWithValidData() {
        int width = 640;
        int height = 480;

        List<NormalizedLandmark> landmarks = new ArrayList<>();
        List<Landmark> worldLandmarks = new ArrayList<>();

        for (int i = 0; i < 33; i++) {
            landmarks.add(NormalizedLandmark.create(0.5f, 0.5f, 0.0f));
            worldLandmarks.add(Landmark.create(0.0f, 0.0f, 0.0f));
        }

        // Set left/right shoulder and hip
        // 11: left shoulder, 12: right shoulder, 23: left hip, 24: right hip
        landmarks.set(11, NormalizedLandmark.create(0.4f, 0.3f, 0.0f));
        landmarks.set(12, NormalizedLandmark.create(0.6f, 0.3f, 0.0f));
        landmarks.set(23, NormalizedLandmark.create(0.4f, 0.7f, 0.0f));
        landmarks.set(24, NormalizedLandmark.create(0.6f, 0.7f, 0.0f));

        worldLandmarks.set(11, Landmark.create(-0.2f, -0.25f, 0.0f));
        worldLandmarks.set(12, Landmark.create(0.2f, -0.25f, 0.0f));
        worldLandmarks.set(23, Landmark.create(-0.2f, 0.25f, 0.0f));
        worldLandmarks.set(24, Landmark.create(0.2f, 0.25f, 0.0f));

        double[] cameraSpaceHip = poseLandmarkerHelper.calculateCameraSpaceHip(landmarks, worldLandmarks, width, height);

        assertNotNull(cameraSpaceHip);
        assertEquals(3, cameraSpaceHip.length);
        assertTrue(cameraSpaceHip[2] > 0); // Z (depth) should be positive
    }

    @Test
    public void testCalculateCameraSpaceHipWithInvalidInputs() {
        assertNull(poseLandmarkerHelper.calculateCameraSpaceHip(null, null, 640, 480));
    }
}
