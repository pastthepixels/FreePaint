package io.github.pastthepixels.freepaint.Tools;

import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.ExtendedPath;
import io.github.pastthepixels.freepaint.Graphics.Point;

/**
 * TODO: zoom into corners of the screen
 * TODO: pan with two fingers without zooming
 */
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
     * ScaleGestureDetector
     */
    private final ScaleGestureDetector scaleDetector;
    /**
     * GestureDetector
     */
    private final GestureDetector gestureDetector;
    /**
     * Scale
     */
    public float scaleFactor = 1f;

    /**
     * Binds the tool to a DrawCanvas, and sets up a <code>ScaleGestureDetector</code>
     * to detect pinch zoom.
     *
     * @param canvas DrawCanvas to bind to the tool
     */
    public PanTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.scaleDetector = new ScaleGestureDetector(canvas.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private final Point lastFocus = new Point(0, 0);
            private final Point lastFocusMapped = new Point(0, 0);

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                lastFocus.set(
                        detector.getFocusX(),
                        detector.getFocusY()
                );
                lastFocusMapped.set(canvas.mapPoint(lastFocus.x, lastFocus.y));
                return super.onScaleBegin(detector);
            }

            /**
             * This absolute bit of math just makes sure that zooming is from the center of the canvas.
             * Zooming is typically done from the top left corner, but we can use panOffset to move
             * the canvas by an amount to make it look like we zoomed from the center.
             */
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                // Scale
                scaleFactor *= detector.getScaleFactor();
                Point newFocus = canvas.mapPoint(lastFocus.x, lastFocus.y);
                panOffset.set(
                        panOffset.x - (lastFocusMapped.x - newFocus.x),
                        panOffset.y - (lastFocusMapped.y - newFocus.y)
                );
                canvas.invalidate();
                return true;
            }
        });
        this.gestureDetector = new GestureDetector(canvas.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                offset.set(offset.x - distanceX / scaleFactor, offset.y - distanceY / scaleFactor);
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });
    }

    /**
     * Updates <code>panOffset</code> so that the canvas is moved to create the effect of
     * center zoom.
     */
    public void centerPanOffset() {
        panOffset.set(
                -(float) canvas.getWidth() * scaleFactor / 2 + ((float) canvas.getWidth() / 2),
                -(float) canvas.getHeight() * scaleFactor / 2 + ((float) canvas.getHeight() / 2)
        );
        panOffset.applyMultiply((float) 1./scaleFactor);
    }

    /**
     * Returns null. In the future it would be nice to draw text saying what the scale factor is.
     */
    public LinkedList<ExtendedPath> getToolPaths() {
        return null;
    }

    /**
     * Pans and zooms.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scrollEvent = gestureDetector.onTouchEvent(event);
        return scaleDetector.onTouchEvent(event) || scrollEvent;
    }

    @Override
    public void reset() {

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
