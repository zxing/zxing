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
   * Determines the camera id to open with {@link Camera#open(int)}, if one exists.
   *
   * @param cameraId camera ID of the camera to use. A negative value
   *  or {@link #NO_REQUESTED_CAMERA} means "no preference"
   * @return the camera ID to use in {@link Camera#open(int)}
   * @throws IllegalStateException if a preferrred camera id does not exist
   */
  public static int getCameraId(int requestedCameraId) {

    int numCameras = Camera.getNumberOfCameras();
    if (numCameras == 0) {
      Log.w(TAG, "No cameras!");
      return NO_REQUESTED_CAMERA;
    }

    boolean explicitRequest = requestedCameraId >= 0;

    if (!explicitRequest) {
      // Select a camera if no explicit camera requested
      int index = 0;
      while (index < numCameras) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(index, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
          break;
        }
        index++;
      }

      requestedCameraId = index;
    }

    if (requestedCameraId < numCameras) {
      Log.i(TAG, "Using camera #" + requestedCameraId);
      return requestedCameraId;
    } else {
      if (explicitRequest) {
        IllegalStateException ex = new IllegalStateException(
            "Requested camera does not exist: " + requestedCameraId);
        Log.w(TAG, "Requested camera does not exist: " + requestedCameraId, ex);
        throw ex;
      } else {
        Log.i(TAG, "No camera facing back; returning camera #0");
        return 0;
      }
    }
  }

}
