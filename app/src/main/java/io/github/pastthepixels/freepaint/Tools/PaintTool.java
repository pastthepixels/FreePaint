package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;

public class PaintTool implements Tool {
    /**
     * The default appearance. You can change this! (through settings)
     */
    private final DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    private final DrawCanvas canvas;
    private DrawPath currentPath;

    /**
     * Constructor for PaintTool, which binds itself to a DrawCanvas
     *
     * @param canvas DrawCanvas to bind to
     */
    public PaintTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Returns nothing as we don't draw custom tool paths
     */
    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return null;
    }

    /**
     * Nothing to do when we select the paint tool (required because of <code>Tool</code>)
     */
    public void init() {

    }

    /**
     * When the user puts a finger on the screen, we create a new DrawPath.
     * Then, as they move it, we add each point Android is able to poll to the path.
     * (This also means that path resolution == Android's native touch polling speed)
     * Once the user lifts their finger off the screen, we finalise that path's points.
     *
     * @param event MotionEvent passed from a DrawCanvas
     * @return Boolean return value passed to a DrawCanvas
     */
    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                appearance.loadFromSettings(canvas.getContext());
                // Starts a new line in the path -- whether or not it is closed is taken from the preferences (defaults to false)
                currentPath = new DrawPath(null);
                currentPath.simplificationAmount = Double.parseDouble(PreferenceManager.getDefaultSharedPreferences(canvas.getContext()).getString("simplificationAmount", "0"));
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
                currentPath.cachePath();
                break;

            default:
                return false;
        }
        return true;
    }

    public boolean allowVersionBackup() {
        return true;
    }
}
