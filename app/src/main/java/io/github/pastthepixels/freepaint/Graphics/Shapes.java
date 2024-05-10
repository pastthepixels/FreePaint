package io.github.pastthepixels.freepaint.Graphics;

import android.graphics.Path;

public class Shapes {
    public static Path diamondShape(float x, float y, float size) {
        Path path = new Path();
        path.moveTo(x, y - size);
        path.lineTo(x + size, y);
        path.lineTo(x, y + size);
        path.lineTo(x - size, y);
        path.close();
        return path;
    }
}
