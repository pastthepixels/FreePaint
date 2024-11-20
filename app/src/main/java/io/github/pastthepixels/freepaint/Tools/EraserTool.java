package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.ExtendedPath;
import io.github.pastthepixels.freepaint.Graphics.PathGenerator;

/**
 * Erases a filled path region from paths, turning them into filled paths if necessary.
 * Like a pencil tool from Illustrator, but one that removes.
 * **Only works on finalized paths, and doesn't edit the list of points on a DrawPath!!**
 */
public class EraserTool implements Tool {
    /**
     * The eraser path
     */
    private final PathGenerator currentPath = new PathGenerator();

    /**
     * The canvas
     */
    private final DrawCanvas canvas;

    /**
     * Radius of eraser path
     */
    private int radius = 25;

    /**
     * Init function, binds the tool to a canvas and sets a default appearance for the eraser path
     *
     * @param canvas The canvas to bind the tool to (paths will be sampled from/drawn on here)
     */
    public EraserTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.currentPath.appearance = new DrawAppearance(Color.RED, -1);
    }

    /**
     * Returns a list of paths entirely used by the tool for visual aid purposes so that it can be drawn by a DrawCanvas.
     * In this case, this draws the red "eraser" path and draws every path that can be erased as green.
     *
     * @return A list of paths for the DrawCanvas to draw
     */
    public LinkedList<ExtendedPath> getToolPaths() {
        LinkedList<ExtendedPath> paths = new LinkedList<>();
        paths.add(currentPath.getPath());
        return paths;
    }

    /**
     * Draws an eraser path, and when done (ACTION_UP) erases that path from any overlapping paths
     * See <code>EraserTool.eraseCurrentPath()</code>.
     *
     * @param event MotionEvent passed from a DrawCanvas
     * @return Boolean return value passed to a DrawCanvas
     */
    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Starts a new line in the path
                this.currentPath.appearance.strokeSize = (int) (2 * this.radius * (1/canvas.getScaleFactor()));
                currentPath.clear();
                break;

            case MotionEvent.ACTION_MOVE:
                // Draws line between last point and this point
                currentPath.addPoint(canvas.mapPoint(event.getX(), event.getY()));
                break;

            case MotionEvent.ACTION_UP:
                eraseCurrentPath();
                currentPath.clear();
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * Turns the current path to a closed path by "expanding" it.
     */
    public ExtendedPath expandEraser() {
        ExtendedPath path = new ExtendedPath();
        Paint paint = new Paint();
        currentPath.appearance.initialisePaint(paint, 1);
        paint.setStyle(Paint.Style.STROKE);
        paint.getFillPath(currentPath.getPath(), path);
        return path;
    }

    /**
     * Loops through all paths, calling <code>path.erase</code>.
     * See <code>DrawPath.erase</code> for how this handles erasing from strokes/filled shapes.
     */
    public void eraseCurrentPath() {
        ExtendedPath toErase = expandEraser();
        for (ExtendedPath path : canvas.paths) {
            path.erase(toErase);
        }
        currentPath.clear();
        init();
    }

    /**
     * Resets the tool
     */
    @Override
    public void reset() {
        currentPath.clear();
    }

    /**
     * Init left empty
     */
    public void init() {};

    public boolean allowVersionBackup() {
        return true;
    }

}