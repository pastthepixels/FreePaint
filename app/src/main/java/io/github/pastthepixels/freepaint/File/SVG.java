package io.github.pastthepixels.freepaint.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.io.OutputStream;

import io.github.pastthepixels.freepaint.DrawAppearance;
import io.github.pastthepixels.freepaint.DrawCanvas;
import io.github.pastthepixels.freepaint.DrawPath;
import io.github.pastthepixels.freepaint.Point;

public class SVG {
    private String data = "";
    private DrawCanvas canvas;

    /*
     * Creates a new SVG instance, but we need a canvas with paths.
     * @param canvas A DrawCanvas instance
     */
    public SVG(DrawCanvas canvas) {
        this.canvas = canvas;
    }

    @SuppressLint("DefaultLocale")
    public void createSVG() {
        this.data = "";
        this.data += String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        this.data += String.format("<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">", (int)canvas.documentSize.x, (int)canvas.documentSize.y);
        for(DrawPath path : canvas.paths) {
            if (path.points.size() > 1) addPath(path);
        }
        // Closes the tag. We are done.
        this.data += "\n</svg>";
        System.out.println(this.data);
    }

    public void writeFile(Uri uri, AppCompatActivity activity) {
        try {
            OutputStream stream = activity.getContentResolver().openOutputStream(uri);
            stream.write(data.getBytes());
            stream.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("DefaultLocale")
    public void addPath(DrawPath path) {
        StringBuilder data = new StringBuilder("\n<path d=\"");
        // Step 1. Add points.
        for(Point point : path.points) {
            String command = point.command == Point.COMMANDS.move ? "M" : "L";
            if (point == path.points.get(0)) command = "M";
            data.append(String.format("%s%.2f %.2f ", command, point.x, point.y));
        }
        if (path.isClosed) {
            data.append("Z");
        }
        data.append("\" ");
        // Step 2. Set the appearance of the path.
        // Inkscape only accepts hex colors, I don't know why
        String fillValue = path.appearance.fill == -1? "none" : DrawAppearance.colorToHex(path.appearance.fill);
        String strokeValue = path.appearance.stroke == -1? "none" : DrawAppearance.colorToHex(path.appearance.stroke);
        data.append(String.format("fill=\"%s\" ", fillValue));
        data.append(String.format("stroke=\"%s\" ", strokeValue));
        if (path.appearance.fill != -1) data.append(String.format("fill-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.fill)));
        if (path.appearance.stroke != -1) data.append(String.format("stroke-opacity=\"%f\" ", DrawAppearance.getColorAlpha(path.appearance.stroke)));
        data.append(String.format("stroke-width=\"%s\" ", path.appearance.strokeSize));
        data.append("stroke-linecap=\"round\" ");
        // Done
        this.data += data + "/>";
    }


}
