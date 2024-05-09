package io.github.pastthepixels.freepaint;

import io.github.pastthepixels.freepaint.Graphics.Point;

public class Utils {
    public static double distanceFromPointToLine(Point lineStart, Point lineEnd, Point test) {
        double numerator = Math.abs((lineEnd.x - lineStart.x) * (test.y - lineStart.y) - (test.x - lineStart.x) * (lineEnd.y - lineStart.y));
        double denominator = Math.sqrt(Math.pow(lineEnd.x - lineStart.x, 2) + Math.pow(lineEnd.y - lineStart.y, 2));
        return numerator / denominator;
    }

    /**
     * Gets the point of collision between two lines, each defined by two points on the line.
     * Precondition: both lines must intersect.
     * @param aStart
     * @param aEnd
     * @param bStart
     * @param bEnd
     * @return A Point where both lines intersect
     */
    public static Point collisionBetweenLines(Point aStart, Point aEnd, Point bStart, Point bEnd) {
        double detA = aStart.x * aEnd.y - aStart.y * aEnd.x;
        double detB = bStart.x * bEnd.y - bStart.y * bEnd.x;
        Point diffA = aStart.subtract(aEnd);
        Point diffB = bStart.subtract(bEnd);
        double denom = diffA.x * diffB.y - diffA.y * diffB.x;
        return new Point(
                (float) ((detA * diffB.x - diffA.x * detB) / denom),
                (float) ((detA * diffB.y - diffA.y * detB) / denom)
        );
    }

    /**
     * Returns the angle between two vectors in radians.
     * @param vecA
     * @param vecB
     * @return The angle between two vectors, radians
     */
    public static double angleBetweenVectors(Point vecA, Point vecB) {
        return Math.acos((vecA.x * vecB.x + vecA.y * vecB.y) / (vecA.length() * vecB.length()));
    }
}
