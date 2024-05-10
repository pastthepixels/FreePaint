package io.github.pastthepixels.freepaint.Graphics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

import io.github.pastthepixels.freepaint.File.SVG;
import io.github.pastthepixels.freepaint.MainActivity;
import io.github.pastthepixels.freepaint.Tools.EraserTool;
import io.github.pastthepixels.freepaint.Tools.PaintTool;
import io.github.pastthepixels.freepaint.Tools.PanTool;
import io.github.pastthepixels.freepaint.Tools.SelectionTool;
import io.github.pastthepixels.freepaint.Tools.Tool;

public final class DrawCanvas extends View {

    public final Paint paint = new Paint();
    // Stores previous "versions" of DrawCanvas.paths you can restore
    // You can move back and forth between this, but every time you create a new change
    // it removes everything after the current index (solving the grandfather paradox, btw)
    public final ArrayList<LinkedList<DrawPath>> versions = new ArrayList<>();
    public final int MAX_VERSIONS = 256;
    public final Point documentSize = new Point(0, 0);
    private final PaintTool paintTool = new PaintTool(this);
    private final EraserTool eraserTool = new EraserTool(this);
    private final PanTool panTool = new PanTool(this);
    private final SelectionTool selectionTool = new SelectionTool(this);
    private final SVG svgHelper = new SVG(this);
    public LinkedList<DrawPath> paths = new LinkedList<>();
    public int documentColor = Color.WHITE;
    private int version_index = -1;
    private TOOLS tool = TOOLS.none;

    // Drawing flags
    // Draws only the document, without any tool paths, or any rotation/translation.
    private boolean drawMinimal = false;

