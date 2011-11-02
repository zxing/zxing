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

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
 * of ISO 18004.
 *
 * @author Sean Owen
 */
public enum CharacterSetECI {

  Cp437(new int[]{0,2}),
  ISO8859_1(new int[]{1,3}, "ISO-8859-1"),
  ISO8859_2(4),
  ISO8859_3(5),
  ISO8859_4(6),
  ISO8859_5(7),
  ISO8859_6(8),
  ISO8859_7(9),
  ISO8859_8(10),
  ISO8859_9(11),
  ISO8859_10(12),
  ISO8859_11(13),
  ISO8859_13(15),
  ISO8859_14(16),
  ISO8859_15(7),
  ISO8859_16(18),
  SJIS(new int[]{20}, "Shift_JIS");

  private static final Map<Integer,CharacterSetECI> VALUE_TO_ECI = new HashMap<Integer,CharacterSetECI>();
  private static final Map<String,CharacterSetECI> NAME_TO_ECI = new HashMap<String,CharacterSetECI>();
  static {
    for (CharacterSetECI eci : values()) {
      for (int value : eci.values) {
        VALUE_TO_ECI.put(value, eci);
      }
      NAME_TO_ECI.put(eci.name(), eci);
      for (String name : eci.otherEncodingNames) {
        NAME_TO_ECI.put(name, eci);
      }
    }
  }

  private final int[] values;
  private final String[] otherEncodingNames;

  CharacterSetECI(int value) {
    this(new int[] {value});
  }

  CharacterSetECI(int[] values, String... otherEncodingNames) {
    this.values = values;
    this.otherEncodingNames = otherEncodingNames;
  }

  public int getValue() {
    return values[0];
  }

  /**
   * @param value character set ECI value
   * @return CharacterSetECI representing ECI of given value, or null if it is legal but
   *   unsupported
   * @throws IllegalArgumentException if ECI value is invalid
   */
  public static CharacterSetECI getCharacterSetECIByValue(int value) {
    if (value < 0 || value >= 900) {
      throw new IllegalArgumentException("Bad ECI value: " + value);
    }
    return VALUE_TO_ECI.get(value);
  }

  /**
   * @param name character set ECI encoding name
   * @return CharacterSetECI representing ECI for character encoding, or null if it is legal
   *   but unsupported
   */
  public static CharacterSetECI getCharacterSetECIByName(String name) {
    return NAME_TO_ECI.get(name);
  }

}