package com.google.zxing.integration.android;

import android.graphics.Bitmap;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes a {@link BitMatrix} to {@link Bitmap}, {@link File} or {@link OutputStream}.
 * Provided here instead of core, since it depends Android-classes.
 *
 * @author Lukas Knuth
 * @version 1.0
 */
public final class MatrixToBitmapWriter {

    private static final MatrixToImageConfig DEFAULT_CONFIG = new MatrixToImageConfig();

    /**
     * This is a static utility-class!
     * @see #toBitmap(com.google.zxing.common.BitMatrix, MatrixToImageConfig)
     */
    private MatrixToBitmapWriter(){} // TODO Test...

    /**
     * {@link #writeToFile(com.google.zxing.common.BitMatrix, android.graphics.Bitmap.CompressFormat, int, java.io.File, MatrixToImageConfig)} with a default {@code MatrixToImageConfig},
     * rendering on to black and off to white.
     */
    public static void writeToFile(BitMatrix matrix, Bitmap.CompressFormat format, int quality, File file) throws IOException{
        MatrixToBitmapWriter.writeToFile(matrix, format, quality, file, DEFAULT_CONFIG);
    }

    /**
     * Convenience-method to first create and then directly write a {@code BitMatrix}
     *  to {@code File}.
     * @param matrix the matrix to write.
     * @param format the format to store the picture in.
     * @param quality the quality. See {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)} for details.
     * @param file the file to write to.
     * @param config the config to use.
     * @throws IOException if the given matrix could not be written to the given stream.
     */
    public static void writeToFile(BitMatrix matrix, Bitmap.CompressFormat format, int quality, File file, MatrixToImageConfig config) throws IOException{
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            MatrixToBitmapWriter.writeToStream(matrix, format, quality, out, config);
        } catch (IOException e){
            throw new IOException("Could not write an image of format "+format+" to "+file, e);
        } finally {
            if (out != null) out.close();
        }
    }

    /**
     * {@link #writeToStream(com.google.zxing.common.BitMatrix, android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream, MatrixToImageConfig)} with a default {@code MatrixToImageConfig},
     * rendering on to black and off to white.
     */
    public static void writeToStream(BitMatrix matrix, Bitmap.CompressFormat format, int quality, OutputStream out) throws IOException {
        writeToStream(matrix, format, quality, out, DEFAULT_CONFIG);
    }

    /**
     * Convenience-method to first create and then directly write a {@code BitMatrix}
     *  to {@code OutputStream}.
     * @param matrix the matrix to write.
     * @param format the format to store the picture in.
     * @param quality the quality. See {@link Bitmap#compress(android.graphics.Bitmap.CompressFormat, int, java.io.OutputStream)} for details.
     * @param out the stream to write to.
     * @param config the config to use.
     * @throws IOException if the given matrix could not be written to the given stream.
     */
    public static void writeToStream(BitMatrix matrix, Bitmap.CompressFormat format, int quality, OutputStream out, MatrixToImageConfig config) throws IOException {
        Bitmap bmp = MatrixToBitmapWriter.toBitmap(matrix, config);
        try {
            if (! bmp.compress(format, quality, out) ){
                throw new IOException("Could not write an image of format "+format);
            }
        } finally {
            out.flush();
        }
    }

    /**
     * {@link #toBitmap(com.google.zxing.common.BitMatrix, MatrixToImageConfig)} with
     *  a default {@code MatrixToImageConfig}, rendering on to black and off to white.
     * @param matrix
     * @return
     */
    public static Bitmap toBitmap(BitMatrix matrix){
        return MatrixToBitmapWriter.toBitmap(matrix, DEFAULT_CONFIG);
    }

    /**
     * Renders the given {@code BitMatrix} on a new {@link Bitmap}-object.
     * @param matrix the matrix to write.
     * @param config the configuration to use for creating the new Bitmap.
     * @return the new {@link Bitmap}-object.
     */
    public static Bitmap toBitmap(BitMatrix matrix, MatrixToImageConfig config){
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, config.getBitmapColorModel());
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                bmp.setPixel(x, y, matrix.get(x,y) ? config.getPixelOnColor() : config.getPixelOffColor());
            }
        }
        return bmp;
    }
}

