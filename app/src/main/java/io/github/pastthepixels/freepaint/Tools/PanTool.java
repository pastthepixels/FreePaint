package io.github.pastthepixels.freepaint.Tools;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

public class PanTool implements Tool {
    // Minimum and maximum scale factor

    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;

    // Scale
    public float scaleFactor = 1f;

    // Offset
    public PointF offset = new PointF(0f, 0f);

    // Location of the last time an ACTION_DOWN touch was initialized (relative positions to that
    // are used for calculating new offsets)

    private PointF touchDown = new PointF(0, 0);

    // Old offset value, recorded before a new one is set
    private PointF oldOffset = new PointF(0, 0);

    private ScaleGestureDetector detector;

    DrawCanvas canvas;

    public PanTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.detector = new ScaleGestureDetector(canvas.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
                canvas.invalidate();
                return true;
            }
        });
    }
    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Default amount of fingers to zoom (2) == center zoom
        detector.onTouchEvent(event);
        // 1 finger == pan
        if (event.getPointerCount() == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDown.set(event.getX(), event.getY());
                    oldOffset.set(offset);
                    break;

                case MotionEvent.ACTION_MOVE:
                    offset.set(
                        oldOffset.x - (touchDown.x - event.getX()) / scaleFactor,
                        oldOffset.y - (touchDown.y - event.getY()) / scaleFactor
                    );
                    break;

                case MotionEvent.ACTION_UP:
                    break;

                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public void init() {

    }
}
