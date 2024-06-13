package io.github.pastthepixels.freepaint.Tools;

import android.graphics.Color;
import android.view.MotionEvent;

import java.util.LinkedList;
import java.util.Random;

import io.github.pastthepixels.freepaint.Graphics.DrawAppearance;
import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.Graphics.DrawPath;

public class SprayPaintTool implements Tool {
    private final DrawAppearance appearance = new DrawAppearance(Color.BLACK, -1);
    private final DrawCanvas canvas;
    private DrawPath currentPath;
    private Random random = new Random();

    public SprayPaintTool(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public LinkedList<DrawPath> getToolPaths() {
        return null;
    }

    public void init() {

    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Update the appearance's color from settings
                appearance.loadFromSettings(canvas.getContext());

                currentPath = new DrawPath(null);
                currentPath.appearance = appearance.clone();
                canvas.paths.add(currentPath);

                // Add multiple points around the touch point to create a spray paint effect
                for (int i = 0; i < 50; i++) {
                    // Use Gaussian distribution for offsets
                    float offsetX = (float) (random.nextGaussian() * 15);
                    float offsetY = (float) (random.nextGaussian() * 15);

                    // Calculate the distance from the center
                    float distance = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);

                    // Calculate the radius based on the distance
                    // The further the point is from the center, the smaller the radius
                    float radius = Math.max(0, 10 - distance); // Adjust these values as needed

                    // Add a new point with the calculated radius
                    currentPath.addPointSprayPaint(canvas.mapPoint(event.getX() + offsetX, event.getY() + offsetY), radius);
                }

                currentPath.finalise();
                currentPath.cachePath();
                break;

            case MotionEvent.ACTION_UP:
                break;

            default:
                return false;
        }
        return true;
    }
    public boolean allowVersionBackup() {
        return true;
    }
}