/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.client.result;

import com.google.zxing.Result;

/**
 * Parses a WIFI configuration string.  Strings will be of the form:
 * WIFI:T:WPA;S:mynetwork;P:mypass;;
 *
 * The fields can come in any order, and there should be tests to see
 * if we can parse them all correctly.
 *
 * @author Vikram Aggarwal
 */
public final class WifiResultParser extends ResultParser {

  @Override
  public WifiParsedResult parse(Result result) {
    String rawText = result.getText();
    if (!rawText.startsWith("WIFI:")) {
      return null;
    }
    // Don't remove leading or trailing whitespace
    boolean trim = false;
    String ssid = matchSinglePrefixedField("S:", rawText, ';', trim);
    if (ssid == null || ssid.length() == 0) {
      return null;
    }
    String pass = matchSinglePrefixedField("P:", rawText, ';', trim);
    String type = matchSinglePrefixedField("T:", rawText, ';', trim);
    if (type == null) {
      type = "nopass";
    }

    return new WifiParsedResult(type, ssid, pass);
  }
}