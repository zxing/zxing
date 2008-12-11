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

package com.google.zxing.client.androidtest;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public final class ViewfinderView extends View {

  private final Paint mPaint;
  private final Rect mBox;
  private final int mMaskColor;
  private final int mFrameColor;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    mPaint = new Paint();
    mBox = new Rect();
    Resources resources = getResources();
    mMaskColor = resources.getColor(R.color.viewfinder_mask);
    mFrameColor = resources.getColor(R.color.viewfinder_frame);
  }

  @Override
  public void onDraw(Canvas canvas) {
    Rect frame = CameraManager.get().getFramingRect();
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened, in red to distinguish it from
    // the regular barcodes app
    mPaint.setColor(mMaskColor);
    mBox.set(0, 0, width, frame.top);
    canvas.drawRect(mBox, mPaint);
    mBox.set(0, frame.top, frame.left, frame.bottom + 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(frame.right + 1, frame.top, width, frame.bottom + 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(0, frame.bottom + 1, width, height);
    canvas.drawRect(mBox, mPaint);

    // Draw a two pixel solid white border inside the framing rect
    mPaint.setColor(mFrameColor);
    mBox.set(frame.left, frame.top, frame.right + 1, frame.top + 2);
    canvas.drawRect(mBox, mPaint);
    mBox.set(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1);
    canvas.drawRect(mBox, mPaint);
    mBox.set(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1);
    canvas.drawRect(mBox, mPaint);
  }

}
