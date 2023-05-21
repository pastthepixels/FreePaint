package io.github.pastthepixels.freepaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.LinkedList;

public class DrawPath {
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    public LinkedList<Point> points = new LinkedList<Point>();
    private Path path;

    public boolean isClosed = false;

    public boolean drawPoints = false;

    public void addPoint(float x, float y) {
        points.add(new Point(x, y));
    }

    public void addPoint(PointF point) {
        points.add(new Point(point.x, point.y));
    }

    // Constructors
    public DrawPath() { }


    public DrawPath(Path path) {
        this.path = path;
    }

    // Generates a basic path by connecting lines, ideal for previewing
    public Path generatePath() {
        Path path = new Path();
        for(Point point : points){
            if(point == points.get(0) || point.command == Point.COMMANDS.move){
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
    /*
     * Generates a "final" path by interpolating lines.
     * Right now, we are just using generatePath(). In the future, look at
     * something like https://www.stkent.com/2015/07/03/building-smooth-paths-using-bezier-curves.html
     * We need to interpolate between points, but unlike the default way of doing so, *also* contact each point.
     */
    public void finalise() {
        this.path = generatePath();
    }

    /*
     * Accessor for <code>path</code>
     * @return The <code>android.graphics.Path</code> instance used for drawing.
     */
    public Path getPath() {
        return path;
    }

    public void draw(Canvas canvas, Paint paint) {
        Path toDraw = path == null? generatePath() : path;
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
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
        if (drawPoints == true) {
            paint.setAlpha(100);
            for(Point pt : points) {
                paint.setColor(pt.color);
                Path path = new Path();
                path.addCircle(pt.x, pt.y, 5, Path.Direction.CW);
                canvas.drawPath(path, paint);
            }
        }
    }

    // Assumes `path` is closed.
    public void erase(DrawPath path) {
        if (isClosed) {
            getPath().op(path.getPath(), Path.Op.DIFFERENCE);
        } else {
            eraseFromStroke(path);
            finalise();
        }
    }

    // Assumes erasePath is closed.
    public void eraseFromStroke(DrawPath erasePath) {
        int index = 0;
        int intersectingPointsIndex = 0;
        boolean state = false; // All the points we are looking at (to our knowledge) don't collide with erasePath
        while(index < points.size()) {
            boolean oldState = state;
            Point point = points.get(index);
            if (erasePath.contains(point)) {
                points.remove(index);
                state = true;
            } else {
                state = false;
                index ++;
            }
            // If there's a STATE CHANGE
            if (oldState != state) {
                if (state == false) {
                    point.command = Point.COMMANDS.move;
                    point.color = Color.GREEN;
                } else if (index > 0) {
                    points.get(index - 1).color = Color.RED;
                }
            }
        }
    }

    /**
     * TODO: Change from https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
     * Return true if the given point is contained inside the boundary.
     * See: http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     * @param test The point to check
     * @return true if the point is inside the boundary, false otherwise
     *
     */
    public boolean contains(PointF test) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            if ((points.get(i).y > test.y) != (points.get(j).y > test.y) &&
                    (test.x < (points.get(j).x - points.get(i).x) * (test.y - points.get(i).y) / (points.get(j).y-points.get(i).y) + points.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }


}
class Point extends PointF {
    public int color = Color.BLACK;

    public enum COMMANDS {none, move, line};
    public COMMANDS command = COMMANDS.none;

    public Point(float x, float y) {
        super(x, y);
    }
}
