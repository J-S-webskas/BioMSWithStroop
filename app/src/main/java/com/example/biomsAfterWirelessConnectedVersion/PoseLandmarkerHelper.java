package com.example.biomsAfterWirelessConnectedVersion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.Landmark;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/*
Camera frame 1 (t=0ms) -> MediaPipe processing -> returnLivestreamResult(result 1) -> CameraFragment
Camera frame 2 (t=33ms) -> MediaPipe processing -> returnLivestreamResult(result 2) -> CameraFragment
etc

Key point: Each call contains only a single frame of data.
 */
public class PoseLandmarkerHelper {
    // Constants (converted from Kotlin companion object)
    public static final String TAG = "PoseLandmarkerHelper";
    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final float DEFAULT_POSE_DETECTION_CONFIDENCE = 0.5F;
    public static final float DEFAULT_POSE_TRACKING_CONFIDENCE = 0.5F;
    public static final float DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.5F;
    public static final int DEFAULT_NUM_POSES = 1;
    public static final int OTHER_ERROR = 0;
    public static final int GPU_ERROR = 1;
    public static final int MODEL_POSE_LANDMARKER_FULL = 0;
    public static final int MODEL_POSE_LANDMARKER_LITE = 1;
    public static final int MODEL_POSE_LANDMARKER_HEAVY = 2;

    long lastTimestamp = 0;  // The timestamp of the previous frame that was processed (time interval between consecutive frames)
    // Properties
    public float minPoseDetectionConfidence;
    public float minPoseTrackingConfidence;
    public float minPosePresenceConfidence;
    public int currentModel;
    public int currentDelegate;
    public RunningMode runningMode;
    public Context context;
    public LandmarkerListener poseLandmarkerHelperListener;
    private double cachedMass = -1;
    private String cachedSex = null;
    private boolean isFirstRun = true;
    private PoseLandmarker poseLandmarker;

    private double[] filteredCHip = null;
    private static final double ALPHA = 0.25;
    public static final double DEFAULT_FOV_H_DEGREES = 60.0;
    private double fovHDegrees = DEFAULT_FOV_H_DEGREES;
    private boolean wasTestStarted = false;

    // todo: pass this from record fragment
    public static boolean preview = true;
    public static boolean countdown = false;
    private MainViewModel viewModel;

    public void setFovDegrees(double fov) {
        if (fov > 0) {
            this.fovHDegrees = fov;
        }
    }

    public double getFovDegrees() {
        return fovHDegrees;
    }

    public void resetFilter() {
        filteredCHip = null;
    }
    public PoseLandmarkerHelper(Context context) {
        this(DEFAULT_POSE_DETECTION_CONFIDENCE,
                DEFAULT_POSE_TRACKING_CONFIDENCE,
                DEFAULT_POSE_PRESENCE_CONFIDENCE,
                MODEL_POSE_LANDMARKER_FULL,
                DELEGATE_CPU,
                RunningMode.IMAGE,
                context,
                null,
                null);
    }

    public PoseLandmarkerHelper(
            float minPoseDetectionConfidence,
            float minPoseTrackingConfidence,
            float minPosePresenceConfidence,
            int currentModel,
            int currentDelegate,
            RunningMode runningMode,
            Context context,
            LandmarkerListener poseLandmarkerHelperListener,
            MainViewModel viewModel
    ) {
        this.minPoseDetectionConfidence = minPoseDetectionConfidence;
        this.minPoseTrackingConfidence = minPoseTrackingConfidence;
        this.minPosePresenceConfidence = minPosePresenceConfidence;
        this.currentModel = currentModel;
        this.currentDelegate = currentDelegate;
        this.runningMode = runningMode;
        this.context = context;
        this.poseLandmarkerHelperListener = poseLandmarkerHelperListener;
        this.viewModel = viewModel;

        this.setupPoseLandmarker();
    }

