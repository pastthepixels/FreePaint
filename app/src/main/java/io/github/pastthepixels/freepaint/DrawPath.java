package io.github.pastthepixels.freepaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.LinkedList;

import dev.romainguy.graphics.path.PathIterator;
import dev.romainguy.graphics.path.PathSegment;
import dev.romainguy.graphics.path.Paths;


public class DrawPath {
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    public LinkedList<Point> points = new LinkedList<>();
    public boolean isClosed = false;
    public boolean drawPoints = false;
    private Path path;

    // Constructors
    public DrawPath() {
    }

    public DrawPath(Path path) {
        this.path = path;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    // Generates a basic path by connecting lines, ideal for previewing
    public Path generatePath() {
        Path path = new Path();
        for (Point point : points) {
            if (point == points.get(0) || point.command == Point.COMMANDS.move) {
                path.moveTo(point.x, point.y);
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        if (isClosed) {
            path.close();
        }
        return path;
    }

    // Clears all points
    public void clear() {
        points.clear();
        this.path = null;
    }

    /*
     * Generates a "final" path by interpolating lines.
     * Right now, we are just using generatePath(). In the future, look at
     * something like https://www.stkent.com/2015/07/03/building-smooth-paths-using-bezier-curves.html
     * We need to interpolate between points, but unlike the default way of doing so, *also* contact each point.
     */
    public void finalise() {
        /*
        Path path = new Path();
        for(int i = 0; i < points.size(); i ++){
            Point point = points.get(i);
            if(i == 0 || point.command == Point.COMMANDS.move){
                path.moveTo(point.x, point.y);
            } else if(i < points.size() - 1 && points.get(i + 1).command != Point.COMMANDS.move) {
                Point next = points.get(i + 1);
                path.quadTo(point.x, point.y, next.x, next.y);
                i ++;
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        if (isClosed) {
            path.close();
        }
        this.path = path;
        */
        this.path = generatePath();
    }

    /*
     * Accessor for <code>path</code>
     * @return The <code>android.graphics.Path</code> instance used for drawing.
     */
    public Path getPath() {
        return path;
    }

    /*
     * Draws the path.
     * @param canvas The canvas to draw to.
     * @param paint The Paint instance to use -- this code is built for reusing the same one so memory can be saved.
     * @param scaleFactor Necessary so we can draw the dots for points to always be the same size
     */
    public void draw(Canvas canvas, Paint paint, float scaleFactor) {
        Path toDraw = path == null ? generatePath() : path;
        // Sets a configuration for the Paint with DrawPath.appearance
        appearance.initialisePaint(paint);
        // Fills, then...
        if (appearance.fill != -1) {
            paint.setColor(appearance.fill);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(toDraw, paint);
        }
        // Strokes
        if (appearance.stroke != -1) {
            paint.setColor(appearance.stroke);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(toDraw, paint);
        }
        // If enabled, draw points on top of everything else
        if (drawPoints) {
            paint.setAlpha(100);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f / scaleFactor);
            for (Point pt : points) {
                paint.setColor(pt.color);
                canvas.drawCircle(pt.x, pt.y, 5.0f / scaleFactor, paint);
            }
        }
    }

    /*
     * Erases a path from another path -- assumes `path` is closed.
     * @param path The path to erase.
     */
    public void erase(DrawPath path) {
        if (isClosed) {
            getPath().op(path.getPath(), Path.Op.DIFFERENCE);
            regeneratePoints();
        } else {
            eraseFromStroke(path);
            finalise();
        }
    }

    /*
     * Regenerates points[] from DrawPath.path (android.graphics.Path)
     */
    public void regeneratePoints() {
        PathIterator iterator = Paths.iterator(path);
        points.clear();
        float[] pointArray = new float[8];
        while (iterator.hasNext()) {
            PathSegment.Type type = iterator.next(pointArray, 0); // The type of segment
            if(type != PathSegment.Type.Close) {
                Point point = new Point(pointArray[0], pointArray[1]);
                point.command = type == PathSegment.Type.Move? Point.COMMANDS.move : Point.COMMANDS.line;
                points.add(point);
            }
        }
    }

    /*
     * Erases a closed path from a stroke by removing points in the stroke that are in contact with the
     * filled shape
     * @param erasePath The path to erase.
     */
    public void eraseFromStroke(DrawPath erasePath) {
        int index = 0;
        boolean state = false; // All the points we are looking at (to our knowledge) don't collide with erasePath
        while (index < points.size()) {
            boolean oldState = state;
            Point point = points.get(index);
            if (erasePath.contains(point)) {
                points.remove(index);
                state = true;
            } else {
                state = false;
                index++;
            }
            // If there's a STATE CHANGE
            if (oldState != state) {
                if (!state) {
                    point.command = Point.COMMANDS.move;
                    point.color = Color.GREEN;
                } else if (index > 0) {
                    points.get(index - 1).color = Color.RED;
                }
            }
        }
    }

    /*
     * Translates all points in a path by an amount, in pixels.
     * @param by The amount to translate all points in the DrawPath by
     */
    public void translate(Point by) {
        for (Point point : points) {
            point.add(by);
        }
    }

    /**
     * TODO: Change from <a href="https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon">...</a>
     * Return true if the given point is contained inside the boundary.
     * See: <a href="http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html">...</a>
     *
     * @param test The point to check
     * @return true if the point is inside the boundary, false otherwise
     */
    public boolean contains(PointF test) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            if ((points.get(i).y > test.y) != (points.get(j).y > test.y) &&
                    (test.x < (points.get(j).x - points.get(i).x) * (test.y - points.get(i).y) / (points.get(j).y - points.get(i).y) + points.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }


}
