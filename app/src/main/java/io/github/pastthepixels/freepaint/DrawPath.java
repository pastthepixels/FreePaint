package io.github.pastthepixels.freepaint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import java.util.LinkedList;

public class DrawPath {
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    private LinkedList<Point> points = new LinkedList<Point>();
    private Path path;

    public void addPoint(float x, float y) {
        points.add(new Point(Math.round(x), Math.round(y)));
    }

    // Generates a basic path by connecting lines, ideal for previewing
    public Path generatePath() {
        Path path = new Path();
        for(Point point : points){
            if(point == points.get(0)){
                path.moveTo(point.x, point.y);
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        return path;
    }

    // Generates a "final" path by interpolating lines.
    // From https://stackoverflow.com/questions/8287949/android-how-to-draw-a-smooth-line-following-your-finger/8289516#8289516
    // need to refactor
    public void finalise() {
        Path path = new Path();
        boolean first = true;
        for(int i = 0; i < points.size(); i += 2){
            Point point = points.get(i);
            if(first){
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
        this.path = path;
    }

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
    }

}
