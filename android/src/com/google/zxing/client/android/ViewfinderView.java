/*
 * Copyright (C) 2008 ZXing authors
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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 100L;

  private final Paint mPaint;
  private final Rect mBox;
  private Bitmap mResultBitmap;
  private final int mMaskColor;
  private final int mResultColor;
  private final int mFrameColor;
  private final int mLaserColor;
  private int mScannerAlpha;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    mPaint = new Paint();
    mBox = new Rect();
    Resources resources = getResources();
    mMaskColor = resources.getColor(R.color.viewfinder_mask);
    mResultColor = resources.getColor(R.color.result_view);
    mFrameColor = resources.getColor(R.color.viewfinder_frame);
    mLaserColor = resources.getColor(R.color.viewfinder_laser);
    mScannerAlpha = 0;
  }

  @Override
  public void onDraw(Canvas canvas) {
    Rect frame = CameraManager.get().getFramingRect();
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    mPaint.setColor(mResultBitmap != null ? mResultColor : mMaskColor);
    mBox.set(0, 0, width, frame.top);
    canvas.drawRect(mBox, mPaint);
    mBox.set(0, frame.top, frame.left, frame.bottom + 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(frame.right + 1, frame.top, width, frame.bottom + 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(0, frame.bottom + 1, width, height);
    canvas.drawRect(mBox, mPaint);

    if (mResultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      mPaint.setAlpha(255);
      canvas.drawBitmap(mResultBitmap, frame.left, frame.top, mPaint);
    } else {
      // Draw a two pixel solid black border inside the framing rect
      mPaint.setColor(mFrameColor);
      mBox.set(frame.left, frame.top, frame.right + 1, frame.top + 2);
      canvas.drawRect(mBox, mPaint);
      mBox.set(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1);
      canvas.drawRect(mBox, mPaint);
      mBox.set(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1);
      canvas.drawRect(mBox, mPaint);
      mBox.set(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1);
      canvas.drawRect(mBox, mPaint);

      // Draw a red "laser scanner" line through the middle to show decoding is active
      mPaint.setColor(mLaserColor);
      mPaint.setAlpha(SCANNER_ALPHA[mScannerAlpha]);
      mScannerAlpha = (mScannerAlpha + 1) % SCANNER_ALPHA.length;
      int middle = frame.height() / 2 + frame.top;
      mBox.set(frame.left + 2, middle - 1, frame.right - 1, middle + 2);
      canvas.drawRect(mBox, mPaint);

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY, mBox.left, mBox.top, mBox.right, mBox.bottom);
    }
  }

  public void drawViewfinder() {
    mResultBitmap = null;
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    mResultBitmap = barcode;
    invalidate();
  }

}
