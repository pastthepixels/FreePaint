package io.github.pastthepixels.freepaint;

import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.LinkedList;

import dev.romainguy.graphics.path.PathIterator;
import dev.romainguy.graphics.path.PathSegment;
import dev.romainguy.graphics.path.Paths;


public class DrawPath {
    /**
     * Appearance of the path (ex. fill/stroke color, stroke width)
     */
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);

    /**
     * List of points
     */
    public LinkedList<Point> points = new LinkedList<>();

    /**
     * Whether or not the path is closed (a line is drawn from the end point to the start point)
     */
    public boolean isClosed = false;

    /**
     * Whether or not to draw each point as a circle.
     */
    public boolean drawPoints = false;

    /**
     * android.graphics.Path instance. FreePaint handles math but this is how we get that math to be shown on the screen.
     */
    private Path path;

    /**
     * Constructor for DrawPath
     */
    public DrawPath(Path path) {
        this.path = path;
    }

    /**
     * Adds an instance of <code>io.github.pastthepixels.freepaint.Point</code> to the list of points
     */
    public void addPoint(Point point) {
        points.add(point);
    }

    /**
     * Generates a basic path by connecting lines, ideal for previewing
     *
     * @return A generated path
     */
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

    /**
     * Clears all points in a DrawPath, then resets <code>DrawPath.path</code>
     */
    public void clear() {
        points.clear();
        this.path = null;
    }

    /**
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

    /**
     * Accessor for <code>path</code>
     *
     * @return The <code>android.graphics.Path</code> instance used for drawing.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Draws the path.
     *
     * @param canvas      The canvas to draw to.
     * @param paint       The Paint instance to use -- this code is built for reusing the same one so memory can be saved.
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
            paint.setBlendMode(BlendMode.EXCLUSION);
            paint.setAlpha(100);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5.0f / scaleFactor);
            for (Point pt : points) {
                paint.setColor(pt.color);
                canvas.drawCircle(pt.x, pt.y, 5.0f / scaleFactor, paint);
            }
        }
    }

    /**
     * Erases a path from another path -- assumes `path` is closed.
     *
     * @param path The path to erase.
     */
    public void erase(DrawPath path) {
        // If there's no path to erase we can't do an erasing operation 💀
        if (getPath() == null) {
            return;
        }
        if (path.getPath() == null) {
            path.finalise();
        }
        if (isClosed) {
            getPath().op(path.getPath(), Path.Op.DIFFERENCE);
            regeneratePoints();
        } else {
            eraseFromStroke(path);
            finalise();
        }
    }

    /**
     * Regenerates points[] from DrawPath.path (android.graphics.Path)
     */
    public void regeneratePoints() {
        PathIterator iterator = Paths.iterator(path);
        points.clear();
        float[] pointArray = new float[8];
        while (iterator.hasNext()) {
            PathSegment.Type type = iterator.next(pointArray, 0); // The type of segment
            if (type != PathSegment.Type.Close) {
                Point point = new Point(pointArray[0], pointArray[1]);
                point.command = type == PathSegment.Type.Move ? Point.COMMANDS.move : Point.COMMANDS.line;
                points.add(point);
            }
        }
    }

    /**
     * Erases a closed path from a stroke by removing points in the stroke that are in contact with the
     * filled shape
     *
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

    /**
     * Translates all points in a path by an amount, in pixels.
     *
     * @param by The amount to translate all points in the DrawPath by
     */
    public void translate(Point by) {
        for (Point point : points) {
            point.add(by);
        }
    }

    /**
     * Point-shape collisions. This should be better than other implementations because by using Path.op we can account
     * for cases where getPath() returns a path with curves instead of a polygon with straight lines!
     * We create a test path with a circle of radius of 1, and then do Point.op with that and the current path.
     * <b>Prioritizes using a generated path, but if it doesn't exist will use generate()</b>
     *
     * @param point The point to test
     * @return Whether or not <code>point</code> is inside of the DrawPath's path.
     */
    public boolean contains(Point point) {
        Path pointPath = new Path();
        pointPath.addCircle(point.x, point.y, 1, Path.Direction.CW);
        pointPath.op(getPath() == null? generatePath() : getPath(), Path.Op.DIFFERENCE);
        return pointPath.isEmpty();
    }


}
