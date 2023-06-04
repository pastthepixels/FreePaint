package io.github.pastthepixels.freepaint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;

import io.github.pastthepixels.freepaint.File.SVG;
import io.github.pastthepixels.freepaint.Tools.EraserTool;
import io.github.pastthepixels.freepaint.Tools.PaintTool;
import io.github.pastthepixels.freepaint.Tools.PanTool;
import io.github.pastthepixels.freepaint.Tools.SelectionTool;
import io.github.pastthepixels.freepaint.Tools.Tool;

public final class DrawCanvas extends View {
    private final PaintTool paintTool = new PaintTool(this);
    private final EraserTool eraserTool = new EraserTool(this);
    private final PanTool panTool = new PanTool(this);
    private final SelectionTool selectionTool = new SelectionTool(this);
    private final SVG svgHelper = new SVG(this);
    public Paint paint = new Paint();
    public LinkedList<DrawPath> paths = new LinkedList<DrawPath>();
    // Letter, portrait, at 100 PPI
    public Point documentSize = new Point(0, 0);
    public int documentColor = Color.WHITE;
    private TOOLS tool = TOOLS.none;
    /*
     * Constructor
     */
    public DrawCanvas(Context context, @Nullable AttributeSet attrs, @Nullable int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setFocusableInTouchMode(true);
        // Initialises documentSize with the size in the last used document
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        documentSize.set(
                Float.parseFloat(prefs.getString("documentWidth", "816")),
                Float.parseFloat(prefs.getString("documentHeight", "1056"))
        );
    }


    public DrawCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawCanvas(Context context) {
        this(context, null, 0);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerDocument();
    }

    private void centerDocument() {
        // Scales the canvas so that the document width takes up 80% of the screen width
        panTool.scaleFactor = (float) ((0.8) * (getWidth() / documentSize.x));
        panTool.updatePanOffset();
        panTool.updateScaleFactor();
        panTool.offset.set(
                (getWidth() / 2 - documentSize.x / 2),
                (getHeight() / 2 - documentSize.y / 2)
        );
    }

    public void saveFile(Uri uri) throws IOException {
        svgHelper.createSVG();
        svgHelper.writeFile(getContext().getContentResolver().openOutputStream(uri, "wt"));
    }

    @SuppressLint("DefaultLocale")
    public void loadFile(Uri uri) throws IOException {
        svgHelper.createSVG();
        svgHelper.loadFile(getContext().getContentResolver().openInputStream(uri));
        if (getTool() != null) getTool().init();
        centerDocument();
        // Sets settings for document width/height to new document size
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString("documentWidth", String.format("%d", (int) documentSize.x));
        editor.putString("documentHeight", String.format("%d", (int) documentSize.y));
        editor.apply();
    }

    /*
     * Adds touch points when the user touches the screen
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Runs chosenTool.onTouchEvent if it exists, otherwise don't update the screen.
        if (tool == TOOLS.none || !getTool().onTouchEvent(event)) {
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
        switch (tool) {
            case none:
                return null;
            case paint:
                return paintTool;
            case eraser:
                return eraserTool;
            case pan:
                return panTool;
            case select:
                return selectionTool;
        }
        return null;
    }

    /*
     * Setting tools
     */
    public void setTool(TOOLS tool) {
        this.tool = tool;
        if (tool != TOOLS.none) getTool().init();
        postInvalidate(); // Indicate view should be redrawn
    }

    public Point mapPoint(float x, float y) {
        return new Point(
                (x / panTool.scaleFactor) - panTool.offset.x - panTool.panOffset.x,
                (y / panTool.scaleFactor) - panTool.offset.y - panTool.panOffset.y
        );
    }

    public float getScaleFactor() {
        return panTool.scaleFactor;
    }

    /*
     * onDraw
     */
    protected void onDraw(Canvas canvas) {
        // Allows us to do things like setting a custom background
        super.onDraw(canvas);
        // Draws things on the screen
        canvas.save();
        // SCALES, THEN TRANSLATES (translations are independent of scales)
        canvas.scale(panTool.scaleFactor, panTool.scaleFactor);
        canvas.translate(panTool.offset.x + panTool.panOffset.x, panTool.offset.y + panTool.panOffset.y);
        // Draws what the page will look like
        paint.setColor(documentColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(12, 0, 0, Color.BLACK);
        RectF page = new RectF(0, 0, documentSize.x, documentSize.y);
        canvas.drawRect(page, paint);
        paint.reset();
        // Draws every path, then tool path
        for (DrawPath path : paths) {
            paint.reset();
            path.draw(canvas, paint, getScaleFactor());
        }
        if (getTool() != null && getTool().getToolPaths() != null) {
            for (DrawPath path : getTool().getToolPaths()) {
                paint.reset();
                path.draw(canvas, paint, getScaleFactor());
            }
        }

        canvas.restore();
    }

    public enum TOOLS {none, paint, eraser, pan, select}
}