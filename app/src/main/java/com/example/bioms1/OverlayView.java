package com.example.bioms1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import java.util.List;
import java.util.Set;

public class OverlayView extends View {

    private static final float LANDMARK_STROKE_WIDTH = 12f;

    @Nullable
    private PoseLandmarkerResult results;
    private Paint pointPaint = new Paint();
    private Paint linePaint = new Paint();

    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;

    public OverlayView(Context context) {
        super(context);
        init(context, null);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        initPaints();
    }

    public void clear() {
        results = null;
        pointPaint.reset();
        linePaint.reset();
        invalidate();
        initPaints();
    }

    private void initPaints() {
        linePaint.setColor(ContextCompat.getColor(getContext(), com.google.android.material.R.color.design_default_color_secondary));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (results != null) {
            List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>> landmarks =
                    results.landmarks();

            for (List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> landmark : landmarks) {
                // Draw points
                for (com.google.mediapipe.tasks.components.containers.NormalizedLandmark normalizedLandmark : landmark) {
                    canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                    );
                }

                // Draw lines
                // Draw lines - CORRECT WAY
                Set<Connection> connections = PoseLandmarker.POSE_LANDMARKS;
                for (Connection connection : connections) {
                    int startIdx = connection.start();    // These might be getStart() or start()
                    int endIdx = connection.end();        // These might be getEnd() or end()

                    if (landmark.size() > startIdx && landmark.size() > endIdx) {
                        canvas.drawLine(
                                landmark.get(startIdx).x() * imageWidth * scaleFactor,
                                landmark.get(startIdx).y() * imageHeight * scaleFactor,
                                landmark.get(endIdx).x() * imageWidth * scaleFactor,
                                landmark.get(endIdx).y() * imageHeight * scaleFactor,
                                linePaint
                        );
                    }
                }
            }
        }
    }

    public void setResults(
            PoseLandmarkerResult poseLandmarkerResults,
            int imageHeight,
            int imageWidth
    ) {
        setResults(poseLandmarkerResults, imageHeight, imageWidth, RunningMode.IMAGE);
    }

    public void setResults(
            PoseLandmarkerResult poseLandmarkerResults,
            int imageHeight,
            int imageWidth,
            RunningMode runningMode
    ) {
        this.results = poseLandmarkerResults;
        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;

        switch (runningMode) {
            case IMAGE:
            case VIDEO:
                scaleFactor = Math.min(
                        getWidth() * 1f / imageWidth,
                        getHeight() * 1f / imageHeight
                );
                break;
            case LIVE_STREAM:
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be displayed.
                scaleFactor = Math.max(
                        getWidth() * 1f / imageWidth,
                        getHeight() * 1f / imageHeight
                );
                break;
        }
        invalidate();
    }
}