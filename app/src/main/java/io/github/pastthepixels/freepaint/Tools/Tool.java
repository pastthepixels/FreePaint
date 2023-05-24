package io.github.pastthepixels.freepaint.Tools;

import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawPath;

public interface Tool {
    // Returns a list of paths entirely used by the tool for visual aid purposes
    // (e.g. showing selected paths) so that it can be drawn by a DrawCanvas
    LinkedList<DrawPath> getToolPaths();

    // When the user touches a DrawCanvas with this tool as the selected one, run this.
    boolean onTouchEvent(MotionEvent event);

    // Every time this tool is selected, run this.
    void init();
}
