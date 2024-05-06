package io.github.pastthepixels.freepaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;

import dev.romainguy.graphics.path.PathIterator;
import dev.romainguy.graphics.path.PathSegment;
import dev.romainguy.graphics.path.Paths;


public class DrawPath {
    // watch about adding new variables because you have to add them to the clone function at the bottom!

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
     * Clears all points in a DrawPath, then resets <code>DrawPath.path</code>
     */
    public void clear() {
        points.clear();
        this.path = null;
    }

    /**
     * Generates a basic path by connecting lines, ideal for previewing
     *
     * @return A generated path
     */
    public Path generatePath() {
        Path path = new Path();
        for(int i = 0; i < points.size(); i ++) {
            Point point = points.get(i);
            if (i == 0 || point.command == Point.COMMANDS.move) {
                path.moveTo(point.x, point.y);
            } else {
                Point prev = points.get(i - 1);
                path.cubicTo(
                        prev.getRightHandle().x,
                        prev.getRightHandle().y,
                        point.getLeftHandle().x,
                        point.getLeftHandle().y,
                        point.x,
                        point.y
                );
            }
        }
        if (isClosed) {
            path.close();
        }
        return path;
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
     * Returns a cached path, or generates a new one.
     */
    private Path getPathOrGenerate() {
        if (getPath() != null) {
            return getPath();
        } else {
            return generatePath();
        }
    }

    /**
     * Caches generatePath() into a thing we can reuse (dp)
     */
    public void cachePath() {
        this.path = generatePath();
    }

    /**
     * Applies operations to the <code>points</code> array to simplify and
     * smoothen lines after they are drawn.
     */
    public void finalise() {
        // Simplifies the path. TODO
        /*
        for(int i = 0; i < points.size(); i ++) {
            for(int j = i; j < points.size(); j ++) {
                // TODO: 3 is hard-coded
                if(j != i && Math.sqrt(Math.pow(points.get(i).x - points.get(j).x, 2) + Math.pow(points.get(i).y - points.get(j).y, 2)) < 20) {
                    points.remove(j);
                }
            }
        }
        */
        // Generates handles for each point.
        for(int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            if (i == 0) {
                Point next = points.get(i + 1);
                point.setRightHandle(new Point(
                        ((next.x - point.x) / 3),
                        ((next.y - point.y) / 3)
                ));
            } else if (i != points.size() - 1) {
                Point prev = points.get(i - 1);
                Point next = points.get(i + 1);
                point.setRightHandle(new Point(
                        ((next.x - prev.x) / 6),
                        ((next.y - prev.y) / 6)
                ));
                // Left handle is mirrored.
                point.setLeftHandle(new Point(
                        -((next.x - prev.x) / 6),
                        -((next.y - prev.y) / 6)
                ));
            }
        }
    }

    /**
     * Draws the path.
     *
     * @param canvas      The canvas to draw to.
     * @param paint       The Paint instance to use -- this code is built for reusing the same one so memory can be saved.
     * @param scaleFactor Necessary so we can draw the dots for points to always be the same size
     */
    public void draw(Canvas canvas, Paint paint, float screenDensity, float scaleFactor) {
        Path toDraw = getPath();
        if (toDraw == null) {
            toDraw = generatePath();
        }
        // Sets a configuration for the Paint with DrawPath.appearance
        appearance.initialisePaint(paint, screenDensity / scaleFactor);
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
            // Laggy but provides good contrast
            //paint.setBlendMode(BlendMode.EXCLUSION);
            paint.setStrokeWidth(screenDensity / scaleFactor);
            for (Point pt : points) {
                Path shape = pt.getShape(6 * screenDensity / scaleFactor);
                // Fill
                paint.setColor(pt.color);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(shape, paint);
                // Stroke
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(shape, paint);
                // HANDLES
                paint.setColor(pt.color);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPath(pt.getLeftHandle().getShape(4 * screenDensity / scaleFactor), paint);
                canvas.drawPath(pt.getRightHandle().getShape(4 * screenDensity / scaleFactor), paint);
                // HANDLE LINES
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(pt.getLeftHandle().x, pt.getLeftHandle().y, pt.x, pt.y, paint);
                canvas.drawLine(pt.getRightHandle().x, pt.getRightHandle().y, pt.x, pt.y, paint);
                // Done
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
        if (isClosed) {
            getPath().op(path.generatePath(), Path.Op.DIFFERENCE);
            regeneratePoints();
        } else {
            eraseFromStroke(path);
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
        pointPath.op(getPathOrGenerate(), Path.Op.DIFFERENCE);
        return pointPath.isEmpty();
    }

    /**
     * Deep clones a DrawPath.
     *
     * @return A cloned version of the DrawPath.
     */
    @NonNull
    @Override
    public DrawPath clone() {
        DrawPath cloned = new DrawPath(new Path());
        // 1. Copy variables.
        cloned.drawPoints = drawPoints;
        cloned.isClosed = isClosed;
        cloned.appearance = appearance.clone();
        // 2. Copy points.
        for (Point point : points) {
            cloned.points.add(point.clone());
        }
        cloned.cachePath();
        return cloned;
    }

}
