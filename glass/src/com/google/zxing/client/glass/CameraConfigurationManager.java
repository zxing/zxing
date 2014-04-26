/*
 * Copyright (C) 2014 ZXing authors
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

package com.google.zxing.client.glass;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Sean Owen
 */
final class CameraConfigurationManager {

  private static final String TAG = "CameraConfiguration";
  private static final int AREA_PER_1000 = 400;
  private static final int MIN_FPS = 10;

  private CameraConfigurationManager() {
  }

  static void configure(Camera camera) {
    Camera.Parameters parameters = camera.getParameters();
    //parameters.setPreviewSize(1024, 768);
    parameters.setPreviewSize(512, 288);
    //configureAdvanced(parameters);
    camera.setParameters(parameters);
  }

  private static void configureAdvanced(Camera.Parameters parameters) {

    setBestPreviewFPS(parameters);

    String sceneMode = findSettableValue(parameters.getSupportedSceneModes(),
                                         Camera.Parameters.SCENE_MODE_BARCODE);
    if (sceneMode != null) {
      parameters.setSceneMode(sceneMode);
    } else {
      Log.i(TAG, "Scene mode is not supported");
    }

    if (parameters.isVideoStabilizationSupported()) {
      Log.i(TAG, "Enabling video stabilization...");
      parameters.setVideoStabilization(true);
    } else {
      Log.i(TAG, "This device does not support video stabilization");
    }

    if (parameters.getMaxNumMeteringAreas() > 0) {
      Log.i(TAG, "Old metering areas: " + parameters.getMeteringAreas());
      List<Camera.Area> middleArea = Collections.singletonList(
          new Camera.Area(new Rect(-AREA_PER_1000, -AREA_PER_1000, AREA_PER_1000, AREA_PER_1000), 1));
      parameters.setMeteringAreas(middleArea);
    } else {
      Log.i(TAG, "Device does not support metering areas");
    }

    if (parameters.isZoomSupported()) {
      Log.i(TAG, "Setting to max zoom");
      parameters.setZoom(parameters.getMaxZoom());
    } else {
      Log.i(TAG, "Zoom is not supported");
    }

  }

  private static String findSettableValue(Collection<String> supportedValues,
                                          String... desiredValues) {
    Log.i(TAG, "Supported values: " + supportedValues);
    String result = null;
    if (supportedValues != null) {
      for (String desiredValue : desiredValues) {
        if (supportedValues.contains(desiredValue)) {
          result = desiredValue;
          break;
        }
      }
    }
    Log.i(TAG, "Settable value: " + result);
    return result;
  }

  private static void setBestPreviewFPS(Camera.Parameters parameters) {
    // Required for Glass compatibility; also improves battery/CPU performance a tad
    List<int[]> supportedPreviewFpsRanges = parameters.getSupportedPreviewFpsRange();
    if (supportedPreviewFpsRanges != null && !supportedPreviewFpsRanges.isEmpty()) {
      int[] minimumSuitableFpsRange = null;
      for (int[] fpsRange : supportedPreviewFpsRanges) {
        int fpsMax = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
        if (fpsMax >= MIN_FPS * 1000 &&
            (minimumSuitableFpsRange == null ||
                fpsMax > minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])) {
          minimumSuitableFpsRange = fpsRange;
        }
      }
      if (minimumSuitableFpsRange == null) {
        Log.i(TAG, "No suitable FPS range?");
      } else {
        int[] currentFpsRange = new int[2];
        parameters.getPreviewFpsRange(currentFpsRange);
        if (!Arrays.equals(currentFpsRange, minimumSuitableFpsRange)) {
          Log.i(TAG, "Setting FPS range to " + Arrays.toString(minimumSuitableFpsRange));
          parameters.setPreviewFpsRange(minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
              minimumSuitableFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
        }
      }
    }
  }

}
