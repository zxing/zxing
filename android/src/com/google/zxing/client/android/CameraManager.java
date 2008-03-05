/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.CameraDevice;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.google.zxing.ResultPoint;

/**
 * This object wraps the CameraDevice and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images and well as high
 * resolution stills.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CameraManager {

  private static final String TAG = "CameraManager";

  private final Context context;
  private Point cameraResolution;
  private Point stillResolution;
  private int stillMultiplier;
  private Point screenResolution;
  private Rect framingRect;
  private final Bitmap bitmap;
  private CameraDevice camera;
  private final CameraDevice.CaptureParams params;
  private boolean previewMode;

  CameraManager(Context context) {
    this.context = context;
    calculateStillResolution();
    getScreenResolution();
    bitmap = Bitmap.createBitmap(stillResolution.x, stillResolution.y, false);
    camera = CameraDevice.open();
    params = new CameraDevice.CaptureParams();
    previewMode = false;
    setPreviewMode(true);
  }

  public void openDriver() {
    if (camera == null) {
      camera = CameraDevice.open();
    }
  }

  public void closeDriver() {
    if (camera != null) {
      camera.close();
      camera = null;
    }
  }

  public void capturePreview(Canvas canvas) {
    setPreviewMode(true);
    camera.capture(canvas);
  }

  public Bitmap captureStill() {
    setPreviewMode(false);
    Canvas canvas = new Canvas(bitmap);
    camera.capture(canvas);
    return bitmap;
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. The actual captured image should be a bit larger than indicated because they might
   * frame the shot too tightly. This target helps with alignment as well as forces the user to hold
   * the device far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect getFramingRect() {
    if (framingRect == null) {
      int size = stillResolution.x * screenResolution.x / cameraResolution.x;
      int leftOffset = (screenResolution.x - size) / 2;
      int topOffset = (screenResolution.y - size) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + size, topOffset + size);
    }
    return framingRect;
  }

  /**
   * Converts the result points from still resolution coordinates to screen coordinates.
   *
   * @param points The points returned by the Reader subclass through Result.getResultPoints().
   * @return An array of Points scaled to the size of the framing rect and offset appropriately
   *         so they can be drawn in screen coordinates.
   */
  public Point[] convertResultPoints(ResultPoint[] points) {
    Rect frame = getFramingRect();
    int frameSize = frame.width();
    int count = points.length;
    Point[] output = new Point[count];
    for (int x = 0; x < count; x++) {
      output[x] = new Point();
      output[x].x = frame.left + (int) (points[x].getX() * frameSize / stillResolution.x + 0.5f);
      output[x].y = frame.top + (int) (points[x].getY() * frameSize / stillResolution.y + 0.5f);
    }
    return output;
  }

  /**
   * Images for the live preview are taken at low resolution in RGB. The final stills for the
   * decoding step are taken in YUV, since we only need the luminance channel. Other code depends
   * on the ability to call this method for free if the correct mode is already set.
   *
   * @param on Setting on true will engage preview mode, setting it false will request still mode.
   */
  private void setPreviewMode(boolean on) {
    if (on != previewMode) {
      if (on) {
        params.type = 1; // preview
        if (cameraResolution.x / (float) cameraResolution.y <
            screenResolution.x / (float) screenResolution.y) {
          params.srcWidth = cameraResolution.x;
          params.srcHeight = cameraResolution.x * screenResolution.y / screenResolution.x;
          params.leftPixel = 0;
          params.topPixel = (cameraResolution.y - params.srcHeight) / 2;
        } else {
          params.srcWidth = cameraResolution.y * screenResolution.x / screenResolution.y;
          params.srcHeight = cameraResolution.y;
          params.leftPixel = (cameraResolution.x - params.srcWidth) / 2;
          params.topPixel = 0;
        }
        params.outputWidth = screenResolution.x;
        params.outputHeight = screenResolution.y;
        params.dataFormat = 2; // RGB565
      } else {
        params.type = 0; // still
        params.srcWidth = stillResolution.x * stillMultiplier;
        params.srcHeight = stillResolution.y * stillMultiplier;
        params.leftPixel = (cameraResolution.x - params.srcWidth) / 2;
        params.topPixel = (cameraResolution.y - params.srcHeight) / 2;
        params.outputWidth = stillResolution.x;
        params.outputHeight = stillResolution.y;
        params.dataFormat = 0; // YUV packed (planar would be better, but it doesn't work right now)
      }
      String captureType = on ? "preview" : "still";
      Log.v(TAG, "Setting params for " + captureType + ": srcWidth " + params.srcWidth +
          " srcHeight " + params.srcHeight + " leftPixel " + params.leftPixel + " topPixel " +
          params.topPixel + " outputWidth " + params.outputWidth + " outputHeight " +
          params.outputHeight);
      camera.setCaptureParams(params);
      previewMode = on;
    }
  }

  /**
   * This method determines how to take the highest quality image (i.e. the one which has the best
   * chance of being decoded) given the capabilities of the camera. It is a balancing act between
   * having enough resolution to read UPCs and having few enough pixels to keep the QR Code
   * processing fast. The result is the dimensions of the rectangle to capture from the center of
   * the sensor, plus a stillMultiplier which indicates whether we'll ask the driver to downsample
   * for us. This has the added benefit of keeping the memory footprint of the bitmap as small as
   * possible.
   */
  private void calculateStillResolution() {
    cameraResolution = getMaximumCameraResolution();
    int minDimension = (cameraResolution.x < cameraResolution.y) ? cameraResolution.x :
        cameraResolution.y;
    int diagonalResolution = (int) Math.sqrt(cameraResolution.x * cameraResolution.x +
        cameraResolution.y * cameraResolution.y);
    float diagonalFov = getFieldOfView();

    // Determine the field of view in the smaller dimension, then calculate how large an object
    // would be at the minimum focus distance.
    float fov = diagonalFov * minDimension / diagonalResolution;
    double objectSize = Math.tan(Math.toRadians(fov / 2.0)) * getMinimumFocusDistance() * 2;

    // Let's assume the largest barcode we might photograph at this distance is 3 inches across. By
    // cropping to this size, we can avoid processing surrounding pixels, which helps with speed and
    // accuracy. 
    // TODO(dswitkin): Handle a device with a great macro mode where objectSize < 4 inches.
    double crop = 3.0 / objectSize;
    int nativeResolution = (int) (minDimension * crop);

    // The camera driver can only capture images which are a multiple of eight, so it's necessary to
    // round up.
    nativeResolution = ((nativeResolution + 7) >> 3) << 3;
    if (nativeResolution > minDimension) {
      nativeResolution = minDimension;
    }

    // There's no point in capturing too much detail, so ask the driver to downsample. I haven't
    // tried a non-integer multiple, but it seems unlikely to work.
    double dpi = nativeResolution / objectSize;
    stillMultiplier = 1;
    if (dpi > 200) {
      stillMultiplier = (int) (dpi / 200 + 1);
    }
    stillResolution = new Point(nativeResolution, nativeResolution);
    Log.v(TAG, "FOV " + fov + " objectSize " + objectSize + " crop " + crop + " dpi " + dpi +
        " nativeResolution " + nativeResolution + " stillMultiplier " + stillMultiplier);
  }

  // FIXME(dswitkin): These three methods have temporary constants until the new Camera API can
  // provide the real values for the current device.
  // Temporary: the camera's maximum resolution in pixels.
  private static Point getMaximumCameraResolution() {
    return new Point(1280, 1024);
  }

  // Temporary: the diagonal field of view in degrees.
  private static float getFieldOfView() {
    return 60.0f;
  }

  // Temporary: the minimum focus distance in inches.
  private static float getMinimumFocusDistance() {
    return 12.0f;
  }

  private Point getScreenResolution() {
    if (screenResolution == null) {
      WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      Display display = manager.getDefaultDisplay();
      screenResolution = new Point(display.getWidth(), display.getHeight());
    }
    return screenResolution;
  }

}
