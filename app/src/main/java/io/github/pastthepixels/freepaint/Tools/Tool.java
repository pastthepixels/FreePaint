package io.github.pastthepixels.freepaint.Tools;

import android.view.MotionEvent;

import java.util.LinkedList;

import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;

public interface Tool {
    public LinkedList<DrawPath> getToolPaths();
    public boolean onTouchEvent(MotionEvent event);
    public void init();
}
