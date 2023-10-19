package io.github.pastthepixels.freepaint;

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

    /**
     * Adds the coordinates of another point to the current point.
     * Returns the current point for single-line operations.
     *
     * @param point The point which has the coordinates you want to add.
     * @return The point you are preforming the operation on (<code>this</code>)
     */
    public Point add(Point point) {
        this.x += point.x;
        this.y += point.y;
        return this;
    }

    /**
     * Subtracts the coordinates of another point to the current point.
     * Returns the current point for single-line operations.
     *
     * @param point The point which has the coordinates you want to subtract.
     * @return The point you are preforming the operation on (<code>this</code>)
     */
    public Point subtract(Point point) {
        this.x -= point.x;
        this.y -= point.y;
        return this;
    }


    /**
     * Divides the coordinates of another point to the current point.
     * Returns the current point for single-line operations.
     *
     * @param number The number which you want to divide both the x and y coordinates by
     * @return The point you are preforming the operation on (<code>this</code>)
     */
    public Point divide(float number) {
        this.x /= number;
        this.y /= number;
        return this;
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
        return new Point(x, y);
    }

    // SVG point commands. Only `none`, `move` and `line` are supported.
    public enum COMMANDS {none, move, line, horizontal, vertical}
}
