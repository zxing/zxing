/*
 * Copyright (C) 2018 ZXing authors
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

package com.google.zxing.client.android.camera;

/**
 * Stores camera configuration, i.e. fields that might alter camera's behavior.
 */
public class CameraConfiguration {
  public boolean autoFocus = true;
  public boolean continuousFocus = false;
  public boolean invertScan = false;
  public boolean barcodeSceneMode = false;
  public boolean metering = false;
  public boolean exposure = false;
  public FrontLightMode frontLightMode = FrontLightMode.OFF;

  public CameraConfiguration copy() {
    final CameraConfiguration copy = new CameraConfiguration();
    copy.autoFocus = autoFocus;
    copy.continuousFocus = continuousFocus;
    copy.invertScan = invertScan;
    copy.barcodeSceneMode = barcodeSceneMode;
    copy.metering = metering;
    copy.exposure = exposure;
    copy.frontLightMode = frontLightMode;
    return copy;
  }
}
