package io.github.pastthepixels.freepaint.Graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;

public class ExtendedPath extends Path {
    /** Contains instructions on how to draw the path */
    public DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);

    /** Whether the path is an open stroke or a closed path. Defaults to true. */
    private boolean isClosed = false;

    /** Wrapper for close */
    public void close() {
        super.close();
        this.isClosed = true;
    }

    /** Constructor with no arguments */
    public ExtendedPath() {
        super();
    }

    /** Constructor using an ExtendedPath */
    public ExtendedPath(ExtendedPath path) {
        super((Path) path);
        this.isClosed = path.isClosed;
        this.appearance = path.appearance;
    }

    /**
     * Draws the path.
     *
     * @param canvas      The canvas to draw to.
     * @param paint       The Paint instance to use -- this code is built for reusing the same one so memory can be saved.
     * @param scaleFactor Necessary so we can draw the dots for points to always be the same size
     */
    public void draw(Canvas canvas, Paint paint, float screenDensity, float scaleFactor) {
        // Sets a configuration for the Paint with DrawPath.appearance
        appearance.initialisePaint(paint, screenDensity / scaleFactor);
        // Fills, then...
        if (appearance.fill != -1) {
            paint.setColor(appearance.fill);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(this, paint);
        }
        // Strokes
        if (appearance.stroke != -1) {
            paint.setColor(appearance.stroke);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(this, paint);
        }
    }


    /**
     * Erases a path from another path -- assumes `path` is closed.
     *
     * @param path The path to erase.
     */
    public void erase(ExtendedPath path) {
        Path erased = new ExtendedPath(this);
        // Expand strokes
        if (!this.isClosed) {
            Paint paint = new Paint();
            appearance.initialisePaint(paint, 1);
            if (appearance.fill != -1) {
                paint.setColor(appearance.fill);
                paint.setStyle(Paint.Style.FILL);
            }
            // Strokes
            if (appearance.stroke != -1) {
                paint.setColor(appearance.stroke);
                paint.setStyle(Paint.Style.STROKE);
            }
            paint.getFillPath(erased, erased);
        }
        // Erase
        boolean op = erased.op(path, Path.Op.DIFFERENCE);
        if (op) {
            if (!this.isClosed) {
                appearance.strokeSize = 0;
                appearance.fill = appearance.stroke;
                appearance.stroke = -1;
            }
            this.isClosed = true;
            this.set(erased);
        }
    }

    /**
     * Translates a path by a Point
     */
    public void translate(Point by) {
        this.offset(by.x, by.y);
    }

    /**
     * Clones an ExtendedPath.
     *
     * @return A cloned version of the ExtendedPath.
     */
    @NonNull
    @Override
    public ExtendedPath clone() {
        return new ExtendedPath(this);
    }
}
