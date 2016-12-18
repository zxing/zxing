/*
 * Copyright (C) 2015 ZXing authors
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

/**
 * Represents an open {@link Camera} and its metadata, like facing direction and orientation.
 */
public final class OpenCamera {
  
  private final int index;
  private final Camera camera;
  private final CameraFacing facing;
  private final int orientation;
  
  public OpenCamera(int index, Camera camera, CameraFacing facing, int orientation) {
    this.index = index;
    this.camera = camera;
    this.facing = facing;
    this.orientation = orientation;
  }

  public Camera getCamera() {
    return camera;
  }

  public CameraFacing getFacing() {
    return facing;
  }

  public int getOrientation() {
    return orientation;
  }

  @Override
  public String toString() {
    return "Camera #" + index + " : " + facing + ',' + orientation;
  }

}
