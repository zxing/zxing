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

package com.google.zxing.qrcode.decoder;

import java.util.Hashtable;

/**
 * Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1.
 *
 * @author srowen@google.com (Sean Owen)
 */
final class CharacterSetECI extends ECI {

  private static final Hashtable VALUE_TO_ECI;
  static {
    VALUE_TO_ECI = new Hashtable(29);
    // TODO figure out if these values are even right!
    addCharacterSet(3, "ISO8859_1");
    addCharacterSet(4, "ISO8859_2");
    addCharacterSet(5, "ISO8859_3");
    addCharacterSet(6, "ISO8859_4");
    addCharacterSet(7, "ISO8859_5");
    addCharacterSet(8, "ISO8859_6");
    addCharacterSet(9, "ISO8859_7");
    addCharacterSet(10, "ISO8859_8");
    addCharacterSet(11, "ISO8859_9");
    addCharacterSet(12, "ISO8859_10");
    addCharacterSet(13, "ISO8859_11");
    addCharacterSet(15, "ISO8859_13");
    addCharacterSet(16, "ISO8859_14");
    addCharacterSet(17, "ISO8859_15");
    addCharacterSet(18, "ISO8859_16");
    addCharacterSet(20, "Shift_JIS");
  }

  private final String encodingName;

  private CharacterSetECI(int value, String encodingName) {
    super(value);
    this.encodingName = encodingName;
  }

  String getEncodingName() {
    return encodingName;
  }

  private static void addCharacterSet(int value, String encodingName) {
    VALUE_TO_ECI.put(new Integer(value), new CharacterSetECI(value, encodingName));
  }

  static CharacterSetECI getCharacterSetECIByValue(int value) {
    CharacterSetECI eci = (CharacterSetECI) VALUE_TO_ECI.get(new Integer(value));
    if (eci == null) {
      throw new IllegalArgumentException("Unsupported value: " + value);
    }
    return eci;
  }

}