    /**
     * Constructor
     */
    public DrawCanvas(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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

    /**
     * Constructor
     */
    public DrawCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     */
    public DrawCanvas(Context context) {
        this(context, null, 0);
    }

    /**
     * Re-centers document when the size of the View changes
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerDocument();
    }

    /**
     * Changes the pan tool's scale factor and offset so that the document is in the middle of the screen.
     */
    private void centerDocument() {
        // Scales the canvas so that the document width takes up 80% of the screen width
        panTool.scaleFactor = (float) ((0.8) * (getWidth() / documentSize.x));
        panTool.updatePanOffset();
        panTool.offset.set(
                ((float) (getWidth()) / 2 - documentSize.x / 2),
                ((float) (getHeight()) / 2 - documentSize.y / 2)
        );
    }

    /**
     * Saves a DrawCanvas as an SVG at a URI
     *
     * @param uri URI of the SVG
     * @throws IOException May be thrown if the file path is invalid or the program can't write to it
     */
    public void saveFile(Uri uri) throws IOException {
        svgHelper.createSVG();
        svgHelper.writeFile(Objects.requireNonNull(getContext().getContentResolver().openOutputStream(uri, "wt")));
    }

    /**
     * Loads path data from an SVG
     *
     * @param uri URI of the SVG
     * @throws IOException May be thrown if the file path is invalid or the program can't read from it.
     */
    @SuppressLint("DefaultLocale")
    public void loadFile(Uri uri) throws IOException {
        // Clear path list/history
        paths.clear();
        versions.clear();
        version_index = -1;
        // Load the file
        svgHelper.createSVG();
        svgHelper.loadFile(getContext().getContentResolver().openInputStream(uri));
        if (getTool() != null) getTool().init();
        centerDocument();
        // Sets settings for document width/height to new document size
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editor.putString("documentWidth", String.format("%d", (int) documentSize.x));
        editor.putString("documentHeight", String.format("%d", (int) documentSize.y));
        editor.apply();
        // Save everything in the version history
        versions.add(cloneDrawPathList(paths));
        version_index += 1;
    }

    /**
     * Adds touch points when the user touches the screen.
     *
     * @param event The motion event.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Runs chosenTool.onTouchEvent if it exists, otherwise don't update the screen.
        if (tool == TOOLS.none || !Objects.requireNonNull(getTool()).onTouchEvent(event)) {
            return false;
        } else {
            if (getTool().allowVersionBackup() && event.getAction() == MotionEvent.ACTION_UP) {
                // Remove any edits after the current.
                while (versions.size() > version_index + 1) {
                    versions.remove(versions.size() - 1);
                }
                versions.add(cloneDrawPathList(paths)); // adds to the end ∴ newest changes are at the end of the list
                System.out.println(versions + " " + versions.size());
                if (versions.size() < MAX_VERSIONS - 1) version_index += 1;
                if (versions.size() > MAX_VERSIONS)
                    versions.remove(0); // delete the oldest change if the list has grown too much
            }
            postInvalidate(); // Indicate view should be redrawn
            return true; // Indicate we've consumed the touch
        }
    }

    /**
     * (deep) Clones a list of DrawPaths.
     * TODO: Instead of making a new list, with pointers to the same DrawPaths, clone those DrawPaths (deep clone the list).
     *       This is so that if you erase a part of a path, and modify it, you can undo that.
     *
     * @param listToClone The list you want to clone.
     * @return A deep cloned version of the list.
     */
    public LinkedList<DrawPath> cloneDrawPathList(LinkedList<DrawPath> listToClone) {
        LinkedList<DrawPath> list = new LinkedList<>();
        for (DrawPath pathToClone : listToClone) {
            list.add(pathToClone.clone());
        }
        return list;
    }

    /**
     * Undoes an operation by resetting DrawCanvas.paths to what it looked like after the previous operation.
     */
    public void undo() {
        if (version_index > 0) {
            System.out.println(versions.toString() + (version_index - 1));
            version_index -= 1;
            paths = cloneDrawPathList(versions.get(version_index));
        } else {
            version_index = -1;
            paths.clear();
        }
        // Force redraw
        postInvalidate();
        // Re-initialise tools
        if (tool == TOOLS.eraser) getTool().init();
    }

    /**
     * Redoes an operation by setting DrawCanvas.paths to what it looked like after an operation you undid to.
     */
    public void redo() {
        if (version_index < versions.size() - 1) {
            version_index += 1;
            paths = cloneDrawPathList(versions.get(version_index));
        } else if (version_index <= 0 && !versions.isEmpty()) {
            version_index = 0;
            paths = cloneDrawPathList(versions.get(0));
        }
        // Force redraw
        postInvalidate();
        // Re-initialise tools
        if (tool == TOOLS.eraser) getTool().init();
    }

    /**
     * Gets a bitmap from a DrawCanvas.
     */
    public Bitmap toBitmap() {
        Bitmap bitmap = Bitmap.createBitmap((int) this.documentSize.x, (int) this.documentSize.y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        this.drawMinimal = true;
        this.draw(canvas);
        this.drawMinimal = false;
        return bitmap;
    }

    /**
     * Gets the chosen tool
     *
     * @return A Tool instance, depending on DrawCanvas.tool
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

    /**
     * Setting tools
     *
     * @param tool Not a Tool instance, but rather from an Enum at <code>DrawCanvas.TOOLS</code>
     */
    public void setTool(TOOLS tool) {
        this.tool = tool;
        if (tool != TOOLS.none && getTool() != null) {
            getTool().init();
        }
        postInvalidate(); // Indicate view should be redrawn
    }

    /**
     * Maps a point from screen coordinates to Canvas coordinates (accounts for transformations)
     *
     * @param x X of the screen point (top left corner = origin point)
     * @param y Y of the screen point (top left corner = origin point)
     * @return New Point instance for the point in Canvas coordinates (has its own origin point)
     */
    public Point mapPoint(float x, float y) {
        return new Point(
                (x / panTool.scaleFactor) - panTool.offset.x - panTool.panOffset.x,
                (y / panTool.scaleFactor) - panTool.offset.y - panTool.panOffset.y
        );
    }

    /**
     * Gets the pan tool's scale factor.
     *
     * @return The scale of the pan tool (1 == 1x)
     */
    public float getScaleFactor() {
        return panTool.scaleFactor;
    }

    /**
     * Gets the pan tool's offset
     *
     * @return The offset of the pan tool
     */
    public Point getPosition() {
        return panTool.offset;
    }

    /**
     * Draws the document rectangle, paths, and then custom paths that tools create (ex. to show bounds of a selection)
     *
     * @param canvas the canvas on which the background will be drawn
     */
    protected void onDraw(@NonNull Canvas canvas) {
        // Allows us to do things like setting a custom background
        super.onDraw(canvas);
        float screenDensity = getResources().getDisplayMetrics().density;
        //
        ((MainActivity) getContext()).updateInfoBar();
        // Draws things on the screen
        canvas.save();
        // SCALES, THEN TRANSLATES (translations are independent of scales)
        if (!drawMinimal) {
            canvas.scale(panTool.scaleFactor, panTool.scaleFactor);
            canvas.translate(panTool.offset.x + panTool.panOffset.x, panTool.offset.y + panTool.panOffset.y);
        }
        // Draws what the page will look like
        paint.setColor(documentColor);
        paint.setStyle(Paint.Style.FILL);
        if (!drawMinimal) {
            paint.setShadowLayer(12, 0, 0, Color.argb(200, 0, 0, 0));
        }
        canvas.drawRect(0, 0, documentSize.x, documentSize.y, paint);
        paint.reset();
        // Draws a stroke for the page
        if (!drawMinimal) {
            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(5 / panTool.scaleFactor); // Always five pixels no matter scale
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, 0, documentSize.x, documentSize.y, paint);
            paint.reset();
        }
        // Draws every path, then tool path
        for (DrawPath path : paths) {
            paint.reset();
            path.draw(canvas, paint, screenDensity, getScaleFactor());
        }
        if (!drawMinimal && getTool() != null && getTool().getToolPaths() != null) {
            if (getTool() instanceof EraserTool) {
                paint.setARGB(150, 0, 0, 0);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawPaint(paint);
            }
            for (DrawPath path : getTool().getToolPaths()) {
                paint.reset();
                path.draw(canvas, paint, screenDensity, getScaleFactor());
            }
        }

        canvas.restore();
    }

    public enum TOOLS {none, paint, eraser, pan, select}
}