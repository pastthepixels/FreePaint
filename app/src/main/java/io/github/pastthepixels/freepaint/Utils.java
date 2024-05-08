package io.github.pastthepixels.freepaint;

import io.github.pastthepixels.freepaint.Graphics.Point;

public class Utils {
    public static double distanceFromPointToLine(Point lineStart, Point lineEnd, Point test) {
        double numerator = Math.abs((lineEnd.x - lineStart.x) * (test.y - lineStart.y) - (test.x - lineStart.x) * (lineEnd.y - lineStart.y));
        double denominator = Math.sqrt(Math.pow(lineEnd.x - lineStart.x, 2) + Math.pow(lineEnd.y - lineStart.y, 2));
        return numerator / denominator;
    }
}
