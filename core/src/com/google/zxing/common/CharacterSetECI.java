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

package com.google.zxing.common;

import java.util.Hashtable;

/**
 * Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
 * of ISO 18004.
 *
 * @author Sean Owen
 */
public final class CharacterSetECI extends ECI {

  private static Hashtable VALUE_TO_ECI;
  private static Hashtable NAME_TO_ECI;

  private static void initialize() {
    VALUE_TO_ECI = new Hashtable(29);
    NAME_TO_ECI = new Hashtable(29);
    // TODO figure out if these values are even right!
    addCharacterSet(0, "Cp437");
    addCharacterSet(1, new String[] {"ISO8859_1", "ISO-8859-1"});
    addCharacterSet(2, "Cp437");
    addCharacterSet(3, new String[] {"ISO8859_1", "ISO-8859-1"});
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
    addCharacterSet(20, new String[] {"SJIS", "Shift_JIS"});
  }

  private final String encodingName;

  private CharacterSetECI(int value, String encodingName) {
    super(value);
    this.encodingName = encodingName;
  }

  public String getEncodingName() {
    return encodingName;
  }

  private static void addCharacterSet(int value, String encodingName) {
    CharacterSetECI eci = new CharacterSetECI(value, encodingName);
    VALUE_TO_ECI.put(new Integer(value), eci); // can't use valueOf
    NAME_TO_ECI.put(encodingName, eci);
  }

  private static void addCharacterSet(int value, String[] encodingNames) {
    CharacterSetECI eci = new CharacterSetECI(value, encodingNames[0]);
    VALUE_TO_ECI.put(new Integer(value), eci); // can't use valueOf
    for (int i = 0; i < encodingNames.length; i++) {
      NAME_TO_ECI.put(encodingNames[i], eci);
    }
  }

  /**
   * @param value character set ECI value
   * @return {@link CharacterSetECI} representing ECI of given value, or null if it is legal but
   *   unsupported
   * @throws IllegalArgumentException if ECI value is invalid
   */
  public static CharacterSetECI getCharacterSetECIByValue(int value) {
    if (VALUE_TO_ECI == null) {
      initialize();
    }
    if (value < 0 || value >= 900) {
      throw new IllegalArgumentException("Bad ECI value: " + value);
    }
    return (CharacterSetECI) VALUE_TO_ECI.get(new Integer(value));
  }

  /**
   * @param name character set ECI encoding name
   * @return {@link CharacterSetECI} representing ECI for character encoding, or null if it is legal
   *   but unsupported
   */
  public static CharacterSetECI getCharacterSetECIByName(String name) {
    if (NAME_TO_ECI == null) {
      initialize();
    }
    return (CharacterSetECI) NAME_TO_ECI.get(name);
  }

}