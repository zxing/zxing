/*
 * Copyright 2008 ZXing authors
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
 * @author Sean Owen
 */
public final class GeoParsedResult extends ParsedResult {

  private final double latitude;
  private final double longitude;
  private final double altitude;
  private final String query;

  GeoParsedResult(double latitude, double longitude, double altitude, String query) {
    super(ParsedResultType.GEO);
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
    this.query = query;
  }

  public String getGeoURI() {
    StringBuilder result = new StringBuilder();
    result.append("geo:");
    result.append(latitude);
    result.append(',');
    result.append(longitude);
    if (altitude > 0) {
      result.append(',');
      result.append(altitude);
    }
    if (query != null) {
      result.append('?');
      result.append(query);
    }
    return result.toString();
  }

  /**
   * @return latitude in degrees
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * @return longitude in degrees
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * @return altitude in meters. If not specified, in the geo URI, returns 0.0
   */
  public double getAltitude() {
    return altitude;
  }

  /**
   * @return query string associated with geo URI or null if none exists
   */
  public String getQuery() {
    return query;
  }

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(20);
    result.append(latitude);
    result.append(", ");
    result.append(longitude);
    if (altitude > 0.0) {
      result.append(", ");
      result.append(altitude);
      result.append('m');
    }
    if (query != null) {
      result.append(" (");
      result.append(query);
      result.append(')');
    }
    return result.toString();
  }

}