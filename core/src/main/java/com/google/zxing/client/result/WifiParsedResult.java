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

/**
 * Represents a parsed result that encodes wifi network information, like SSID and password.
 *
 * @author Vikram Aggarwal
 */
public final class WifiParsedResult extends ParsedResult {

  private final String ssid;
  private final String networkEncryption;
  private final String password;
  private final boolean hidden;

  public WifiParsedResult(String networkEncryption, String ssid, String password) {
    this(networkEncryption, ssid, password, false);
  }

  public WifiParsedResult(String networkEncryption, String ssid, String password, boolean hidden) {
    super(ParsedResultType.WIFI);
    this.ssid = ssid;
    this.networkEncryption = networkEncryption;
    this.password = password;
    this.hidden = hidden;
  }

  public String getSsid() {
    return ssid;
  }

  public String getNetworkEncryption() {
    return networkEncryption;
  }

  public String getPassword() {
    return password;
  }

  public boolean isHidden() {
    return hidden;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(80);
    maybeAppend(ssid, result);
    maybeAppend(networkEncryption, result);
    maybeAppend(password, result);
    maybeAppend(Boolean.toString(hidden), result);
    return result.toString();
  }

}