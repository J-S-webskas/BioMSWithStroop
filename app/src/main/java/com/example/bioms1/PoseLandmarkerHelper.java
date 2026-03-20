package com.example.bioms1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private PoseLandmarker poseLandmarker;

    // todo: pass this from record fragment
    public static boolean preview = true;
    public static boolean countdown = false;

    public PoseLandmarkerHelper(Context context) {
        this(DEFAULT_POSE_DETECTION_CONFIDENCE,
                DEFAULT_POSE_TRACKING_CONFIDENCE,
                DEFAULT_POSE_PRESENCE_CONFIDENCE,
                MODEL_POSE_LANDMARKER_FULL,
                DELEGATE_CPU,
                RunningMode.IMAGE,
                context,
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
            LandmarkerListener poseLandmarkerHelperListener
    ) {
        this.minPoseDetectionConfidence = minPoseDetectionConfidence;
        this.minPoseTrackingConfidence = minPoseTrackingConfidence;
        this.minPosePresenceConfidence = minPosePresenceConfidence;
        this.currentModel = currentModel;
        this.currentDelegate = currentDelegate;
        this.runningMode = runningMode;
        this.context = context;
        this.poseLandmarkerHelperListener = poseLandmarkerHelperListener;

        this.setupPoseLandmarker();
    }

    public void clearPoseLandmarker() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
        }
        poseLandmarker = null;
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
            List<PoseLandmarkerResult> resultList = new ArrayList<>();
            resultList.add(result);  // Coordinate data of the current frame
            ResultBundle bundle = new ResultBundle(resultList, inferenceTime, input.getHeight(), input.getWidth());  // input.getHeight() and input.getWidth() get the pixel dimensions of the camera frame/image that was processed.
            poseLandmarkerHelperListener.onResults(bundle); // onResults is implemented inside of CameraFragment
            // Get the first (and only) PoseLandmarkerResult from the list
            List<NormalizedLandmark> landmarks = bundle.results.get(0).landmarks().get(0);  // first person's 33 landmarks all in here

            if (!preview && !countdown) {
                float left_hip = calculateAngle(landmarks.get(11), landmarks.get(23), landmarks.get(25));
                float right_knee = calculateAngle(landmarks.get(24), landmarks.get(26), landmarks.get(28));
                float left_knee = calculateAngle(landmarks.get(23), landmarks.get(25), landmarks.get(27));

                RecordFragment.addAnglePerFrame("right_knee", right_knee);
                RecordFragment.addAnglePerFrame("left_knee", left_knee);

                RecordFragment.HipBalanceaddAnglePerFrame("left_hip", left_hip);
                RecordFragment.KneeBalanceaddAnglePerFrame("left_knee", left_knee);
            }
        }
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