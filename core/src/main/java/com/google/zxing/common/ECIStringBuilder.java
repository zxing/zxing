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
   * appends {@code value} as a byte value
   */
  public void append(char value) {
    currentBytes.append((char) (value & 0xff));
  }

  public void append(byte value) {
    currentBytes.append((char) (value & 0xff));
  }

  /**
   * appends the characters in {@code value} as bytes values
   */
  public void append(String value) {
    currentBytes.append(value);
  }

  /**
   * short for {@code append("" + value)}
   */
  public void append(int value) {
    append("" + value);
  }

  public void appendECI(int value) throws FormatException {
    encodeCurrentBytesIfAny();
    CharacterSetECI characterSetECI = CharacterSetECI.getCharacterSetECIByValue(value);
    if (characterSetECI == null) {
      throw FormatException.getFormatInstance(new RuntimeException("Unsupported ECI value " + value));
    }
    currentCharset = characterSetECI.getCharset();
  }

  private void encodeCurrentBytesIfAny() {
    if (currentCharset == StandardCharsets.ISO_8859_1) {
      if (currentBytes.length() > 0) {
        if (result == null) {
          result = currentBytes;
          currentBytes = new StringBuilder();
        } else {
          result.append(currentBytes);
          currentBytes.setLength(0);
        }
      }
    } else if (currentBytes.length() > 0) {
      byte[] bytes = new byte[currentBytes.length()];
      for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte) (currentBytes.charAt(i) & 0xff);
      }
      currentBytes.setLength(0);
      if (result == null) {
        result = new StringBuilder(new String(bytes, currentCharset));
      } else {
        result.append(new String(bytes, currentCharset));
      }
    }
  }

  /**
   * appends the characters from sb (unlike all other append methods of this class who append bytes)
   */
  public void appendCharacters(StringBuilder value) {
    encodeCurrentBytesIfAny();
    result.append(value);
  }

  /**
   * short for {@code toString().length()} (if possible, use {@link #isEmpty()} instead)
   */
  public int length() {
    return toString().length();
  }

  public boolean isEmpty() {
    return currentBytes.length() == 0 && (result == null || result.length() == 0);
  }

  @Override
  public String toString() {
    encodeCurrentBytesIfAny();
    return result == null ? "" : result.toString();
  }
}
