package com.google.zxing.integration.android;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Encapsulates custom configuration used in methods of {@link MatrixToBitmapWriter}.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public class MatrixToImageConfig {

    private final int on_color;
    private final int off_color;

    /**
     * Creates a new default-configuration, using {@link Color#BLACK} for on-bits
     *  and {@link Color#WHITE} for off-bits, generating normal black-on-white codes.
     */
    public MatrixToImageConfig(){
        this(Color.BLACK, Color.WHITE);
    }

    /**
     * <p>Creates a new config with the specified colors to use.</p>
     * <p>Colors used here should be created using {@link Color}'s static helper-
     *  methods or the {@link android.content.res.Resources#getColor(int)}-
     *  method, to get them from XML.</p>
     * @param on_color the color for an on-pixel.
     * @param off_color the color for an off-pixel.
     */
    public MatrixToImageConfig(int on_color, int off_color) {
        this.on_color = on_color;
        this.off_color = off_color;
    }

    public int getPixelOnColor() {
        return on_color;
    }

    public int getPixelOffColor() {
        return off_color;
    }

    Bitmap.Config getBitmapColorModel() {
        // Use smaller RGB_565 if colors match default
        return (this.on_color == Color.BLACK && this.off_color == Color.WHITE)
                ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
    }

}
