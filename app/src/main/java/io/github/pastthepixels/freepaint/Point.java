package io.github.pastthepixels.freepaint;

import android.graphics.Color;
import android.graphics.PointF;

public class Point extends PointF {
    public int color = Color.BLACK;
    public COMMANDS command = COMMANDS.none;

    public Point(float x, float y) {
        super(x, y);
    }

    public Point add(Point point) {
        this.x += point.x;
        this.y += point.y;
        return this;
    }

    public Point subtract(Point point) {
        this.x -= point.x;
        this.y -= point.y;
        return this;
    }

    public Point divide(float number) {
        this.x /= number;
        this.y /= number;
        return this;
    }

    public Point clone() {
        return new Point(x, y);
    }

    // SVG point commands. Only `none`, `move` and `line` are supported.
    public enum COMMANDS {none, move, line, horizontal, vertical}
}
