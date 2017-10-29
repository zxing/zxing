/*
 * Copyright (C) 2011 ZXing authors
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

package com.google.zxing.client.android.wifi;

enum NetworkType {

  WEP,
  WPA,
  NO_PASSWORD,
  WPA2_EAP;

  static NetworkType forIntentValue(String networkTypeString) {
    if (networkTypeString == null) {
      return NO_PASSWORD;
    }
    switch (networkTypeString) {
      case "WPA":
      case "WPA2":
        return WPA;
      case "WPA2-EAP":
        return WPA2_EAP;
      case "WEP":
        return WEP;
      case "nopass":
        return NO_PASSWORD;
      default:
        throw new IllegalArgumentException(networkTypeString);
    }
  }

}
