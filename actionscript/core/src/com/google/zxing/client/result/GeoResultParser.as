package com.google.zxing.client.result
{
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

import com.google.zxing.Result;

/**
 * Parses a "geo:" URI result, which specifices a location on the surface of
 * the Earth as well as an optional altitude above the surface. See
 * <a href="http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00">
 * http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00</a>.
 *
 * @author Sean Owen
 */
public final class GeoResultParser extends ResultParser {

  public function GeoResultParser() {
  }

  public static function  parse(result:Result ):GeoParsedResult {
    var rawText:String  = result.getText();
    if (rawText == null || (!(rawText.substr(0,4) == "geo:") && !(rawText.substr(0,4) == "GEO:"))) {
      return null;
    }
    // Drop geo, query portion
    var queryStart:int = rawText.indexOf('?', 4);
    var geoURIWithoutQuery:String = queryStart < 0 ? rawText.substring(4) : rawText.substring(4, queryStart);
    var latitudeEnd:int = geoURIWithoutQuery.indexOf(',');
    if (latitudeEnd < 0) {
      return null;
    }
    var longitudeEnd:int = geoURIWithoutQuery.indexOf(',', latitudeEnd + 1);    
    var latitude:Number, longitude:Number, altitude:Number;
    try {
      latitude = Number(geoURIWithoutQuery.substring(0, latitudeEnd));
      if (longitudeEnd < 0) {
        longitude = Number(geoURIWithoutQuery.substring(latitudeEnd + 1));
        altitude = 0.0;
      } else {
        longitude = Number(geoURIWithoutQuery.substring(latitudeEnd + 1, longitudeEnd));
        altitude = Number(geoURIWithoutQuery.substring(longitudeEnd + 1));
      }
    } catch (nfe:Error) {
      return null;
    }
    return new GeoParsedResult((rawText.substr(0,4) == "GEO:") ? "geo:" + rawText.substring(4) : rawText,
                               latitude, longitude, altitude);
  }

}
}