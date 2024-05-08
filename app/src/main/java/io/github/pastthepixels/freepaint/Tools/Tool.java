package io.github.pastthepixels.freepaint.Tools;

import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.Graphics.DrawPath;

public interface Tool {
    /**
     * Returns a list of paths entirely used by the tool for visual aid purposes
     * (e.g. showing selected paths) so that it can be drawn by a DrawCanvas
     *
     * @return A list of paths for the DrawCanvas to draw
     */
    LinkedList<DrawPath> getToolPaths();

    /**
     * When the user touches a DrawCanvas with this tool as the selected one, run this.
     *
     * @param event MotionEvent passed from a DrawCanvas
     * @return Boolean return value passed to a DrawCanvas
     */
    boolean onTouchEvent(MotionEvent event);

    /**
     * Whether to make a backup of DrawCanvas.paths when your finger is lifted from the screen with this tool.
     */
    boolean allowVersionBackup();

    /**
     * Every time this tool is selected, run this.
     */
    void init();
}
