package io.github.pastthepixels.freepaint.Tools;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import androidx.annotation.NonNull;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

public class PanTool implements Tool {

    public float scaleFactor = 1.f;
    private ScaleGestureDetector detector;

    DrawCanvas canvas;

    public PanTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.detector = new ScaleGestureDetector(canvas.getContext(), new OnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                return false;
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                return false;
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {

            }
        });
    }
    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        return true;
    }

    @Override
    public void init() {

    }
}
