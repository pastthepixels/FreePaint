package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;

/**
 * Erases a filled path region from paths, turning them into filled paths if necessary.
 * Like a pencil tool from Illustrator, but one that removes.
 * **Only works on finalized paths, and doesn't edit the list of points on a DrawPath!!**
 */
public class EraserTool implements Tool {
    /**
     * List of paths to redraw, where we highlight points.
     */
    private final LinkedList<DrawPath> toolPaths = new LinkedList<>();

    /**
     * The eraser path
     */
    private final DrawPath currentPath = new DrawPath(null);

    /**
     * The canvas
     */
    private final DrawCanvas canvas;

    /**
     * Init function, binds the tool to a canvas and sets a default appearance for the eraser path
     *
     * @param canvas The canvas to bind the tool to (paths will be sampled from/drawn on here)
     */
    public EraserTool(DrawCanvas canvas) {
        this.canvas = canvas;
        this.currentPath.appearance = new DrawAppearance(-1, Color.RED);
    }

    /**
     * Returns a list of paths entirely used by the tool for visual aid purposes so that it can be drawn by a DrawCanvas.
     * In this case, this draws the red "eraser" path and draws every path that can be erased as green.
     *
     * @return A list of paths for the DrawCanvas to draw
     */
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
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
                currentPath.clear();
                break;

            case MotionEvent.ACTION_MOVE:
                // Draws line between last point and this point
                currentPath.addPoint(canvas.mapPoint(event.getX(), event.getY()));
                break;

            case MotionEvent.ACTION_UP:
                currentPath.finalise();
                eraseCurrentPath();
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * Loops through all paths, calling <code>path.erase</code>.
     * See <code>DrawPath.erase</code> for how this handles erasing from strokes/filled shapes.
     */
    public void eraseCurrentPath() {
        for (DrawPath path : canvas.paths) {
            path.erase(currentPath);
            path.cachePath();
        }
        currentPath.clear();
        init();
    }

    /**
     * Initialises by building a list of DrawPaths which have their points highlighted
     * and saves this to toolPaths.
     */
    public void init() {
        toolPaths.clear();
        for (DrawPath path : canvas.paths) {
            DrawPath cloned = new DrawPath(path.getPath());
            cloned.points = path.points;
            cloned.isClosed = path.isClosed;
            if (cloned.isClosed) {
                cloned.appearance = new DrawAppearance(-1, Color.GREEN);
            } else {
                cloned.appearance = new DrawAppearance(Color.GREEN, -1);
                cloned.appearance.strokeSize = 1;
                cloned.appearance.useDP = true;
                cloned.drawPoints = true;
            }
            toolPaths.add(cloned);
        }
        toolPaths.add(currentPath);
    }

    public boolean allowVersionBackup() {
        return true;
    }

}