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
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CameraManager {
  private static final String TAG = "CameraManager";
  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 480;
  private static final int MAX_FRAME_HEIGHT = 360;

  private static CameraManager cameraManager;
  private Camera camera;
  private final Context context;
  private Point screenResolution;
  private Point cameraResolution;
  private Rect framingRect;
  private Handler previewHandler;
  private int previewMessage;
  private Handler autoFocusHandler;
  private int autoFocusMessage;
  private boolean initialized;
  private boolean previewing;
  private int previewFormat;
  private String previewFormatString;
  private boolean useOneShotPreviewCallback;

  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
    public void onPreviewFrame(byte[] data, Camera camera) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      if (previewHandler != null) {
        Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
            cameraResolution.y, data);
        message.sendToTarget();
        previewHandler = null;
      }
    }
  };

  /**
   * Autofocus callbacks arrive here, and are dispatched to the Handler which requested them.
   */
  private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
    public void onAutoFocus(boolean success, Camera camera) {
      if (autoFocusHandler != null) {
        Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
        // Barcode Scanner needs to insert a delay here because it does continuous focus,
        // but this test app does not, so send the message immediately.
        message.sendToTarget();
        autoFocusHandler = null;
      }
    }
  };

  /**
   * Initializes this static object with the Context of the calling Activity.
   *
   * @param context The Activity which wants to use the camera.
   */
  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  /**
   * Gets the CameraManager singleton instance.
   *
   * @return A reference to the CameraManager singleton.
   */
  public static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {
    this.context = context;
    camera = null;
    initialized = false;
    previewing = false;

    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
    // the more efficient one shot callback, as the older one can swamp the system and cause it
    // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
    //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
    useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake
  }

  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public String openDriver(SurfaceHolder holder, boolean getParameters) throws IOException {
    String result = null;
    if (camera == null) {
      camera = Camera.open();
      camera.setPreviewDisplay(holder);

      if (!initialized) {
        initialized = true;
        getScreenResolution();
      }

      if (getParameters) {
        result = collectCameraParameters();
      }
      setCameraParameters();
    }
    return result;
  }

  /**
   * Closes the camera driver if still in use.
   */
  public void closeDriver() {
    if (camera != null) {
      camera.release();
      camera = null;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewHandler = null;
      autoFocusHandler = null;
      previewing = false;
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
    if (camera != null && previewing) {
      previewHandler = handler;
      previewMessage = message;
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  /**
   * Asks the camera hardware to perform an autofocus.
   *
   * @param handler The Handler to notify when the autofocus completes.
   * @param message The message to deliver.
   */
  public void requestAutoFocus(Handler handler, int message) {
    if (camera != null && previewing) {
      autoFocusHandler = handler;
      autoFocusMessage = message;
      camera.autoFocus(autoFocusCallback);
    }
  }

  /**
   * Calculates the framing rect which the UI should draw to show the user where to place the
   * barcode. This target helps with alignment as well as forces the user to hold the device
   * far enough away to ensure the image will be in focus.
   *
   * @return The rectangle to draw on screen in window coordinates.
   */
  public Rect getFramingRect() {
    if (framingRect == null) {
      if (camera == null) {
        return null;
      }
      int width = cameraResolution.x * 3 / 4;
      if (width < MIN_FRAME_WIDTH) {
        width = MIN_FRAME_WIDTH;
      } else if (width > MAX_FRAME_WIDTH) {
        width = MAX_FRAME_WIDTH;
      }
      int height = cameraResolution.y * 3 / 4;
      if (height < MIN_FRAME_HEIGHT) {
        height = MIN_FRAME_HEIGHT;
      } else if (height > MAX_FRAME_HEIGHT) {
        height = MAX_FRAME_HEIGHT;
      }
      int leftOffset = (cameraResolution.x - width) / 2;
      int topOffset = (cameraResolution.y - height) / 2;
      framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
      Log.v(TAG, "Calculated framing rect: " + framingRect);
    }
    return framingRect;
  }

  /**
   * Sets the camera up to take preview images which are used for both preview and decoding.
   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
   * and the planar Y can be used for barcode scanning without a copy in some cases.
   */
  private void setCameraParameters() {
    Camera.Parameters parameters = camera.getParameters();
    Camera.Size size = parameters.getPreviewSize();
    Log.v(TAG, "Default preview size: " + size.width + ", " + size.height);
    previewFormat = parameters.getPreviewFormat();
    previewFormatString = parameters.get("preview-format");
    Log.v(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);

    // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
    // TODO: A better solution would be to request the supported preview resolutions
    // and pick the best match, but this parameter is not standardized in Cupcake.
    cameraResolution = new Point();
    cameraResolution.x = (screenResolution.x >> 3) << 3;
    cameraResolution.y = (screenResolution.y >> 3) << 3;
    Log.v(TAG, "Setting preview size: " + cameraResolution.x + ", " + cameraResolution.y);
    parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);

    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
    parameters.set("flash-value", 2);

    // This is the standard setting to turn the flash off that all devices should honor.
    parameters.set("flash-mode", "off");

    camera.setParameters(parameters);
  }

  private String collectCameraParameters() {
    Camera.Parameters parameters = camera.getParameters();
    String[] params = parameters.flatten().split(";");
    StringBuffer result = new StringBuffer();
    result.append("Default camera parameters:");
    for (String param : params) {
      result.append("\n  ");
      result.append(param);
    }
    result.append('\n');
    return result.toString();
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
