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

package com.google.zxing.client.android.camera.metering;

import java.util.Collections;
import java.util.List;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

public final class MeteringInterface {

  private static final String TAG = MeteringInterface.class.getSimpleName();
  private static final int AREA_PER_1000 = 400;

  private MeteringInterface() {
  }

  public static void setFocusArea(Camera.Parameters parameters) {
    if (parameters.getMaxNumFocusAreas() > 0) {
      Log.i(TAG, "Old focus areas: " + toString(parameters.getFocusAreas()));
      List<Camera.Area> middleArea = buildMiddleArea();
      Log.i(TAG, "Setting focus area to : " + toString(middleArea));
      parameters.setFocusAreas(middleArea);
    } else {
      Log.i(TAG, "Device does not support focus areas");
    }
  }

  public static void setMetering(Camera.Parameters parameters) {
    if (parameters.getMaxNumMeteringAreas() > 0) {
      Log.i(TAG, "Old metering areas: " + parameters.getMeteringAreas());
      List<Camera.Area> middleArea = buildMiddleArea();
      Log.i(TAG, "Setting metering area to : " + toString(middleArea));
      parameters.setMeteringAreas(middleArea);
    } else {
      Log.i(TAG, "Device does not support metering areas");
    }
  }

  private static List<Camera.Area> buildMiddleArea() {
    return Collections.singletonList(
        new Camera.Area(new Rect(-AREA_PER_1000, -AREA_PER_1000, AREA_PER_1000, AREA_PER_1000), 1));
  }

  private static String toString(Iterable<Camera.Area> areas) {
    if (areas == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    for (Camera.Area area : areas) {
      result.append(area.rect).append(':').append(area.weight).append(' ');
    }
    return result.toString();
  }

}
