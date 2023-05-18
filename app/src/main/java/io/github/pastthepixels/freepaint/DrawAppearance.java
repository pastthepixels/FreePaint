package io.github.pastthepixels.freepaint;

import android.graphics.Color;
import android.graphics.Paint;

import org.jetbrains.annotations.Nullable;

public class DrawAppearance {
    public int stroke = -1;
    public int fill = -1;

    public DrawAppearance(int stroke, int fill) {
        this.stroke = stroke;
        this.fill = fill;
    }

    public DrawAppearance clone() {
        return new DrawAppearance(stroke, fill);
    }
}
