package io.github.pastthepixels.freepaint.Graphics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class DrawAppearance {
    public EFFECTS effect = EFFECTS.none;
    public int stroke;
    public int fill;
    public int strokeSize = 5;
    // If this is set to true, stroke size is measured in dp instead of px
    public boolean useDP = false;

    // Constructor with just stroke/fill (integer colors)
    public DrawAppearance(int stroke, int fill) {
        this.stroke = stroke;
        this.fill = fill;
    }

    // Constructor with every option
    public DrawAppearance(int stroke, int fill, int strokeSize) {
        this.strokeSize = strokeSize;
        this.stroke = stroke;
        this.fill = fill;
    }

    /*
     * Converts an integer color to CSS RGBA
     * @param color The integer color value, ARGB
     * @return The color as an RGBA string (e.g. "rgba(255, 255, 0, 0)")
     */
    @SuppressLint("DefaultLocale")
    public static String colorToRGBA(int color) {
        return String.format("rgba(%d, %d, %d, %d)", Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color));
    }

    /*
     * Converts an ARGB color to RGB hexadecimal string
     *  Solution from https://stackoverflow.com/questions/6539879/how-to-convert-a-color-integer-to-a-hex-string-in-android
     *  allows for me to not use a substring (%06X formats a hexadecimal integer)
     * @param Color integer, in Android's preferred ARGB
     * @return A hexadecimal string ("#RRGGBB")
     */
    public static String colorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /*
     * Returns the alpha value of an integer color.
     * @param color The integer color value, ARGB
     * @return The alpha of the color, float, 0-1
     */
    public static float getColorAlpha(int color) {
        return (float) Color.alpha(color) / 255f;
    }

    /*
     * Sets DrawAppearance properties from application settings
     * @param context You need to pass a context e.g. MainActivity instance
     */
    public void loadFromSettings(Context context) {
        this.stroke = PreferenceManager.getDefaultSharedPreferences(context).getInt("strokeColor", -1);
        this.fill = PreferenceManager.getDefaultSharedPreferences(context).getInt("fillColor", -1);
        this.strokeSize = (int) Float.parseFloat(PreferenceManager.getDefaultSharedPreferences(context).getString("strokeSize", "5"));
    }

    /*
     * Initialises a <code>Paint</code> with a default configuration.
     * @param paint The <code>Paint</code> to initialise.
     */
    public void initialisePaint(Paint paint, float dpCorrection) {
        paint.setAntiAlias(true);
        paint.setStrokeWidth(!useDP ? strokeSize : strokeSize * dpCorrection);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (Objects.requireNonNull(effect) == EFFECTS.dashed) {
            paint.setPathEffect(new DashPathEffect(new float[]{
                    !useDP ? 5 : 5 * dpCorrection,
                    !useDP ? 15 : 15 * dpCorrection
            }, 0));
        }
    }

    /*
     * Creates a new DrawAppearance instance with the same values as the current one.
     * @return The copied DrawAppearance.
     */
    @NonNull
    @Override
    public DrawAppearance clone() {
        return new DrawAppearance(stroke, fill, strokeSize);
    }

    // Basic implementation of special FX
    public enum EFFECTS {none, dashed}
}
