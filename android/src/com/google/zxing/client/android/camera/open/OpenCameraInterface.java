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

public final class OpenCameraInterface {

  private static final String TAG = OpenCameraInterface.class.getName();

  private OpenCameraInterface() {
  }

  /** For {@link #open(int)}, means no preference for which camera to open. */
  public static final int NO_REQUESTED_CAMERA = -1;

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

    boolean explicitRequest = cameraId >= 0;

    Camera.CameraInfo selectedCameraInfo = null;
    int index;
    if (explicitRequest) {
      index = cameraId;
      selectedCameraInfo = new Camera.CameraInfo();
      Camera.getCameraInfo(index, selectedCameraInfo);
    } else {
      index = 0;
      while (index < numCameras) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(index, cameraInfo);
        CameraFacing reportedFacing = CameraFacing.values()[cameraInfo.facing];
        if (reportedFacing == CameraFacing.BACK) {
          selectedCameraInfo = cameraInfo;
          break;
        }
        index++;
      }
    }

    Camera camera;
    if (index < numCameras) {
      Log.i(TAG, "Opening camera #" + index);
      camera = Camera.open(index);
    } else {
      if (explicitRequest) {
        Log.w(TAG, "Requested camera does not exist: " + cameraId);
        camera = null;
      } else {
        Log.i(TAG, "No camera facing " + CameraFacing.BACK + "; returning camera #0");
        camera = Camera.open(0);
        selectedCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, selectedCameraInfo);
      }
    }

    if (camera == null) {
      return null;
    }
    return new OpenCamera(index,
                          camera,
                          CameraFacing.values()[selectedCameraInfo.facing],
                          selectedCameraInfo.orientation);
  }

}
