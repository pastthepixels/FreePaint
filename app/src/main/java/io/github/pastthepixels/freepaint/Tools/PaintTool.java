package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

public class PaintTool implements Tool {
    private LinkedList<DrawPath> toolPaths = new LinkedList<DrawPath>();
    private final DrawAppearance appearance = new DrawAppearance(Color.BLACK, /*Color.RED*/-1);
    private DrawPath currentPath;
    DrawCanvas canvas;

    public PaintTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
    }

    public void init() {

    }

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
                currentPath.addPoint(canvas.mapPoint(event.getX(), event.getY()));
                break;

            case MotionEvent.ACTION_UP:
                currentPath.finalise();
                break;

            default:
                return false;
        }
        return true;
    }
}
