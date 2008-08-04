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

package com.google.zxing.client.rim.util;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Used to decode URL encoded characters.
 *
 * This code was contributed by LifeMarks.
 *
 * @author Matt York (matt@lifemarks.mobi)
 */
public final class URLDecoder {

  private URLDecoder() {
  }

  private static final Hashtable decodingMap;
  static {
    decodingMap = new Hashtable(37);
    decodingMap.put("%21", "!");
    decodingMap.put("%2A", "*");
    decodingMap.put("%2a", "*");
    decodingMap.put("%27", "'");
    decodingMap.put("%28", "(");
    decodingMap.put("%29", ")");
    decodingMap.put("%3B", ";");
    decodingMap.put("%3b", ";");
    decodingMap.put("%3A", ":");
    decodingMap.put("%3a", ":");
    decodingMap.put("%40", "@");
    decodingMap.put("%26", "&");
    decodingMap.put("%3D", "=");
    decodingMap.put("%3d", "=");
    decodingMap.put("%3B", "+");
    decodingMap.put("%3b", "+");
    decodingMap.put("%24", "$");
    decodingMap.put("%2C", "`");
    decodingMap.put("%2c", "`");
    decodingMap.put("%2F", "/");
    decodingMap.put("%2f", "/");
    decodingMap.put("%3F", "?");
    decodingMap.put("%3f", "?");
    decodingMap.put("%25", "%");
    decodingMap.put("%23", "#");
    decodingMap.put("%5B", "[");
    decodingMap.put("%5b", "[");
    decodingMap.put("%5D", "]");
    decodingMap.put("%5d", "]");
  }

  public static String decode(String uri) {
    Log.info("Original uri: " + uri);
    if (uri.indexOf('%') >= 0) { // skip this if no encoded chars
      Enumeration keys = decodingMap.keys();
      while (keys.hasMoreElements()) {
        String encodedChar = (String) keys.nextElement();
        int encodedCharIndex = uri.indexOf(encodedChar);
        while (encodedCharIndex != -1) {
          uri = uri.substring(0, encodedCharIndex) + decodingMap.get(encodedChar) + uri.substring(encodedCharIndex + encodedChar.length());
          encodedCharIndex = uri.indexOf(encodedChar, encodedCharIndex);
        }
      }
    }
    Log.info("Final URI: " + uri);
    return uri;
  }
}