    public void clearPoseLandmarker() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
        }
        poseLandmarker = null;
        filteredCHip = null;
    }

    private void updateCacheIfNeeded(Context context) {
        if (isFirstRun) {
            cachedMass = DataStorageManager.getMass(context);
            cachedSex = DataStorageManager.getSex(context);
            isFirstRun = false;
        }
    }

    public boolean isClose() {
        return poseLandmarker == null;
    }

    public void setupPoseLandmarker() {
        BaseOptions.Builder baseOptionBuilder = BaseOptions.builder();

        switch (currentDelegate) {
            case DELEGATE_CPU:
                baseOptionBuilder.setDelegate(Delegate.CPU);
                break;
            case DELEGATE_GPU:
                baseOptionBuilder.setDelegate(Delegate.GPU);
                break;
        }

        String modelName;
        switch (currentModel) {
            case MODEL_POSE_LANDMARKER_FULL:
                modelName = "pose_landmarker_full.task";
                break;
            case MODEL_POSE_LANDMARKER_LITE:
                modelName = "pose_landmarker_lite.task";
                break;
            case MODEL_POSE_LANDMARKER_HEAVY:
                modelName = "pose_landmarker_heavy.task";
                break;
            default:
                modelName = "pose_landmarker_full.task";
                break;
        }

        baseOptionBuilder.setModelAssetPath(modelName);

        if (runningMode == RunningMode.LIVE_STREAM) {
            if (poseLandmarkerHelperListener == null) {
                throw new IllegalStateException(
                        "poseLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                );
            }
        }

        try {
            BaseOptions baseOptions = baseOptionBuilder.build();
            PoseLandmarker.PoseLandmarkerOptions.Builder optionsBuilder =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                            .setMinTrackingConfidence(minPoseTrackingConfidence)
                            .setMinPosePresenceConfidence(minPosePresenceConfidence)
                            .setRunningMode(runningMode);

            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError);
            }

            PoseLandmarker.PoseLandmarkerOptions options = optionsBuilder.build();
            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
        } catch (IllegalStateException e) {
            if (poseLandmarkerHelperListener != null) {
                poseLandmarkerHelperListener.onError(
                        "Pose Landmarker failed to initialize. See error logs for details"
                );
            }
            Log.e(TAG, "MediaPipe failed to load the task with error: " + e.getMessage());
        } catch (RuntimeException e) {
            if (poseLandmarkerHelperListener != null) {
                poseLandmarkerHelperListener.onError(
                        "Pose Landmarker failed to initialize. See error logs for details",
                        GPU_ERROR
                );
            }
            Log.e(TAG, "Image classifier failed to load model with error: " + e.getMessage());
        }
    }
    public void detectLiveStream(ImageProxy imageProxy, boolean isFrontCamera) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw new IllegalArgumentException(
                    "Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM"
            );
        }
        long frameTime = SystemClock.uptimeMillis();

        // Creates empty bitmap matching camera frame dimensions. Bitmap = Digital image representation in Android
        Bitmap bitmapBuffer = Bitmap.createBitmap(
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                Bitmap.Config.ARGB_8888  // ARGB_8888 = 8 bits per channel (Alpha, Red, Green, Blue) = 32-bit color
        );

        imageProxy.getPlanes()[0].getBuffer().rewind();
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());  // Copies raw camera data into bitmap. Bitmap becomes the processed image frame for MediaPipe
        imageProxy.close();

        Matrix matrix = new Matrix();
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

        if (isFrontCamera) {
            matrix.postScale(
                    -1f,
                    1f,
                    imageProxy.getWidth(),
                    imageProxy.getHeight()
            );
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(),
                matrix, true
        );

        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();
        detectAsync(mpImage, frameTime);
    }

    @VisibleForTesting
    public void detectAsync(MPImage mpImage, long frameTime) {
        if (poseLandmarker != null) {
            poseLandmarker.detectAsync(mpImage, frameTime);
        }
    }

    // Includes all frames of the video (batch processing) -> for learnings sake analogy: getting menu and letting all dishes come up vs
    // Real-time processing (stream processing): Take a plate -> Eat -> Take another plate -> Eat (Like eating at a buffet)

    /*
        inferenceIntervalMs:
        Purpose: Determines the density of the analyzed video.
        Example Values:
        33: Analyze one frame every 33ms ≈ 30fps (High Density)
        100: Analyze one frame every 100ms ≈ 10fps (Medium Density)
        1000: Analyze one frame per second (Low Density)
     */
    public ResultBundle detectVideoFile(Uri videoUri, long inferenceIntervalMs) throws IOException {
        // Check the running mode: It must be VIDEO mode
        if (runningMode != RunningMode.VIDEO) {
            throw new IllegalArgumentException(
                    "Attempting to call detectVideoFile while not using RunningMode.VIDEO"
            );
        }

        long startTime = SystemClock.uptimeMillis();
        boolean didErrorOccurred = false;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, videoUri);  // Load video file
        // Get video duration (milliseconds)
        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Long videoLengthMs = durationStr != null ? Long.parseLong(durationStr) : null;

        // Get the first frame to determine the video size
        Bitmap firstFrame = retriever.getFrameAtTime(0);
        Integer width = firstFrame != null ? firstFrame.getWidth() : null;
        Integer height = firstFrame != null ? firstFrame.getHeight() : null;

        // Check if the basic information is complete, like video lenght, width, height
        if (videoLengthMs == null || width == null || height == null) {
            retriever.release();
            return null;  // Unable to process, returns null
        }

        // Prepare the results list and calculate the number of frames.
        List<PoseLandmarkerResult> resultList = new ArrayList<>();
        // Calculate how many frames need to be read
        // For example: a 10-second video (10000ms), analyzing one frame every 33ms → 10000/33 ≈ 303 frames
        long numberOfFrameToRead = videoLengthMs / inferenceIntervalMs;

        // Iterate through each frame (by time interval)
        for (long i = 0; i <= numberOfFrameToRead; i++) {
            // Calculate the timestamp of the current frame
            long timestampMs = i * inferenceIntervalMs;
            // Extract frames from a video at a specified time
            Bitmap frame = retriever.getFrameAtTime(
                    timestampMs * 1000, // Convert to microseconds
                    MediaMetadataRetriever.OPTION_CLOSEST // Get the closest frame
            );

            if (frame != null) {
                // Ensure the frame format is ARGB_8888 (the format required by MediaPipe)
                Bitmap argb8888Frame;
                if (frame.getConfig() == Bitmap.Config.ARGB_8888) {
                    argb8888Frame = frame;
                } else {
                    argb8888Frame = frame.copy(Bitmap.Config.ARGB_8888, false); // Convert to the correct format
                }

                // Convert Bitmap to MediaPipe image format
                MPImage mpImage = new BitmapImageBuilder(argb8888Frame).build();
                if (poseLandmarker != null) {
                    // Detect the pose of the current frame
                    PoseLandmarkerResult detectionResult =
                            poseLandmarker.detectForVideo(mpImage, timestampMs);
                    if (detectionResult != null) {
                        resultList.add(detectionResult); // Add to result list
                    } else {
                        didErrorOccurred = true; // Marking error
                        if (poseLandmarkerHelperListener != null) {
                            poseLandmarkerHelperListener.onError(
                                    "ResultBundle could not be returned in detectVideoFile"
                            );
                        }
                    }
                }
            } else {  // Unable to acquire frame, error flagged
                didErrorOccurred = true;
                if (poseLandmarkerHelperListener != null) {
                    poseLandmarkerHelperListener.onError(
                            "Frame at specified time could not be retrieved when detecting in video."
                    );
                }
            }
        }

        retriever.release();  // Release video search engine

        // Calculate the average processing time per frame
        long inferenceTimePerFrameMs = (SystemClock.uptimeMillis() - startTime) / numberOfFrameToRead;

        if (didErrorOccurred) {
            return null;
        } else {
            // Returns a ResultBundle containing all frame results.
            return new ResultBundle(resultList, inferenceTimePerFrameMs, height, width);
        }
    }

    // Includes only a single image, assuming it is the mediapipe skeleton image
    public ResultBundle detectImage(Bitmap image) {
        if (runningMode != RunningMode.IMAGE) {
            throw new IllegalArgumentException(
                    "Attempting to call detectImage while not using RunningMode.IMAGE"
            );
        }

        long startTime = SystemClock.uptimeMillis();
        MPImage mpImage = new BitmapImageBuilder(image).build();

        if (poseLandmarker != null) {
            PoseLandmarkerResult landmarkResult = poseLandmarker.detect(mpImage);
            if (landmarkResult != null) {
                long inferenceTimeMs = SystemClock.uptimeMillis() - startTime;
                List<PoseLandmarkerResult> resultList = new ArrayList<>();
                resultList.add(landmarkResult);
                return new ResultBundle(resultList, inferenceTimeMs, image.getHeight(), image.getWidth());
            }
        }

        if (poseLandmarkerHelperListener != null) {
            poseLandmarkerHelperListener.onError("Pose Landmarker failed to detect.");
        }
        return null;
    }

    // When returnLivestreamResult(PoseLandmarkerResult result, MPImage input) is called by MediaPipe
    // The result parameter (an object of PoseLandmarkerResult) only stores coordinate for one frame
    // It can get the list of all the people using the method result.landmarks()
    // result.landmark:
    //    [Person 1: 33 landmarks],  // List #1
    //    [Person 2: 33 landmarks]   // List #2 (if multiple people) etc.
    //    result.landmarks().get(0)  // Gets the first person
    //    result.landmarks().get(0).get(0)  // Gets the first person's 0th coordinate, which is the nose
    // This result object gets added to a List<PoseLandmarkerResult> resultList inside ResultBundle
    // The ResultBundle is then sent to listener via poseLandmarkerHelperListener.onResults(bundle)
    // onResults is implemented in the camera fragment, which will extract the coordinates.

    // result only contains the pose detection results for the current frame
    // Each call uses a new frame with new coordinate data1
    private void returnLivestreamResult(PoseLandmarkerResult result, MPImage input) {
        try {
            long finishTimeMs = SystemClock.uptimeMillis();  // The current system time in milliseconds when processing of the frame finished
            long inferenceTime = finishTimeMs - result.timestampMs();  // The time it took to process the frame (Measures performance)

            long currentTimestamp = result.timestampMs();  // The starting time of the current frame being processed
            float actualFrameRate = 0;

            if (lastTimestamp != 0) {
                long frameIntervalMs = currentTimestamp - lastTimestamp;
                actualFrameRate = 1000f / frameIntervalMs; // Calculate actual FPS
            }
            lastTimestamp = currentTimestamp;

            RecordFragment.setFrameRate(actualFrameRate);

            if (poseLandmarkerHelperListener != null) {
                boolean hasValidPose = false;
                // Check if we have valid pose detection
                if (result != null && result.landmarks() != null && !result.landmarks().isEmpty()) {
                    List<PoseLandmarkerResult> resultList = new ArrayList<>();

                    resultList.add(result);  // Coordinate data of the current frame
                    ResultBundle bundle = new ResultBundle(resultList, inferenceTime, input.getHeight(), input.getWidth());  // input.getHeight() and input.getWidth() get the pixel dimensions of the camera frame/image that was processed.
                    poseLandmarkerHelperListener.onResults(bundle); // onResults is implemented inside of CameraFragment
                    // Get the first (and only) PoseLandmarkerResult from the list
                    List<NormalizedLandmark> landmarks = bundle.results.get(0).landmarks().get(0);  // first person's 33 landmarks all in here
                    List<Landmark> worldLandmarks = result.worldLandmarks().get(0);

                    if (landmarks != null && landmarks.size() > 28) {
                        updateCacheIfNeeded(context);
                        double mass = cachedMass;
                        List<Double> xz_final= new ArrayList<>();
                        if ("female".equalsIgnoreCase(cachedSex)) {
                            xz_final= calculateCOMForFemale(mass, worldLandmarks);
                        } else { // For others, just do male
                            xz_final= calculateCOMForMale(mass, worldLandmarks);
                        }
                        double comZ_final = 0, comX_final = 0;
                        if (!xz_final.isEmpty()) {
                            double comX_rel = xz_final.get(0);
                            double comZ_rel = xz_final.get(1);

                            double[] cameraSpaceHip = calculateCameraSpaceHip(landmarks, worldLandmarks, input.getWidth(), input.getHeight());
                            if (cameraSpaceHip != null) {
                                comX_final = cameraSpaceHip[0] + comX_rel;
                                comZ_final = cameraSpaceHip[2] + comZ_rel;
                            } else {
                                comX_final = comX_rel;
                                comZ_final = comZ_rel;
                            }
                        }

                        hasValidPose = true;
                        boolean currentlyStarted = RecordFragment.isTestStarted();
                        if (currentlyStarted && !wasTestStarted) {
                            filteredCHip = null; // Clean reset at start of trial
                        }
                        wasTestStarted = currentlyStarted;

                        if (currentlyStarted) {
                            RecordFragment.setHasValidPose(true);
                        }
                        float left_hip = calculateAngle(landmarks.get(11), landmarks.get(23), landmarks.get(25));
                        float right_knee = calculateAngle(landmarks.get(24), landmarks.get(26), landmarks.get(28));
                        float left_knee = calculateAngle(landmarks.get(23), landmarks.get(25), landmarks.get(27));

                        if (RecordFragment.isTestStarted()) {
                            // pass the COM x and y to record fragment:
                            if (comX_final != 0 && comZ_final != 0) {
                                RecordFragment.addCOMPerFrame(comX_final, comZ_final);
                            }
                            RecordFragment.addAnglePerFrame("right_knee", right_knee);
                            RecordFragment.addAnglePerFrame("left_knee", left_knee);
                            RecordFragment.HipBalanceaddAnglePerFrame("left_hip", left_hip);
                            RecordFragment.KneeBalanceaddAnglePerFrame("left_knee", left_knee);
                        }
                    }
                }
                if (!hasValidPose) {
                    filteredCHip = null;
                    if (RecordFragment.isTestStarted()) {
                        RecordFragment.setHasValidPose(false);
                    }
                    Log.d(TAG, "No valid pose detected, sending clear signal");
                    ResultBundle clearBundle = new ResultBundle(new ArrayList<>(), inferenceTime,
                            input != null ? input.getHeight() : 0,
                            input != null ? input.getWidth() : 0);
                    poseLandmarkerHelperListener.onResults(clearBundle);

                    // Note: NO data recording happens here
                }
            }
        } catch (IndexOutOfBoundsException e){
            Log.e(TAG, "IndexOutOfBoundsException in returnLivestreamResult: " + e.getMessage());
            Log.e(TAG, "This typically occurs when no pose is detected or insufficient landmarks");
            if (poseLandmarkerHelperListener != null) {
                // Notify listener with empty result to avoid breaking the stream
                ResultBundle emptyBundle = new ResultBundle(new ArrayList<>(), 0,
                        input != null ? input.getHeight() : 0,
                        input != null ? input.getWidth() : 0);
                poseLandmarkerHelperListener.onResults(emptyBundle);
            }
        } catch (NullPointerException e){
            Log.e(TAG, "NullPointerException in returnLivestreamResult: " + e.getMessage());
            Log.e(TAG, "This occurs when result, landmarks, or input is null");
            if (poseLandmarkerHelperListener != null) {
                ResultBundle emptyBundle = new ResultBundle(new ArrayList<>(), 0,
                        input != null ? input.getHeight() : 0,
                        input != null ? input.getWidth() : 0);
                poseLandmarkerHelperListener.onResults(emptyBundle);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
            // Send clear signal on error
            if (poseLandmarkerHelperListener != null) {
                ResultBundle clearBundle = new ResultBundle(new ArrayList<>(), 0,
                        input != null ? input.getHeight() : 0,
                        input != null ? input.getWidth() : 0);
                poseLandmarkerHelperListener.onResults(clearBundle);
            }
        }
    }

    public double[] calculateCameraSpaceHip(List<NormalizedLandmark> landmarks, List<Landmark> worldLandmarks, int width, int height) {
        if (landmarks == null || worldLandmarks == null || landmarks.size() <= 24 || worldLandmarks.size() <= 24 || width <= 0 || height <= 0) {
            return null;
        }

        NormalizedLandmark l_sh = landmarks.get(11);
        NormalizedLandmark r_sh = landmarks.get(12);
        NormalizedLandmark l_hip = landmarks.get(23);
        NormalizedLandmark r_hip = landmarks.get(24);

        double l_sh_px_x = l_sh.x() * width;
        double l_sh_px_y = l_sh.y() * height;
        double r_sh_px_x = r_sh.x() * width;
        double r_sh_px_y = r_sh.y() * height;
        double sh_mid_px_x = (l_sh_px_x + r_sh_px_x) / 2.0;
        double sh_mid_px_y = (l_sh_px_y + r_sh_px_y) / 2.0;

        double l_hip_px_x = l_hip.x() * width;
        double l_hip_px_y = l_hip.y() * height;
        double r_hip_px_x = r_hip.x() * width;
        double r_hip_px_y = r_hip.y() * height;
        double hip_mid_px_x = (l_hip_px_x + r_hip_px_x) / 2.0;
        double hip_mid_px_y = (l_hip_px_y + r_hip_px_y) / 2.0;

        double d_torso_pixels = Math.hypot(sh_mid_px_x - hip_mid_px_x, sh_mid_px_y - hip_mid_px_y);
        if (d_torso_pixels <= 1e-5) {
            return null;
        }

        Landmark l_sh_w = worldLandmarks.get(11);
        Landmark r_sh_w = worldLandmarks.get(12);
        Landmark l_hip_w = worldLandmarks.get(23);
        Landmark r_hip_w = worldLandmarks.get(24);

        double sh_mid_w_x = (l_sh_w.x() + r_sh_w.x()) / 2.0;
        double sh_mid_w_y = (l_sh_w.y() + r_sh_w.y()) / 2.0;
        double sh_mid_w_z = (l_sh_w.z() + r_sh_w.z()) / 2.0;

        double hip_mid_w_x = (l_hip_w.x() + r_hip_w.x()) / 2.0;
        double hip_mid_w_y = (l_hip_w.y() + r_hip_w.y()) / 2.0;
        double hip_mid_w_z = (l_hip_w.z() + r_hip_w.z()) / 2.0;

        double dx = sh_mid_w_x - hip_mid_w_x;
        double dy = sh_mid_w_y - hip_mid_w_y;
        double dz = sh_mid_w_z - hip_mid_w_z;
        double D_torso_meters = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double f_pixels = width / (2.0 * Math.tan(Math.toRadians(fovHDegrees / 2.0)));
        double cx = width / 2.0;
        double cy = height / 2.0;

        double Z_hip = f_pixels * D_torso_meters / d_torso_pixels;
        double X_hip = D_torso_meters * (hip_mid_px_x - cx) / d_torso_pixels;
        double Y_hip = D_torso_meters * (hip_mid_px_y - cy) / d_torso_pixels;

        double[] rawCHip = new double[]{X_hip, Y_hip, Z_hip};

        if (filteredCHip == null) {
            filteredCHip = rawCHip;
        } else {
            filteredCHip[0] = ALPHA * rawCHip[0] + (1 - ALPHA) * filteredCHip[0];
            filteredCHip[1] = ALPHA * rawCHip[1] + (1 - ALPHA) * filteredCHip[1];
            filteredCHip[2] = ALPHA * rawCHip[2] + (1 - ALPHA) * filteredCHip[2];
        }

        return filteredCHip;
    }

    private List<Double> calculateCOMForMale(double totalMass, List<Landmark> worldLandmarks) {
        List<Double> com = new ArrayList<>();
        double comX = 0;
        double comZ = 0;  // Using Z for depth (forward/backward in normalized space)
        double totalMassUsed = 0;

        // 1. TRUNK (43.46% for male)
        Landmark leftShoulder = worldLandmarks.get(11);
        Landmark rightShoulder = worldLandmarks.get(12);
        Landmark leftHip = worldLandmarks.get(23);
        Landmark rightHip = worldLandmarks.get(24);

        // Shoulder midpoint (proximal end of trunk)
        double shoulderX = (leftShoulder.x() + rightShoulder.x()) / 2;
        double shoulderZ = (leftShoulder.z() + rightShoulder.z()) / 2;

        // Hip midpoint (distal end of trunk)
        double hipX = (leftHip.x() + rightHip.x()) / 2;
        double hipZ = (leftHip.z() + rightHip.z()) / 2;

        // Trunk COM = proximal + (distal - proximal) * COM_location_ratio
        // COM location ratio for male trunk: 0.4458 (from proximal)
        double trunkCOM_X = shoulderX + (hipX - shoulderX) * 0.4458;
        double trunkCOM_Z = shoulderZ + (hipZ - shoulderZ) * 0.4458;
        double trunkMass = 0.4346 * totalMass;  // 43.46% for male
        comX += trunkCOM_X * trunkMass;
        comZ += trunkCOM_Z * trunkMass;
        totalMassUsed += trunkMass;

        // 2. LEFT THIGH (14.16% for male)
        Landmark leftHipJoint = worldLandmarks.get(23);    // Proximal end
        Landmark leftKnee = worldLandmarks.get(25);         // Distal end
        // COM location for male thigh: 0.3775 from proximal
        double leftThighCOM_X = leftHipJoint.x() + (leftKnee.x() - leftHipJoint.x()) * 0.3775;
        double leftThighCOM_Z = leftHipJoint.z() + (leftKnee.z() - leftHipJoint.z()) * 0.3775;
        double leftThighMass = 0.1416 * totalMass;  // 14.16% for male
        comX += leftThighCOM_X * leftThighMass;
        comZ += leftThighCOM_Z * leftThighMass;
        totalMassUsed += leftThighMass;

        // 3. RIGHT THIGH (14.16% for male)
        Landmark rightHipJoint = worldLandmarks.get(24);    // Proximal end
        Landmark rightKnee = worldLandmarks.get(26);        // Distal end
        double rightThighCOM_X = rightHipJoint.x() + (rightKnee.x() - rightHipJoint.x()) * 0.3775;
        double rightThighCOM_Z = rightHipJoint.z() + (rightKnee.z() - rightHipJoint.z()) * 0.3775;
        double rightThighMass = 0.1416 * totalMass;  // 14.16% for male
        comX += rightThighCOM_X * rightThighMass;
        comZ += rightThighCOM_Z * rightThighMass;
        totalMassUsed += rightThighMass;

        // 4. LEFT SHANK (4.33% for male)
        Landmark leftKnee2 = worldLandmarks.get(25);        // Proximal end
        Landmark leftAnkle = worldLandmarks.get(27);        // Distal end
        // COM location for male shank: 0.4225 from proximal
        double leftShankCOM_X = leftKnee2.x() + (leftAnkle.x() - leftKnee2.x()) * 0.4225;
        double leftShankCOM_Z = leftKnee2.z() + (leftAnkle.z() - leftKnee2.z()) * 0.4225;
        double leftShankMass = 0.0433 * totalMass;  // 4.33% for male
        comX += leftShankCOM_X * leftShankMass;
        comZ += leftShankCOM_Z * leftShankMass;
        totalMassUsed += leftShankMass;

        // 5. RIGHT SHANK (4.33% for male)
        Landmark rightKnee2 = worldLandmarks.get(26);       // Proximal end
        Landmark rightAnkle = worldLandmarks.get(28);       // Distal end
        double rightShankCOM_X = rightKnee2.x() + (rightAnkle.x() - rightKnee2.x()) * 0.4225;
        double rightShankCOM_Z = rightKnee2.z() + (rightAnkle.z() - rightKnee2.z()) * 0.4225;
        double rightShankMass = 0.0433 * totalMass;  // 4.33% for male
        comX += rightShankCOM_X * rightShankMass;
        comZ += rightShankCOM_Z * rightShankMass;
        totalMassUsed += rightShankMass;

        // 6. LEFT FOOT (1.37% for male)
        Landmark leftAnkle2 = worldLandmarks.get(27);       // Proximal end
        Landmark leftHeel = worldLandmarks.get(29);         // Heel
        Landmark leftToe = worldLandmarks.get(31);          // Toe
        // Using heel as distal for simplicity
        double leftFootDistalX = leftHeel.x();
        double leftFootDistalZ = leftHeel.z();
        // Alternative: use midpoint of heel and toe
        // double leftFootDistalX = (leftHeel.x() + leftToe.x()) / 2;
        // double leftFootDistalZ = (leftHeel.z() + leftToe.z()) / 2;

        // COM location for male foot: 0.4069 from proximal
        double leftFootCOM_X = leftAnkle2.x() + (leftFootDistalX - leftAnkle2.x()) * 0.4069;
        double leftFootCOM_Z = leftAnkle2.z() + (leftFootDistalZ - leftAnkle2.z()) * 0.4069;
        double leftFootMass = 0.0137 * totalMass;  // 1.37% for male
        comX += leftFootCOM_X * leftFootMass;
        comZ += leftFootCOM_Z * leftFootMass;
        totalMassUsed += leftFootMass;

        // 7. RIGHT FOOT (1.37% for male)
        Landmark rightAnkle2 = worldLandmarks.get(28);      // Proximal end
        Landmark rightHeel = worldLandmarks.get(30);        // Heel
        Landmark rightToe = worldLandmarks.get(32);         // Toe
        double rightFootDistalX = rightHeel.x();
        double rightFootDistalZ = rightHeel.z();

        double rightFootCOM_X = rightAnkle2.x() + (rightFootDistalX - rightAnkle2.x()) * 0.4069;
        double rightFootCOM_Z = rightAnkle2.z() + (rightFootDistalZ - rightAnkle2.z()) * 0.4069;
        double rightFootMass = 0.0137 * totalMass;  // 1.37% for male
        comX += rightFootCOM_X * rightFootMass;
        comZ += rightFootCOM_Z * rightFootMass;
        totalMassUsed += rightFootMass;

        // Normalize by totalMassUsed, so we are not being biased
        double comX_final = comX / totalMassUsed;
        double comZ_final = comZ / totalMassUsed;
        com.add(comX_final);
        com.add(comZ_final);
        return com;
    }

    private List<Double> calculateCOMForFemale(double totalMass, List<Landmark> worldLandmarks) {
        List<Double> com = new ArrayList<>();
        double comX = 0;
        double comZ = 0;  // Using Z for depth (forward/backward in normalized space)
        double totalMassUsed = 0;

        // Trunk (approximate using shoulder and hip midpoints)
        Landmark leftShoulder = worldLandmarks.get(11);
        Landmark rightShoulder = worldLandmarks.get(12);
        Landmark leftHip = worldLandmarks.get(23);
        Landmark rightHip = worldLandmarks.get(24);

        // Shoulder midpoint (proximal end of trunk)
        double shoulderX = (leftShoulder.x() + rightShoulder.x()) / 2;
        double shoulderZ = (leftShoulder.z() + rightShoulder.z()) / 2;

        // Hip midpoint (distal end of trunk)
        double hipX = (leftHip.x() + rightHip.x()) / 2;
        double hipZ = (leftHip.z() + rightHip.z()) / 2;

        // Trunk COM = proximal + (distal - proximal) * COM_location_ratio
        // COM location ratio for female trunk: 0.4964 (from proximal)
        double trunkCOM_X = shoulderX + (hipX - shoulderX) * 0.4964;
        double trunkCOM_Z = shoulderZ + (hipZ - shoulderZ) * 0.4964;
        double trunkMass = 0.4257 * totalMass;
        comX += trunkCOM_X * trunkMass;
        comZ += trunkCOM_Z * trunkMass;
        totalMassUsed += trunkMass;

        // 2. LEFT THIGH (14.78% for female)
        Landmark leftHipJoint = worldLandmarks.get(23);    // Proximal end
        Landmark leftKnee = worldLandmarks.get(25);         // Distal end
        // COM location for female thigh: 0.3612 from proximal
        double leftThighCOM_X = leftHipJoint.x() + (leftKnee.x() - leftHipJoint.x()) * 0.3612;
        double leftThighCOM_Z = leftHipJoint.z() + (leftKnee.z() - leftHipJoint.z()) * 0.3612;
        double leftThighMass = 0.1478 * totalMass;
        comX += leftThighCOM_X * leftThighMass;
        comZ += leftThighCOM_Z * leftThighMass;
        totalMassUsed += leftThighMass;

        // 3. RIGHT THIGH (14.78% for female)
        Landmark rightHipJoint = worldLandmarks.get(24);    // Proximal end
        Landmark rightKnee = worldLandmarks.get(26);        // Distal end
        double rightThighCOM_X = rightHipJoint.x() + (rightKnee.x() - rightHipJoint.x()) * 0.3612;
        double rightThighCOM_Z = rightHipJoint.z() + (rightKnee.z() - rightHipJoint.z()) * 0.3612;
        double rightThighMass = 0.1478 * totalMass;
        comX += rightThighCOM_X * rightThighMass;
        comZ += rightThighCOM_Z * rightThighMass;
        totalMassUsed += rightThighMass;

        // 4. LEFT SHANK (4.81% for female)
        Landmark leftKnee2 = worldLandmarks.get(25);        // Proximal end
        Landmark leftAnkle = worldLandmarks.get(27);        // Distal end
        // COM location for female shank: 0.4352 from proximal
        double leftShankCOM_X = leftKnee2.x() + (leftAnkle.x() - leftKnee2.x()) * 0.4352;
        double leftShankCOM_Z = leftKnee2.z() + (leftAnkle.z() - leftKnee2.z()) * 0.4352;
        double leftShankMass = 0.0481 * totalMass;
        comX += leftShankCOM_X * leftShankMass;
        comZ += leftShankCOM_Z * leftShankMass;
        totalMassUsed += leftShankMass;

        // 5. RIGHT SHANK (4.81% for female)
        Landmark rightKnee2 = worldLandmarks.get(26);       // Proximal end
        Landmark rightAnkle = worldLandmarks.get(28);       // Distal end
        double rightShankCOM_X = rightKnee2.x() + (rightAnkle.x() - rightKnee2.x()) * 0.4352;
        double rightShankCOM_Z = rightKnee2.z() + (rightAnkle.z() - rightKnee2.z()) * 0.4352;
        double rightShankMass = 0.0481 * totalMass;
        comX += rightShankCOM_X * rightShankMass;
        comZ += rightShankCOM_Z * rightShankMass;
        totalMassUsed += rightShankMass;

        // 6. LEFT FOOT (1.29% for female)
        Landmark leftAnkle2 = worldLandmarks.get(27);       // Proximal end
        Landmark leftHeel = worldLandmarks.get(29);         // Heel
        Landmark leftToe = worldLandmarks.get(31);          // Toe
        // For foot, use ankle and toe (or heel) midpoint as distal
        // Using heel as distal for simplicity, or you can use toe
        double leftFootDistalX = leftHeel.x();
        double leftFootDistalZ = leftHeel.z();
        // Alternative: use midpoint of heel and toe
        // double leftFootDistalX = (leftHeel.x() + leftToe.x()) / 2;
        // double leftFootDistalZ = (leftHeel.z() + leftToe.z()) / 2;

        // COM location for female foot: 0.4014 from proximal
        double leftFootCOM_X = leftAnkle2.x() + (leftFootDistalX - leftAnkle2.x()) * 0.4014;
        double leftFootCOM_Z = leftAnkle2.z() + (leftFootDistalZ - leftAnkle2.z()) * 0.4014;
        double leftFootMass = 0.0129 * totalMass;
        comX += leftFootCOM_X * leftFootMass;
        comZ += leftFootCOM_Z * leftFootMass;
        totalMassUsed += leftFootMass;

        // 7. RIGHT FOOT (1.29% for female)
        Landmark rightAnkle2 = worldLandmarks.get(28);      // Proximal end
        Landmark rightHeel = worldLandmarks.get(30);        // Heel
        Landmark rightToe = worldLandmarks.get(32);         // Toe
        double rightFootDistalX = rightHeel.x();
        double rightFootDistalZ = rightHeel.z();

        double rightFootCOM_X = rightAnkle2.x() + (rightFootDistalX - rightAnkle2.x()) * 0.4014;
        double rightFootCOM_Z = rightAnkle2.z() + (rightFootDistalZ - rightAnkle2.z()) * 0.4014;
        double rightFootMass = 0.0129 * totalMass;
        comX += rightFootCOM_X * rightFootMass;
        comZ += rightFootCOM_Z * rightFootMass;
        totalMassUsed += rightFootMass;

        // Normalize by totalMassUsed, so we are not being biased
        double comX_final = comX / totalMassUsed;
        double comZ_final = comZ / totalMassUsed;
        com.add(comX_final);
        com.add(comZ_final);
        return com;
    }

    // Calculate angle between three points per one frame in time
    public static float calculateAngle(NormalizedLandmark a, NormalizedLandmark b, NormalizedLandmark c) {
        // Convert to vectors using x and y coordinates
        float[] ba = {a.x() - b.x(), a.y() - b.y()};
        float[] bc = {c.x() - b.x(), c.y() - b.y()};

        // Dot product
        float dot = ba[0]*bc[0] + ba[1]*bc[1];

        // Magnitudes
        float magBA = (float)Math.sqrt(ba[0]*ba[0] + ba[1]*ba[1]);
        float magBC = (float)Math.sqrt(bc[0]*bc[0] + bc[1]*bc[1]);

        // Angle in radians, then convert to degrees
        float angleRad = (float)Math.acos(dot / (magBA * magBC));
        return (float)Math.toDegrees(angleRad);
        // float angleDeg = (float)Math.toDegrees(angleRad);

        // // Ensure angle is between 0-180
        // return Math.abs(angleDeg) <= 180 ? Math.abs(angleDeg) : 360 - Math.abs(angleDeg);
    }

    private void returnLivestreamError(RuntimeException error) {
        if (poseLandmarkerHelperListener != null) {
            poseLandmarkerHelperListener.onError(
                    error.getMessage() != null ? error.getMessage() : "An unknown error has occurred"
            );
        }
    }

    // Inner classes for ResultBundle and LandmarkerListener
    /*
    Data Structure:
    ResultBundle {
    results: [  <- This list contains all frames!
        PoseLandmarkerResult { <-  Frame 1
            timestampMs: 0,
            landmarks: [33 keypoint coordinates]
        },
        PoseLandmarkerResult {  <- Frame 2
            timestampMs: 33,  <-Assuming 30fps
            landmarks: [33 keypoint coordinates]
        },
        PoseLandmarkerResult {  <- Frame 3
            timestampMs: 67,
            landmarks: [33 keypoint coordinates]
        },
        // ... All frames
    ]
}
     */
    public static class ResultBundle {
        public List<PoseLandmarkerResult> results;  // Contains all the coordinate data from frame
        public long inferenceTime;
        public int inputImageHeight;
        public int inputImageWidth;

        public ResultBundle(List<PoseLandmarkerResult> results, long inferenceTime,
                            int inputImageHeight, int inputImageWidth) {
            this.results = results;  // Store coordinate data
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }
    }

    public interface LandmarkerListener {
        void onError(String error, int errorCode);
        void onError(String error); // Overloaded method
        void onResults(ResultBundle resultBundle);  // Implemented inside of CameraFragment
    }
}