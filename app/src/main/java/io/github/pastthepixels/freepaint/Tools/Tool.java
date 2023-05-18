package io.github.pastthepixels.freepaint.Tools;

import android.view.MotionEvent;

import io.github.pastthepixels.freepaint.DrawCanvas;

public interface Tool {
    public boolean onTouchEvent(MotionEvent event);
    public void init();
}
