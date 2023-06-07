package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region;
import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;
import io.github.pastthepixels.freepaint.Point;

public class SelectionTool implements Tool {
    private final DrawAppearance SELECTION_APPEARANCE = new DrawAppearance(Color.RED, Color.argb(100, 255, 0, 0));

    private final DrawAppearance TRANSFORMATION_APPEARANCE = new DrawAppearance(Color.GREEN, Color.argb(100, 0, 255, 0));

    private final LinkedList<DrawPath> toolPaths = new LinkedList<>();

    private final DrawPath currentPath = new DrawPath(null);

    public Point originalPoint = new Point(0, 0);

    public Point previousPoint = null;

    private final DrawCanvas canvas;

    private TOUCH_MODES mode;

    /**
     * Creates a SelectionTool instance, saying that the selection path has to be closed (it's a rectangle)
     *
     * @param canvas The DrawCanvas to bind to
     */
    public SelectionTool(DrawCanvas canvas) {
        this.canvas = canvas;
        currentPath.isClosed = true;
    }


    /**
     * Returns a list of paths entirely used by the tool for visual aid purposes
     * (e.g. showing selected paths) so that it can be drawn by a DrawCanvas
     *
     * @return A list of paths for the DrawCanvas to draw
     */
    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
    }

    /**
     * Every time we select the selection tool (heh), it clears the previous selection.
     * One of the reasons for doing this is that if we selected a path and its shape changed/it's no longer there,
     * we can be lazy and don't have to recompute a bounding box or check if it's still there.
     */
    public void init() {
        currentPath.clear();
        toolPaths.clear();
    }

    /**
     * When the user touches the canvas while the tool is selected:
     * (1) Create a new selection if the touch point is outside the current
     * (1a) When the user lifts their finger, resize the selection square to fit the selection,
     * effectively repurposing the square from showing the region to select to showing the
     * bounds of the selected paths.
     * (2) If the touch point is in in the selected square, move the selection.
     * (3) TODO: If the touch point is on the edges of the square (draw circle "handles" that can be used to determine this), scale the selection.
     *
     * @param event MotionEvent passed from the DrawCanvas
     * @return Boolean return value passed to the DrawCanvas
     */
    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If the touch action is outside the currently selected rectangle, we're not trying to manipulate it
                // -- we're trying to make a new one
                originalPoint = canvas.mapPoint(event.getX(), event.getY());
                if (!currentPath.contains(originalPoint)) {
                    currentPath.appearance = SELECTION_APPEARANCE;
                    mode = TOUCH_MODES.define;
                    toolPaths.clear();
                    toolPaths.add(currentPath);
                    currentPath.clear();
                } else {
                    currentPath.appearance = TRANSFORMATION_APPEARANCE;
                    mode = TOUCH_MODES.move;
                    previousPoint = null;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                Point touchPoint = canvas.mapPoint(event.getX(), event.getY());
                if (mode == TOUCH_MODES.define) {
                    // If we're trying to define a new selection, redraw the current path with the bounds
                    currentPath.clear();
                    currentPath.addPoint(originalPoint);
                    currentPath.addPoint(new Point(touchPoint.x, originalPoint.y));
                    currentPath.addPoint(touchPoint);
                    currentPath.addPoint(new Point(originalPoint.x, touchPoint.y));
                }
                if (mode == TOUCH_MODES.move && previousPoint != null) {
                    // If we're trying to move all the paths we selected... well, move them!
                    for (DrawPath path : toolPaths) {
                        path.translate(touchPoint.clone().subtract(previousPoint));
                        if (path != currentPath) path.finalise();
                    }
                }
                // Important for second if statement
                previousPoint = touchPoint.clone();
                break;

            case MotionEvent.ACTION_UP:
                if (mode == TOUCH_MODES.define) {
                    // If we're releasing our finger from selecting a bunch of paths, we need to
                    // do math to actually select those paths.
                    selectPaths();
                }
                mode = TOUCH_MODES.none;

            default:
                return false;
        }
        return true;
    }

    /**
     * Selects paths that overlap with the selection square, then resizes the square
     * so it now represents the bounds of the selection.
     */
    public void selectPaths() {
        Point startPoint = canvas.mapPoint(0, 0);
        Point endPoint = canvas.mapPoint(canvas.getWidth(), canvas.getHeight());
        Region clip = new Region(Math.round(startPoint.x), Math.round(startPoint.y), Math.round(endPoint.x), Math.round(endPoint.y));

        // Top left of a bounding box for all selections ('cause we're rebuilding currentPath after this!)
        Point boundsTop = null;
        // Bottom right
        Point boundsBottom = null;

        // Creates a Region from the current path to do bounding box math
        Region currentPathRegion = new Region();
        currentPathRegion.setPath(currentPath.generatePath(), clip);

        // Bounding box math! (If a path collides with the current path, add it to the selection.)
        for (DrawPath path : canvas.paths) {
            Region region = new Region();
            region.setPath(path.getPath(), clip);
            Rect bounds = region.getBounds();
            if (!region.quickReject(currentPathRegion) && region.op(currentPathRegion, Region.Op.INTERSECT)) {
                toolPaths.add(path);
                // Checks to see if the bounding box for all selections can be expanded.
                // Speaking of expanding things, you should click the minimise button the left for each if statement.
                if (boundsTop == null) {
                    boundsTop = new Point(bounds.left, bounds.top);
                }
                if (boundsBottom == null) {
                    boundsBottom = new Point(bounds.right, bounds.bottom);
                }
                if (bounds.top < boundsTop.y) {
                    boundsTop.y = bounds.top;
                }
                if (bounds.left < boundsTop.x) {
                    boundsTop.x = bounds.left;
                }
                if (bounds.bottom > boundsBottom.y) {
                    boundsBottom.y = bounds.bottom;
                }
                if (bounds.right > boundsBottom.x) {
                    boundsBottom.x = bounds.right;
                }
            }
        }

        // Yep, we are rebuilding the current path to reflect not the bounds the user selected,
        // but the bounds of the *paths* the user selected.
        currentPath.clear();
        if (boundsTop != null) {
            currentPath.addPoint(boundsTop);
            currentPath.addPoint(new Point(boundsBottom.x, boundsTop.y));
            currentPath.addPoint(boundsBottom);
            currentPath.addPoint(new Point(boundsTop.x, boundsBottom.y));
        }
    }

    /**
     * You can either define a new selection or move a selection. Each touch mode is set from
     * different conditions and reset once you lift your finger off the screen.
     */
    private enum TOUCH_MODES {none, define, move}
}
