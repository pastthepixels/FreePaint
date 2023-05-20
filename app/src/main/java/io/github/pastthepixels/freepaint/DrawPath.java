package io.github.pastthepixels.freepaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Region;

import java.util.LinkedList;

public class DrawPath {
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    private LinkedList<Point> points = new LinkedList<Point>();
    private Path path;

    public boolean isClosed = false;

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

    // Generates a "final" path by interpolating lines.
    // From https://stackoverflow.com/questions/8287949/android-how-to-draw-a-smooth-line-following-your-finger/8289516#8289516
    // need to refactor
    public void finalise() {
        this.path = generatePath();
        /*Path path = new Path();
        boolean first = true;
        for(int i = 0; i < points.size(); i += 2){
            Point point = points.get(i);
            if(first || point.command == Point.COMMANDS.move){
                first = false;
                path.moveTo(point.x, point.y);
            }

            else if(i < points.size() - 1){
                Point next = points.get(i + 1);
                path.quadTo(point.x, point.y, next.x, next.y);
            }
            else{
                path.lineTo(point.x, point.y);
            }
        }
        if (isClosed) {
            path.close();
        }
        this.path = path;*/
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




        PathMeasure pm = new PathMeasure(getPath(), false);
        //coordinates will be here

        float num_pts = points.size() - 1;

        float length = pm.getLength();
        System.out.println(length);
        float distance = 0f;
        float speed = 10f;
        float[] aCoordinates = new float[2];

        /*while (distance <= (length + speed)) {
            // get point from the path
            pm.getPosTan(distance, aCoordinates, null);
            //
            Path path = new Path();
            path.addCircle(aCoordinates[0], aCoordinates[1], 5, Path.Direction.CW);
            canvas.drawPath(path, paint);
            //
            distance = distance + speed;

        }*/

        for(Point pt : points) {
            //if (pt.color != Color.BLACK) {
                paint.setColor(pt.color);
                Path path = new Path();
                path.addCircle(pt.x, pt.y, 5, Path.Direction.CW);
                canvas.drawPath(path, paint);
            //}
        }
    }

    // Assumes `path` is closed.
    public void erase(DrawPath path, Region clip) {
        if (isClosed) {
            getPath().op(path.getPath(), Path.Op.DIFFERENCE);
        } else {
            Region region = new Region();
            region.setPath(path.getPath(), clip);
/*
            LinkedList<Point> toDelete = new LinkedList<Point>();
            for(Point point : points) {
                if (region.contains((int)point.x, (int)point.y)) {
                    toDelete.add(point);
                }
            }
            for(Point point : toDelete) {
                points.remove(point);
            }
*/
            //getIntersectingPoints(path);
            eraseFromStroke(path);
            finalise();
        }
    }

    // Assumes erasePath is closed.
    public void eraseFromStroke(DrawPath erasePath) {
        int index = 0;
        int intersectingPointsIndex = 0;
        boolean state = false; // All the points we are looking at (to our knowledge) don't collide with erasePath
        //LinkedList<Point> intersectingPoints = getIntersectingPoints(erasePath);
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
            if (oldState != state /*&& intersectingPointsIndex < intersectingPoints.size()*/) {
                if (state == false) {
                    point.command = Point.COMMANDS.move;
                    point.color = Color.GREEN;
                } else if (index > 0) {
                    points.get(index - 1).color = Color.RED;
                }
                //points.add(intersectingPoints.get(intersectingPointsIndex));
                //intersectingPointsIndex ++;
            }
        }
    }

    public LinkedList<Point> getIntersectingPoints(DrawPath path) {
        LinkedList<Point> intersectingPoints = new LinkedList<Point>();
        PathMeasure pm = new PathMeasure(getPath(), false);
        //coordinates will be here

        float length = pm.getLength();
        float distance = 0f;
        float speed = 10f;
        float[] aCoordinates = new float[2];


        Point lastGoodPoint = null;
        boolean isContains = false;

    //    points.clear();
        while (distance <= (length + speed)) {
            // get point from the path
            pm.getPosTan(distance, aCoordinates, null);
            //
            //if(region.contains((int)aCoordinates[0], (int)aCoordinates[1]) == isContains) {
            if (path.contains(new PointF(aCoordinates[0], aCoordinates[1])) == isContains) {
                lastGoodPoint = new Point(aCoordinates[0], aCoordinates[1]);
                //if (path.contains(new PointF(aCoordinates[0], aCoordinates[1]))) lastGoodPoint.color = Color.GREEN;
    //            /*if (isContains == false)*/ points.add(lastGoodPoint);
            } else /* if (lastGoodPoint != null)*/ {
                //if (isContains == false) points.removeLast();
                if (lastGoodPoint != null) {
                    lastGoodPoint.color = Color.GREEN;
                    intersectingPoints.add(lastGoodPoint);
                }
                //lastGoodPoint.command = isContains == false? Point.COMMANDS.line : Point.COMMANDS.move;
                //points.add(lastGoodPoint);//intersectingPoints.add(lastGoodPoint);
                isContains = !isContains;
            }
            //
            distance = distance + speed;
        }

        System.out.println(intersectingPoints.size());

        return intersectingPoints;
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
