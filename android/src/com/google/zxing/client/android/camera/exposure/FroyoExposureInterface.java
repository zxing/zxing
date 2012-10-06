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

package com.google.zxing.client.android.camera.exposure;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.util.Log;

@TargetApi(8)
public final class FroyoExposureInterface implements ExposureInterface {

  private static final String TAG = FroyoExposureInterface.class.getSimpleName();

  private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
  private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;

  @Override
  public void setExposure(Camera.Parameters parameters, boolean lightOn) {
    int minExposure = parameters.getMinExposureCompensation();
    int maxExposure = parameters.getMaxExposureCompensation();
    if (minExposure != 0 || maxExposure != 0) {
      float step = parameters.getExposureCompensationStep();
      int desiredCompensation;
      if (lightOn) {
        // Light on; set low exposue compensation
        desiredCompensation = Math.max((int) (MIN_EXPOSURE_COMPENSATION / step), minExposure);
      } else {
        // Light off; set high compensation
        desiredCompensation = Math.min((int) (MAX_EXPOSURE_COMPENSATION / step), maxExposure);
      }
      Log.i(TAG, "Setting exposure compensation to " + desiredCompensation + " / " + (step * desiredCompensation));
      parameters.setExposureCompensation(desiredCompensation);
    } else {
      Log.i(TAG, "Camera does not support exposure compensation");
    }
  }

}
