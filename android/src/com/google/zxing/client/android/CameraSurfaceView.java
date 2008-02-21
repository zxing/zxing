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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.zxing.ResultPoint;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};

  private CameraManager cameraManager;
  private SurfaceHolder surfaceHolder;
  private boolean hasSurface;
  private int scannerAlpha;

  CameraSurfaceView(Context context, CameraManager cameraManager) {
    super(context);
    this.cameraManager = cameraManager;

    // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created
    // and destroyed.
    surfaceHolder = getHolder();
    surfaceHolder.setCallback(this);
    hasSurface = false;
    scannerAlpha = 0;
    surfaceHolder.setSizeFromLayout();
  }

  public boolean surfaceCreated(SurfaceHolder holder) {
    hasSurface = true;

    // Tell the system that we filled the surface in this call. This is a lie to prevent the system
    // from filling the surface for us automatically. THIS IS REQUIRED because otherwise we'll
    // access the Surface object from 2 different threads which is not allowed.
    return true;
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    // FIXME(dswitkin): The docs say this surface will be destroyed when this method returns. In
    // practice this has not been a problem so far. I need to investigate.
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // Surface size or format has changed. This won't happen because of the  setFixedSize() call.
  }

  /**
   * This method is only called from the WorkerThread. It's job is to grab the next preview frame
   * from the camera, draw the framing rectangle, and blit everything to the screen.
   */
  public void capturePreviewAndDraw() {
    if (hasSurface) {
      Canvas canvas = surfaceHolder.lockCanvas();
      cameraManager.capturePreview(canvas);
      Rect frame = cameraManager.getFramingRect();
      int width = canvas.getBitmapWidth();
      int height = canvas.getBitmapHeight();

      // Draw the exterior (i.e. outside the framing rect) as half darkened
      Paint paint = new Paint();
      paint.setColor(Color.BLACK);
      paint.setAlpha(96);
      Rect box = new Rect(0, 0, width, frame.top);
      canvas.drawRect(box, paint);
      box.set(0, frame.top, frame.left, frame.bottom + 1);
      canvas.drawRect(box, paint);
      box.set(frame.right + 1, frame.top, width, frame.bottom + 1);
      canvas.drawRect(box, paint);
      box.set(0, frame.bottom + 1, width, height);
      canvas.drawRect(box, paint);

      // Draw a two pixel solid black border inside the framing rect
      paint.setAlpha(255);
      box.set(frame.left, frame.top, frame.right + 1, frame.top + 2);
      canvas.drawRect(box, paint);
      box.set(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1);
      canvas.drawRect(box, paint);
      box.set(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1);
      canvas.drawRect(box, paint);
      box.set(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1);
      canvas.drawRect(box, paint);

      // Draw a red "laser scanner" line through the middle
      paint.setColor(Color.RED);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      int middle = frame.height() / 2 + frame.top;
      box.set(frame.left + 2, middle - 1, frame.right - 1, middle + 2);
      canvas.drawRect(box, paint);

      surfaceHolder.unlockCanvasAndPost(canvas);

      // This cheap animation is tied to the rate at which we pull previews from the camera.
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
    }
  }

  /**
   * Draw a line for 1D barcodes (which return two points) or otherwise a set of points returned
   * from the decoder to indicate what we found.
   * TODO(dswitkin): It might be nice to clear the framing rect and zoom in on the actual still that
   * was captured, then paint the green points on it. This would also clear the red scanner line
   * which doesn't make sense after the capture.
   *
   * @param resultPoints An array of points from the decoder, whose coordinates are expressed
   * relative to the still image from the camera.
   */
  public void drawResultPoints(ResultPoint[] resultPoints) {
    if (hasSurface) {
      Canvas canvas = surfaceHolder.lockCanvas();
      Paint paint = new Paint();
      paint.setColor(Color.GREEN);
      paint.setAlpha(128);

      Point[] points = cameraManager.convertResultPoints(resultPoints);
      if (points.length == 2) {
        paint.setStrokeWidth(4);
        canvas.drawLine(points[0].x, points[0].y, points[1].x, points[1].y, paint);
      } else {
        paint.setStrokeWidth(10);
        for (int x = 0; x < points.length; x++) {
          canvas.drawPoint(points[x].x, points[x].y, paint);
        }
      }

      surfaceHolder.unlockCanvasAndPost(canvas);
    }
	}
	
}
