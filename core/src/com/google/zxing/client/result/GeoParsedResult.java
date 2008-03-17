/*
 * Copyright 2008 Google Inc.
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
 * Represents a "geo:" URI result. See
 * <a href="http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00">
 * http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00</a>.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class GeoParsedResult extends ParsedReaderResult {

  private final float latitude;
  private final float longitude;
  private final float altitude;

  private GeoParsedResult(float latitude, float longitude, float altitude) {
    super(ParsedReaderResultType.GEO);
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
  }

  public static GeoParsedResult parse(Result result) {
    String rawText = result.getText();
    if (!rawText.startsWith("geo:")) {
      return null;
    }
    // Drop geo, query portion
    int queryStart = rawText.indexOf('?', 4);
    if (queryStart < 0) {
      rawText = rawText.substring(4);
    } else {
      rawText = rawText.substring(4, queryStart);
    }
    int latitudeEnd = rawText.indexOf(',');
    if (latitudeEnd < 0) {
      return null;
    }
    float latitude = Float.parseFloat(rawText.substring(0, latitudeEnd));
    int longitudeEnd = rawText.indexOf(',', latitudeEnd + 1);
    float longitude;
    float altitude;
    if (longitudeEnd < 0) {
      longitude = Float.parseFloat(rawText.substring(latitudeEnd + 1));
      altitude = 0.0f;
    } else {
      longitude = Float.parseFloat(rawText.substring(latitudeEnd + 1, longitudeEnd));
      altitude = Float.parseFloat(rawText.substring(longitudeEnd + 1));
    }
    return new GeoParsedResult(latitude, longitude, altitude);
  }

  /**
   * @return latitude in degrees
   */
  public float getLatitude() {
    return latitude;
  }

  /**
   * @return longitude in degrees
   */
  public float getLongitude() {
    return longitude;
  }

  /**
   * @return altitude in meters. If not specified, in the geo URI, returns 0.0
   */
  public float getAltitude() {
    return altitude;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(50);
    result.append(latitude);
    result.append("deg N, ");
    result.append(longitude);
    result.append("deg E, ");
    result.append(altitude);
    result.append('m');
    return result.toString();
  }

}