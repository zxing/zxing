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
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.SurfaceHolder;
import com.google.zxing.ResultPoint;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 */
final class CameraManager {

    private static final String TAG = "CameraManager";

    private static CameraManager mCameraManager;
    private Camera mCamera;
    private final Context mContext;
    private Point mScreenResolution;
    private Rect mFramingRect;
    private Handler mPreviewHandler;
    private int mPreviewMessage;
    private Handler mAutoFocusHandler;
    private int mAutoFocusMessage;
    private boolean mInitialized;
    private boolean mPreviewing;

    public static void init(Context context) {
        if (mCameraManager == null) {
            mCameraManager = new CameraManager(context);
        }
    }

    public static CameraManager get() {
        return mCameraManager;
    }

    private CameraManager(Context context) {
        mContext = context;
        mCamera = null;
        mInitialized = false;
        mPreviewing = false;
    }

    public void openDriver(SurfaceHolder holder) {
        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(holder);

            if (!mInitialized) {
                mInitialized = true;
                getScreenResolution();
            }

            setCameraParameters();
        }
    }

    public void closeDriver() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void startPreview() {
        if (mCamera != null && !mPreviewing) {
            mCamera.startPreview();
            mPreviewing = true;
        }
    }

    public void stopPreview() {
        if (mCamera != null && mPreviewing) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mPreviewHandler = null;
            mAutoFocusHandler = null;
            mPreviewing = false;
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public void requestPreviewFrame(Handler handler, int message) {
        if (mCamera != null && mPreviewing) {
            mPreviewHandler = handler;
            mPreviewMessage = message;
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    public void requestAutoFocus(Handler handler, int message) {
        if (mCamera != null && mPreviewing) {
            mAutoFocusHandler = handler;
            mAutoFocusMessage = message;
            mCamera.autoFocus(autoFocusCallback);
        }
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
        if (mFramingRect == null) {
            int size = ((mScreenResolution.x < mScreenResolution.y) ? mScreenResolution.x :
                    mScreenResolution.y) * 3 / 4;
            int leftOffset = (mScreenResolution.x - size) / 2;
            int topOffset = (mScreenResolution.y - size) / 2;
            mFramingRect = new Rect(leftOffset, topOffset, leftOffset + size, topOffset + size);
            Log.v(TAG, "Calculated framing rect: " + mFramingRect);
        }
        return mFramingRect;
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
        int count = points.length;
        Point[] output = new Point[count];
        for (int x = 0; x < count; x++) {
            output[x] = new Point();
            output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
            output[x].y = frame.top + (int) (points[x].getY() + 0.5f);
        }
        return output;
    }

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            camera.setPreviewCallback(null);
            if (mPreviewHandler != null) {
                Message message = mPreviewHandler.obtainMessage(mPreviewMessage, mScreenResolution.x,
                        mScreenResolution.y, data);
                message.sendToTarget();
                mPreviewHandler = null;
            }
        }
    };

    private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            if (mAutoFocusHandler != null) {
                Message message = mAutoFocusHandler.obtainMessage(mAutoFocusMessage, success);
                // Simulate continuous autofocus by sending a focus request every second.
                mAutoFocusHandler.sendMessageDelayed(message, 1000);
                mAutoFocusHandler = null;
            }
        }
    };

    /**
     * Sets the camera up to take preview images which are used for both preview and decoding. We're
     * counting on the default YUV420 semi-planar data. If that changes in the future, we'll need to
     * specify it explicitly with setPreviewFormat().
     */
    private void setCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mScreenResolution.x, mScreenResolution.y);
        mCamera.setParameters(parameters);
        Log.v(TAG, "Setting params for preview: width " + mScreenResolution.x + " height " +
                mScreenResolution.y);
    }

    private Point getScreenResolution() {
        if (mScreenResolution == null) {
            WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            mScreenResolution = new Point(display.getWidth(), display.getHeight());
        }
        return mScreenResolution;
    }

}
