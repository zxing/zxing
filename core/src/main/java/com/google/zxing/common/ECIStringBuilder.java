/*
 * Copyright 2022 ZXing authors
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

import com.google.zxing.FormatException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Class that converts a sequence of ECIs and bytes into a string
 *
 * @author Alex Geller
 */
public final class ECIStringBuilder {
  private StringBuilder currentBytes;
  private StringBuilder result;
  private Charset currentCharset = StandardCharsets.ISO_8859_1;

  public ECIStringBuilder() {
    currentBytes = new StringBuilder();
  }
  public ECIStringBuilder(int initialCapacity) {
    currentBytes = new StringBuilder(initialCapacity);
  }

  /**
   * Appends {@code value} as a byte value
   *
   * @param value character whose lowest byte is to be appended
   */
  public void append(char value) {
    currentBytes.append((char) (value & 0xff));
  }

  /**
   * Appends {@code value} as a byte value
   *
   * @param value byte to append
   */
  public void append(byte value) {
    currentBytes.append((char) (value & 0xff));
  }

  /**
   * Appends the characters in {@code value} as bytes values
   *
   * @param value string to append
   */
  public void append(String value) {
    currentBytes.append(value);
  }

  /**
   * Append the string repesentation of {@code value} (short for {@code append(String.valueOf(value))})
   *
   * @param value int to append as a string
   */
  public void append(int value) {
    append(String.valueOf(value));
  }

  /**
   * Appends ECI value to output.
   *
   * @param value ECI value to append, as an int
   * @throws FormatException on invalid ECI value
   */
  public void appendECI(int value) throws FormatException {
    encodeCurrentBytesIfAny();
    CharacterSetECI characterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
    if (characterSetECI == null) {
      throw FormatException.getFormatInstance();
    }
    currentCharset = characterSetECI.getCharset();
  }

  private void encodeCurrentBytesIfAny() {
    if (currentCharset.equals(StandardCharsets.ISO_8859_1)) {
      if (currentBytes.length() > 0) {
        if (result == null) {
          result = currentBytes;
          currentBytes = new StringBuilder();
        } else {
          result.append(currentBytes);
          currentBytes = new StringBuilder();
        }
      }
    } else if (currentBytes.length() > 0) {
      byte[] bytes = currentBytes.toString().getBytes(StandardCharsets.ISO_8859_1);
      currentBytes = new StringBuilder();
      if (result == null) {
        result = new StringBuilder(new String(bytes, currentCharset));
      } else {
        result.append(new String(bytes, currentCharset));
      }
    }
  }

  /**
   * Appends the characters from {@code value} (unlike all other append methods of this class who append bytes)
   *
   * @param value characters to append
   */
  public void appendCharacters(StringBuilder value) {
    encodeCurrentBytesIfAny();
    result.append(value);
  }

  /**
   * Short for {@code toString().length()} (if possible, use {@link #isEmpty()} instead)
   *
   * @return length of string representation in characters
   */
  public int length() {
    return toString().length();
  }

  /**
   * @return true iff nothing has been appended
   */
  public boolean isEmpty() {
    return currentBytes.length() == 0 && (result == null || result.length() == 0);
  }

  @Override
  public String toString() {
    encodeCurrentBytesIfAny();
    return result == null ? "" : result.toString();
  }
}
