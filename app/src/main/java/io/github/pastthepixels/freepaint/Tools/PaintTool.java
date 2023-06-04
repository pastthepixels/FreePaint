package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

public class PaintTool implements Tool {
    private final LinkedList<DrawPath> toolPaths = new LinkedList<>();

    // The default appearance. We're going to be able to change this!
    private final DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    DrawCanvas canvas;
    private DrawPath currentPath;

    public PaintTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
    }

    /*
     * Nothing to do when we select the paint tool
     */
    public void init() {

    }

    /*
     * When the user puts a finger on the screen, we create a new DrawPath.
     * Then, as they move it, we add each point Android is able to poll to the path.
     * (This also means that path resolution == Android's native touch polling speed)
     * Once the user lifts their finger off the screen, we finalise that path's points.
     */
    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                appearance.loadFromSettings(canvas.getContext());
                // Starts a new line in the path -- whether or not it is closed is taken from the preferences (defaults to false)
                currentPath = new DrawPath();
                currentPath.isClosed = PreferenceManager.getDefaultSharedPreferences(canvas.getContext()).getBoolean("drawFilledShapes", false);
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
