package io.github.pastthepixels.freepaint.Graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import io.github.pastthepixels.freepaint.Utils;

/**
 * Generates an ExtendedPath based on a set of points
 */
public class PathGenerator {
    /**
     * Appearance of the path to apply to generated paths.
     */
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);

    /**
     * List of points
     */
    public ArrayList<Point> points = new ArrayList<>();

    /**
     * Whether or not the path is closed (a line is drawn from the end point to the start point)
     */
    public boolean isClosed = false;

    /**
     * Epsilon value for the simplification algorithm (RDP)
     */
    public double simplificationAmount = 0;

    /**
     * Adds an instance of <code>io.github.pastthepixels.freepaint.Graphics.Point</code> to the list of points
     */
    public void addPoint(Point point) {
        points.add(point);
    }

    /**
     * Clears all points in a DrawPath, then resets <code>DrawPath.path</code>
     */
    public void clear() {
        points.clear();
    }

    /**
     * Generates a android.graphics.Path based on instructions on each of the DrawPath's points.
     *
     * @return A path generated from this.points.
     */
    public ExtendedPath getPath() {
        ExtendedPath path = new ExtendedPath();
        path.appearance = appearance;
        for (int i = 0; i < points.size(); i++) {
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
     * Applies operations to the <code>points</code> array to simplify and
     * smoothen lines after they are drawn.
     */
    public void apply() {
        // Simplifies the path.
        points = simplify(points, simplificationAmount);
        // Generates handles for each point.
        for (int i = 0; i < points.size(); i++) {
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
                // Set handles (left handle is mirrored; hermite splines!
                Point rightHandle = new Point(
                        ((next.x - prev.x) / 6),
                        ((next.y - prev.y) / 6)
                );
                point.setRightHandle(rightHandle);
                point.setLeftHandle(rightHandle.multiply(-1));
                // If the angles between the current point and the next point/current and previous are acute/right, make the corner sharp.
                double angle = Utils.angleBetweenVectors(prev.subtract(point), point.subtract(next));
                if (Math.abs(angle) >= Math.PI/2) { // idk how this works but it does. it shouldn't be this way.
                    point.setLeftHandle(new Point(0, 0 ));
                    point.setRightHandle(new Point(0, 0 ));
                }
            }
        }
    }

    /**
     * Simplifies points using the Ramer-Douglas-Peucker algorithm.
     * Adapted from the pseudocde from https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
     */
    private ArrayList<Point> simplify(ArrayList<Point> points, double epsilon) {
        if (points.isEmpty()) {
            return points;
        }
        double max_distance = 0;
        int index = 0;
        for (int i = 2; i < points.size() - 1; i++) {
            double distance = Utils.distanceFromPointToLine(points.get(0), points.get(points.size() - 1), points.get(i));
            if (distance > max_distance) {
                index = i;
                max_distance = distance;
            }
        }

        ArrayList<Point> simplified = new ArrayList<>();

        if (max_distance > epsilon) {
            // Like merge sort
            ArrayList<Point> leftHalf = simplify(new ArrayList<Point>(points.subList(0, index)), epsilon);
            ArrayList<Point> rightHalf = simplify(new ArrayList<Point>(points.subList(index, points.size())), epsilon);
            Point point = rightHalf.get(0).clone().applySubtract(leftHalf.get(leftHalf.size() - 1));
            leftHalf.remove(leftHalf.size() - 1);
            simplified.addAll(leftHalf);
            simplified.addAll(rightHalf);
        } else {
            simplified.add(points.get(0));
            simplified.add(points.get(points.size() - 1));
        }

        return simplified;
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
        pointPath.op(getPath(), Path.Op.DIFFERENCE);
        return pointPath.isEmpty();
    }

}
