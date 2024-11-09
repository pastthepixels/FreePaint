package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;
import io.github.pastthepixels.freepaint.Graphics.Point;

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
        this.toolPaths.add(this.currentPath);
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
                this.currentPath.appearance.strokeSize = 2 * this.radius;
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
     * Turns the current path to a closed path by doing an expensive(!) operation where circles are added in place of each point.
     * Effectively "expands" the eraser path.
     */
    public Path expandEraser() {
        // 1. Create an "expanded" path and a circle that we'll "stamp" at each touch point.
        Path path = new Path();
        Point oldPoint = null;
        // 2. Loop through all points.
        for(Point point : currentPath.points) {
            // 1. Add circle for current point.
            Path circle = new Path();
            circle.addCircle(point.x, point.y, this.radius, Path.Direction.CCW);
            path.op(circle, Path.Op.UNION);
            // 2. Linear interpolate between current point and old point.
            if (oldPoint != null) {
                // Direction
                Point dir = point.subtract(oldPoint);
                float length = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y);
                dir = dir.multiply(1.f/length * this.radius);
                // Normal
                Point nor = new Point(dir.y, -dir.x);
                // Draw box between two circles
                Path inter = new Path();
                inter.moveTo(point.x + nor.x, point.y + nor.y);
                inter.lineTo(point.x - nor.x, point.y - nor.y);
                inter.lineTo(oldPoint.x - nor.x, oldPoint.y - nor.y);
                inter.lineTo(oldPoint.x + nor.x, oldPoint.y + nor.y);
                // Union
                path.op(inter, Path.Op.UNION);
            }
            // 3. Set oldPoint
            oldPoint = point;
        }
        return path;
    }

    /**
     * Loops through all paths, calling <code>path.erase</code>.
     * See <code>DrawPath.erase</code> for how this handles erasing from strokes/filled shapes.
     */
    public void eraseCurrentPath() {
        DrawPath currentPath = new DrawPath(expandEraser());
        for (DrawPath path : canvas.paths) {
            path.erase(currentPath);
            //path.cachePath();
        }
        currentPath.clear();
        init();
    }

    /**
     * Init left empty
     */
    public void init() {};

    public boolean allowVersionBackup() {
        return true;
    }

}