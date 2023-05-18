package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Region;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

// Erases a filled path region from paths, turning them into filled paths if necessary.
// Like a pencil tool from Illustrator, but one that removes.
// **Only works on finalized paths, and doesn't edit the list of points on a DrawPath!!**
public class EraserTool implements Tool {
    private final DrawAppearance appearance = new DrawAppearance(-1, Color.RED);
    private DrawPath currentPath;
    DrawCanvas canvas;

    public EraserTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    public void init() {}

    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Starts a new line in the path
                currentPath = new DrawPath();
                currentPath.appearance = appearance.clone();
                canvas.paths.add(currentPath);
                break;

            case MotionEvent.ACTION_MOVE:
                // Draws line between last point and this point
                currentPath.addPoint(event.getX(), event.getY());
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
        LinkedList<DrawPath> pathsToDelete = new LinkedList<DrawPath>();
        for(DrawPath path : canvas.paths) {
            if (path == currentPath) {
                path.appearance.stroke = Color.RED;
            } else {
                // TODO: Edit from https://stackoverflow.com/questions/11184397/path-intersection-in-android
                Region clip = new Region(0, 0, canvas.getHeight(), canvas.getWidth());
                Region region1 = new Region();
                region1.setPath(path.getPath(), clip);
                Region region2 = new Region();
                region2.setPath(currentPath.getPath(), clip);
                if (!region1.quickReject(region2) && region1.op(region2, Region.Op.INTERSECT)) {
                    path.getPath().op(currentPath.getPath(), Path.Op.DIFFERENCE);
                }
            }
        }
        // Remove all lines flagged for removal (so as to not interfere with
        // canvas.paths while we are looping through it)
        for(DrawPath path : pathsToDelete) {
            canvas.paths.remove(path);
        }
        canvas.paths.remove(currentPath);
    }
}
