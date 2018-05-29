/*
 * Copyright (C) 2012 ZXing authors
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

package com.google.zxing.client.android.camera.open;

import android.hardware.Camera;
import android.util.Log;

/**
 * Abstraction over the {@link Camera} API that helps open them and return their metadata.
 */
@SuppressWarnings("deprecation") // camera APIs
public final class OpenCameraInterface {

  private static final String TAG = OpenCameraInterface.class.getName();

  /** For {@link #open(int)}, means no preference for which camera to open. */
  public static final int NO_REQUESTED_CAMERA = -1;

  private OpenCameraInterface() {
  }

  /**
   * Opens the requested camera with {@link Camera#open(int)}, if one exists.
   *
   * @param cameraId camera ID of the camera to use. A negative value
   *  or {@link #NO_REQUESTED_CAMERA} means "no preference", in which case a rear-facing
   *  camera is returned if possible or else any camera
   * @return handle to {@link OpenCamera} that was opened
   */
  public static OpenCamera open(int cameraId) {

    int numCameras = Camera.getNumberOfCameras();
    if (numCameras == 0) {
      Log.w(TAG, "No cameras!");
      return null;
    }
    if (cameraId >= numCameras) {
      Log.w(TAG, "Requested camera does not exist: " + cameraId);
      return null;
    }

    if (cameraId <= NO_REQUESTED_CAMERA) {
      cameraId = 0;
      while (cameraId < numCameras) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        if (CameraFacing.values()[cameraInfo.facing] == CameraFacing.BACK) {
          break;
        }
        cameraId++;
      }
      if (cameraId == numCameras) {
        Log.i(TAG, "No camera facing " + CameraFacing.BACK + "; returning camera #0");
        cameraId = 0;
      }
    }

    Log.i(TAG, "Opening camera #" + cameraId);
    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);
    Camera camera = Camera.open(cameraId);
    if (camera == null) {
      return null;
    }
    return new OpenCamera(cameraId,
                          camera,
                          CameraFacing.values()[cameraInfo.facing],
                          cameraInfo.orientation);
  }

}
