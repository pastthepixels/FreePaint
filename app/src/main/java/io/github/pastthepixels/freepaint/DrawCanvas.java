package io.github.pastthepixels.freepaint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
//import android.graphics.Point;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Tools.EraserTool;
import io.github.pastthepixels.freepaint.Tools.PaintTool;
import io.github.pastthepixels.freepaint.Tools.PanTool;
import io.github.pastthepixels.freepaint.Tools.Tool;

public final class DrawCanvas extends View {
    public enum TOOLS { none, paint, eraser, pan };

    private TOOLS tool = TOOLS.none;

    public Paint paint = new Paint();
    public LinkedList<DrawPath> paths = new LinkedList<DrawPath>();

    private PaintTool paintTool = new PaintTool(this);

    private EraserTool eraserTool = new EraserTool(this);
    private PanTool panTool = new PanTool(this);


    /*
     * Constructor
     */
    public DrawCanvas(Context context, @Nullable AttributeSet attrs, @Nullable int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initialisePaint();
    }

    public DrawCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawCanvas(Context context) {
        this(context, null, 0);
    }

    /*
     * Setting tools
     */
    public void setTool(TOOLS tool) {
        this.tool = tool;
        postInvalidate(); // Indicate view should be redrawn
    }

    /*
     * Initialises <code>paint</code> with a default configuration.
     */
    public void initialisePaint() {
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /*
     * Adds touch points when the user touches the screen
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Runs chosenTool.onTouchEvent if it exists, otherwise don't update the screen.
        if (tool == TOOLS.none || getTool().onTouchEvent(event) == false) {
            return false;
        } else {
            postInvalidate(); // Indicate view should be redrawn
            return true; // Indicate we've consumed the touch
        }
    }

    /*
     * Gets the chosen tool
     */
    public Tool getTool() {
        switch(tool) {
            case none:
                return null;
            case paint:
                return paintTool;
            case eraser:
                return eraserTool;
            case pan:
                return panTool;
        }
        return null;
    }

    public PointF mapPoint(float x, float y) {
        return new PointF((x / panTool.scaleFactor) - panTool.offset.x, (y / panTool.scaleFactor) - panTool.offset.y);
    }

    /*
     * onDraw
     */
    protected void onDraw(Canvas canvas) {
        // Allows us to do things like setting a custom background
        super.onDraw(canvas);
        // Draws things on the screen
        canvas.save();
        canvas.scale(panTool.scaleFactor, panTool.scaleFactor);
        canvas.translate(panTool.offset.x, panTool.offset.y);

        for(DrawPath path : paths) {
            paint.reset();
            path.draw(canvas, paint);
        }
        if (getTool() != null && getTool().getToolPaths() != null) {
            for (DrawPath path : getTool().getToolPaths()) {
                paint.reset();
                path.draw(canvas, paint);
            }
        }

        canvas.restore();
    }
}