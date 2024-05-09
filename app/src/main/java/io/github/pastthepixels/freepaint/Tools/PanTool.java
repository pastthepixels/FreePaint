package io.github.pastthepixels.freepaint.Tools;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;
import io.github.pastthepixels.freepaint.Graphics.Point;

public class PanTool implements Tool {
    /**
     * Offset
     */
    public final Point offset = new Point(0f, 0f);
    /**
     * Offset that's applied separately to <code>offset</code>, to make sure panning is from the middle of the screen.
     */
    public final Point panOffset = new Point(0f, 0f);
    final DrawCanvas canvas;
    /**
     * Location of the last time an ACTION_DOWN touch was initialized (relative positions to that
     * are used for calculating new offsets)
     */
    private final PointF touchDown = new PointF(0, 0);
    /**
     * Old offset value, recorded before a new one is set
     */
    private final Point oldOffset = new Point(0, 0);
    private final ScaleGestureDetector detector;
    /**
     * Scale
     */
    public float scaleFactor = 1f;
    /**
     * Used in onTouchEvent.
     */
    boolean isScaling = false;
    boolean disableIsScalingOnNextUp = false;

    /**
     * Binds the tool to a DrawCanvas, and sets up a <code>ScaleGestureDetector</code>
     * to detect pinch zoom.
     *
     * @param canvas DrawCanvas to bind to the tool
     */
    public PanTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.detector = new ScaleGestureDetector(canvas.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private final Point lastFocus = new Point(0, 0);

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                lastFocus.set(
                        detector.getFocusX(),
                        detector.getFocusY()
                );
                return super.onScaleBegin(detector);
            }

            /**
             * This absolute bit of math just makes sure that zooming is from the center of the canvas.
             * Zooming is typically done from the top left corner, but we can use panOffset to move
             * the canvas by an amount to make it look like we zoomed from the center.
             */
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                updatePanOffset();
                canvas.invalidate();
                return true;
            }
        });
    }

    /**
     * Updates <code>panOffset</code> so that the canvas is moved to create the effect of
     * center zoom.
     */
    public void updatePanOffset() {
        panOffset.set(
                -(float) canvas.getWidth() * scaleFactor / 2 + ((float) canvas.getWidth() / 2),
                -(float) canvas.getHeight() * scaleFactor / 2 + ((float) canvas.getHeight() / 2)
        );
        panOffset.applyMultiply((float) 1./scaleFactor);
    }

    /**
     * Returns null. In the future it would be nice to draw text saying what the scale factor is.
     */
    public LinkedList<DrawPath> getToolPaths() {
        return null;
    }

    /**
     * Pans the canvas if one finger is on the screen, or zooms it if there's two.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Scaling
        detector.onTouchEvent(event);
        // This bit of code basically prevents edge cases where you're scrolling and let go of one finger
        // first instead of two at the first time -- the code would usually detect the remaining finger and
        // treat it the same way as a touch with just one finger (you intended to pan)
        if (detector.isInProgress()) {
            isScaling = true;
        } else {
            disableIsScalingOnNextUp = true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP && disableIsScalingOnNextUp) {
            disableIsScalingOnNextUp = false;
            isScaling = false;
        }
        // Panning
        if (!isScaling) {
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
            }
        }
        return true;
    }

    /**
     * Nothing to do when the tool is initialized, but this function is required
     * since PanTool implements Tool.
     */
    public void init() {

    }

    public boolean allowVersionBackup() {
        return false;
    }
}
