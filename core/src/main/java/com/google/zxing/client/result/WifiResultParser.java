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
 * <p>Parses a WIFI configuration string. Strings will be of the form:</p>
 *
 * <p>{@code WIFI:T:[network type];S:[network SSID];P:[network password];H:[hidden?];;}</p>
 *
 * <p>For WPA2 enterprise (EAP), strings will be of the form:</p>
 *
 * <p>{@code WIFI:T:WPA2-EAP;S:[network SSID];H:[hidden?];E:[EAP method];H:[Phase 2 method];A:[anonymous identity];I:[username];P:[password];;}</p>
 *
 * <p>"EAP method" can e.g. be "TTLS" or "PWD" or one of the other fields in <a href="https://developer.android.com/reference/android/net/wifi/WifiEnterpriseConfig.Eap.html">WifiEnterpriseConfig.Eap</a> and "Phase 2 method" can e.g. be "MSCHAPV2" or any of the other fields in <a href="https://developer.android.com/reference/android/net/wifi/WifiEnterpriseConfig.Phase2.html">WifiEnterpriseConfig.Phase2</a></p>
 *
 * <p>The fields can appear in any order. Only "S:" is required.</p>
 *
 * @author Vikram Aggarwal
 * @author Sean Owen
 * @author Steffen Kieß
 */
public final class WifiResultParser extends ResultParser {

  @Override
  public WifiParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    if (!rawText.startsWith("WIFI:")) {
      return null;
    }
    rawText = rawText.substring("WIFI:".length());
    String ssid = matchSinglePrefixedField("S:", rawText, ';', false);
    if (ssid == null || ssid.isEmpty()) {
      return null;
    }
    String pass = matchSinglePrefixedField("P:", rawText, ';', false);
    String type = matchSinglePrefixedField("T:", rawText, ';', false);
    if (type == null) {
      type = "nopass";
    }
    boolean hidden = Boolean.parseBoolean(matchSinglePrefixedField("H:", rawText, ';', false));
    String identity = matchSinglePrefixedField("I:", rawText, ';', false);
    String anonymousIdentity = matchSinglePrefixedField("A:", rawText, ';', false);
    String eapMethod = matchSinglePrefixedField("E:", rawText, ';', false);
    String phase2Method = matchSinglePrefixedField("H:", rawText, ';', false);
    return new WifiParsedResult(type, ssid, pass, hidden, identity, anonymousIdentity, eapMethod, phase2Method);
  }
}