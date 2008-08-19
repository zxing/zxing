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

package com.android.barcodes;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import com.google.zxing.ResultPoint;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 */
public class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final int ANIMATION_DELAY = 100;

    private Paint mPaint;
    private Rect mBox;
    private Point[] mResultPoints;
    private int mMaskColor;
    private int mFrameColor;
    private int mPointsColor;
    private int mLaserColor;
    private int mScannerAlpha;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        mPaint = new Paint();
        mBox = new Rect();
        Resources resources = getResources();
        mMaskColor = resources.getColor(R.color.viewfinder_mask);
        mFrameColor = resources.getColor(R.color.viewfinder_frame);
        mPointsColor = resources.getColor(R.color.result_points);
        mLaserColor = resources.getColor(R.color.viewfinder_laser);
        mScannerAlpha = 0;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        mPaint.setColor(mMaskColor);
        mBox.set(0, 0, width, frame.top);
        canvas.drawRect(mBox, mPaint);
        mBox.set(0, frame.top, frame.left, frame.bottom + 1);
        canvas.drawRect(mBox, mPaint);
        mBox.set(frame.right + 1, frame.top, width, frame.bottom + 1);
        canvas.drawRect(mBox, mPaint);
        mBox.set(0, frame.bottom + 1, width, height);
        canvas.drawRect(mBox, mPaint);

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

        if (mResultPoints != null) {
            // Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode
            mPaint.setColor(mPointsColor);
            if (mResultPoints.length == 2) {
                mPaint.setStrokeWidth(4);
                canvas.drawLine(mResultPoints[0].x, mResultPoints[0].y, mResultPoints[1].x,
                        mResultPoints[1].y, mPaint);
            } else {
                mPaint.setStrokeWidth(10);
                for (int x = 0; x < mResultPoints.length; x++) {
                    canvas.drawPoint(mResultPoints[x].x, mResultPoints[x].y, mPaint);
                }
            }
        } else {
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
        mResultPoints = null;
        invalidate();
    }

    /**
     * Draw a line for 1D barcodes (which return two points) or otherwise a set of points returned
     * from the decoder to indicate what we found. For efficiency, convert these to drawable
     * coordinates once here.
     *
     * @param resultPoints An array of points from the decoder, whose coordinates are expressed
     *                     relative to the still image from the camera.
     */
    public void drawResultPoints(ResultPoint[] resultPoints) {
        mResultPoints = CameraManager.get().convertResultPoints(resultPoints);
        invalidate();
    }

}
