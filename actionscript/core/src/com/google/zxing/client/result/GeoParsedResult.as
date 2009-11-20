package com.google.zxing.client.result
{
	import com.google.zxing.common.flexdatatypes.StringBuilder;
	
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

/**
 * @author Sean Owen
 */
public final class GeoParsedResult extends ParsedResult {

  private var  geoURI:String;
  private var latitude:Number;
  private var longitude:Number;
  private var altitude:Number;

  public function GeoParsedResult(geoURI:String, latitude:Number, longitude:Number, altitude:Number ) {
    super(ParsedResultType.GEO);
    this.geoURI = geoURI;
    this.latitude = latitude;
    this.longitude = longitude;
    this.altitude = altitude;
  }

  public function getGeoURI():String {
    return geoURI;
  }

  /**
   * @return latitude in degrees
   */
  public function getLatitude():Number {
    return latitude;
  }

  /**
   * @return longitude in degrees
   */
  public function getLongitude():Number {
    return longitude;
  }

  /**
   * @return altitude in meters. If not specified, in the geo URI, returns 0.0
   */
  public function getAltitude():Number {
    return altitude;
  }

  public override function getDisplayResult():String {
    var result:StringBuilder = new StringBuilder(50);
    result.Append(latitude);
    result.Append(", ");
    result.Append(longitude);
    if (altitude > 0.0) {
      result.Append(", ");
      result.Append(altitude);
      result.Append('m');
    }
    return result.toString();
  }

  /**
   * @return a URI link to Google Maps which display the point on the Earth described
   *  by this instance, and sets the zoom level in a way that roughly reflects the
   *  altitude, if specified
   */
  /*
  public String getGoogleMapsURI() {
    StringBuffer result = new StringBuffer(50);
    result.append("http://maps.google.com/?ll=");
    result.append(latitude);
    result.append(',');
    result.append(longitude);
    if (altitude > 0.0f) {
      // Map altitude to zoom level, cleverly. Roughly, zoom level 19 is like a
      // view from 1000ft, 18 is like 2000ft, 17 like 4000ft, and so on.
      double altitudeInFeet = altitude * 3.28;
      int altitudeInKFeet = (int) (altitudeInFeet / 1000.0);
      // No Math.log() available here, so compute log base 2 the old fashioned way
      // Here logBaseTwo will take on a value between 0 and 18 actually
      int logBaseTwo = 0;
      while (altitudeInKFeet > 1 && logBaseTwo < 18) {
        altitudeInKFeet >>= 1;
        logBaseTwo++;
      }
      int zoom = 19 - logBaseTwo;
      result.append("&z=");
      result.append(zoom);
    }
    return result.toString();
  }
   */

}}