package io.github.pastthepixels.freepaint.Graphics;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PointF;

import androidx.annotation.NonNull;

public class Point extends PointF {
    /**
     * Color for drawing -- currently used to signify when a path stops (red) and continues (green)
     * Works best with Paint.setBlendMode(BlendMode.EXCLUSION);
     */
    public int color = Color.WHITE;

    /**
     * Command for the point -- when we draw a line, we loop through all points. When we get to this point,
     * this lets us know if we should move to the coordinates, draw a line to the coordinates, or something else.
     */
    public COMMANDS command = COMMANDS.none;

    /**
     * Left or previous handle to the point, if it's in a curve.
     */
    private Point leftHandle = null;

    /**
     * Right or next handle to the point, if it's in a curve.
     */
    private Point rightHandle = null;

    /**
     * Constructor for Point.
     *
     * @param x X-coordinate as a float
     * @param y Y-coordinate as a float
     */
    public Point(float x, float y) {
        super(x, y);
    }

    public Point(float x, float y, COMMANDS command) {
        super(x, y);
        this.command = command;
    }

    public Point(float x, float y, COMMANDS command, int color) {
        super(x, y);
        this.command = command;
        this.color = color;
    }

    /**
     * Gets a right handle with coordinates in global space.
     *
     * @return a new Point
     */
    public Point getRightHandle() {
        if (this.command == COMMANDS.handle) {
            return null;
        } else {
            Point point = new Point(this.x, this.y);
            point.command = COMMANDS.handle;
            if (rightHandle != null) {
                point.add(rightHandle);
            }
            return point;
        }
    }

    /**
     * Sets the right handle of the point - this defines the curvature in a spline.
     *
     * @param rightHandle A handle with coordinates relative to the point.
     */
    public void setRightHandle(Point rightHandle) {
        this.rightHandle = rightHandle;
    }

    /**
     * Gets a left handle with coordinates in global space.
     *
     * @return a new Point
     */
    public Point getLeftHandle() {
        if (this.command == COMMANDS.handle) {
            return null;
        } else {
            Point point = new Point(this.x, this.y);
            point.command = COMMANDS.handle;
            if (leftHandle != null) {
                point.add(leftHandle);
            }
            return point;
        }
    }

    /**
     * Sets the left handle of the point - this defines the curvature in a spline.
     *
     * @param leftHandle A handle with coordinates relative to the point.
     */
    public void setLeftHandle(Point leftHandle) {
        this.leftHandle = leftHandle;
    }

    /**
     * Adds the coordinates of another point to the current point.
     * Returns the current point for single-line operations.
     *
     * @param point The point which has the coordinates you want to add.
     */
    public void add(Point point) {
        this.x += point.x;
        this.y += point.y;
    }

    /**
     * Subtracts the coordinates of another point to the current point, and applies changes to the current point.
     * Returns the current point for single-line operations.
     *
     * @param point The point which has the coordinates you want to subtract.
     * @return The point you are preforming the operation on (<code>this</code>)
     */
    public Point applySubtract(Point point) {
        this.x -= point.x;
        this.y -= point.y;
        return this;
    }

    /**
     * Subtracts the coordinates of another point to the current point.
     *
     * @param point The point which has the coordinates you want to subtract.
     * @return A new point that is the difference between the two points of concern.
     */
    public Point subtract(Point point) {
        return new Point(this.x - point.x, this.y - point.y);
    }


    /**
     * Divides the coordinates of another point to the current point, and applies changes to the current point.
     * Returns the current point for single-line operations.
     *
     * @param number The number which you want to divide both the x and y coordinates by
     */
    public void applyMultiply(float number) {
        this.x *= number;
        this.y *= number;
    }


    /**
     * Divides the coordinates of another point to the current point, and applies changes to the current point.
     *
     * @param scale The number which you want to divide both the x and y coordinates by
     * @return A new point that is scaled.
     */
    public Point multiply(float scale) {
        return new Point(this.x * scale, this.y * scale);
    }

    /**
     * Gets the shape associates with the path type
     */
    public Path getShape(float size) {
        return Shapes.diamondShape(x, y, size);
    }

    /**
     * Returns a new point with the same x and y coordinates as the current one.
     *
     * @return A cloned point.
     */
    @NonNull
    @Override
    public Point clone() {
        Point point = new Point(x, y, command, color);
        if (this.getLeftHandle() != null) point.setLeftHandle(this.getLeftHandle().applySubtract(this));
        if (this.getRightHandle() != null)
            point.setRightHandle(this.getRightHandle().applySubtract(this));
        return point;
    }

    // SVG point commands, with an extra command that defines handles. Only `none`, `move` and `line` are supported.
    public enum COMMANDS {none, move, line, horizontal, vertical, cubicBezier, smoothCubicBezier, handle}
}
