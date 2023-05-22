package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.graphics.Path;
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

    private LinkedList<DrawPath> toolPaths = new LinkedList<DrawPath>();

    private DrawPath currentPath = new DrawPath();

    DrawCanvas canvas;

    public SelectionTool(DrawCanvas canvas) {
        this.canvas = canvas;
        currentPath.isClosed = true;
    }

    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return toolPaths;
    }

    public void init() {
        currentPath.clear();
        toolPaths.clear();
    }

    public Point originalPoint = new Point(0, 0);

    public Point previousPoint = null;

    private enum TOUCH_MODES {none, define, move}

    private TOUCH_MODES mode;

    public boolean onTouchEvent(MotionEvent event) {
        // Checks for the event that occurs
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If the touch action is outside the currently selected rectangle, we're not trying to manipulate it
                // -- we're trying to make a new one
                originalPoint = canvas.mapPoint(event.getX(), event.getY());
                if (currentPath.contains(originalPoint) == false) {
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
                    currentPath.clear();
                    currentPath.addPoint(originalPoint);
                    currentPath.addPoint(new Point(touchPoint.x, originalPoint.y));
                    currentPath.addPoint(touchPoint);
                    currentPath.addPoint(new Point(originalPoint.x, touchPoint.y));
                }
                if (mode == TOUCH_MODES.move && previousPoint != null) {
                    for(DrawPath path : toolPaths) {
                        path.translate(touchPoint.clone().subtract(previousPoint));
                        if (path != currentPath) path.finalise();
                    }
                }
                previousPoint = touchPoint.clone();
                break;

            case MotionEvent.ACTION_UP:
                if (mode == TOUCH_MODES.define) {
                    selectPaths();
                }
                mode = TOUCH_MODES.none;

            default:
                return false;
        }
        return true;
    }

    public void selectPaths() {
        Point startPoint = canvas.mapPoint(0, 0);
        Point endPoint = canvas.mapPoint(canvas.getWidth(), canvas.getHeight());
        Region clip = new Region(Math.round(startPoint.x), Math.round(startPoint.y), Math.round(endPoint.x), Math.round(endPoint.y));

        // Top left
        Point boundsTop = null;
        // Bottom right
        Point boundsBottom = null;

        Region region2 = new Region();
        region2.setPath(currentPath.generatePath(), clip);

        for(DrawPath path : canvas.paths) {
            // TODO: Edit from https://stackoverflow.com/questions/11184397/path-intersection-in-android
            Region region1 = new Region();
            region1.setPath(path.getPath(), clip);
            Rect bounds = region1.getBounds();

            if (!region1.quickReject(region2) && region1.op(region2, Region.Op.INTERSECT)) {
                toolPaths.add(path);
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

        currentPath.clear();
        if (boundsTop != null && boundsBottom != null) {
            currentPath.addPoint(boundsTop);
            currentPath.addPoint(new Point(boundsBottom.x, boundsTop.y));
            currentPath.addPoint(boundsBottom);
            currentPath.addPoint(new Point(boundsTop.x, boundsBottom.y));
        }
    }

    public void updateToolPaths() {
        /*
        toolPaths.clear();
        PointF startPoint = canvas.mapPoint(0, 0);
        PointF endPoint = canvas.mapPoint(canvas.getWidth(), canvas.getHeight());
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
        }*/
    }
}
