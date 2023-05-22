package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Region;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;
import io.github.pastthepixels.freepaint.Point;

// Erases a filled path region from paths, turning them into filled paths if necessary.
// Like a pencil tool from Illustrator, but one that removes.
// **Only works on finalized paths, and doesn't edit the list of points on a DrawPath!!**
public class EraserTool implements Tool {
    private LinkedList<DrawPath> toolPaths = new LinkedList<DrawPath>();
    private final DrawAppearance appearance = new DrawAppearance(-1, Color.RED);
    private DrawPath currentPath;
    DrawCanvas canvas;

    public EraserTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
    }

    public void init() {}

    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Starts a new line in the path
                updateToolPaths();
                currentPath = new DrawPath();
                currentPath.appearance = appearance.clone();
                toolPaths.add(currentPath);
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

    public void eraseCurrentPath() {
        for(DrawPath path : canvas.paths) {
            path.erase(currentPath);
        }
        toolPaths.clear();
    }

    public void updateToolPaths() {
        toolPaths.clear();
        Point startPoint = canvas.mapPoint(0, 0);
        Point endPoint = canvas.mapPoint(canvas.getWidth(), canvas.getHeight());
        Region clip = new Region(Math.round(startPoint.x), Math.round(startPoint.y), Math.round(endPoint.x), Math.round(endPoint.y));
        for(DrawPath path : canvas.paths) {
            DrawPath cloned = new DrawPath(path.getPath());
            cloned.points = path.points;
            cloned.isClosed = path.isClosed;
            if (cloned.isClosed) {
                cloned.appearance = new DrawAppearance(-1, Color.GREEN);
            } else {
                cloned.appearance = new DrawAppearance(Color.GREEN, -1);
                cloned.drawPoints = true;
            }
            toolPaths.add(cloned);
        }
    }
}
