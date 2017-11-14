/*
 * Copyright 2006-2007 Jeremias Maerki.
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

package com.google.zxing.datamatrix.encoder;

import com.google.zxing.Dimension;

import java.util.Arrays;

/**
 * DataMatrix ECC 200 data encoder following the algorithm described in ISO/IEC 16022:200(E) in
 * annex S.
 */
public final class HighLevelEncoder {

  /**
   * Padding character
   */
  private static final char PAD = 129;
  /**
   * mode latch to C40 encodation mode
   */
  static final char LATCH_TO_C40 = 230;
  /**
   * mode latch to Base 256 encodation mode
   */
  static final char LATCH_TO_BASE256 = 231;
  /**
   * FNC1 Codeword
   */
  //private static final char FNC1 = 232;
  /**
   * Structured Append Codeword
   */
  //private static final char STRUCTURED_APPEND = 233;
  /**
   * Reader Programming
   */
  //private static final char READER_PROGRAMMING = 234;
  /**
   * Upper Shift
   */
  static final char UPPER_SHIFT = 235;
  /**
   * 05 Macro
   */
  private static final char MACRO_05 = 236;
  /**
   * 06 Macro
   */
  private static final char MACRO_06 = 237;
  /**
   * mode latch to ANSI X.12 encodation mode
   */
  static final char LATCH_TO_ANSIX12 = 238;
  /**
   * mode latch to Text encodation mode
   */
  static final char LATCH_TO_TEXT = 239;
  /**
   * mode latch to EDIFACT encodation mode
   */
  static final char LATCH_TO_EDIFACT = 240;
  /**
   * ECI character (Extended Channel Interpretation)
   */
  //private static final char ECI = 241;

  /**
   * Unlatch from C40 encodation
   */
  static final char C40_UNLATCH = 254;
  /**
   * Unlatch from X12 encodation
   */
  static final char X12_UNLATCH = 254;

  /**
   * 05 Macro header
   */
  private static final String MACRO_05_HEADER = "[)>\u001E05\u001D";
  /**
   * 06 Macro header
   */
  private static final String MACRO_06_HEADER = "[)>\u001E06\u001D";
  /**
   * Macro trailer
   */
  private static final String MACRO_TRAILER = "\u001E\u0004";

  static final int ASCII_ENCODATION = 0;
  static final int C40_ENCODATION = 1;
  static final int TEXT_ENCODATION = 2;
  static final int X12_ENCODATION = 3;
  static final int EDIFACT_ENCODATION = 4;
  static final int BASE256_ENCODATION = 5;

  private HighLevelEncoder() {
  }

  /*
   * Converts the message to a byte array using the default encoding (cp437) as defined by the
   * specification
   *
   * @param msg the message
   * @return the byte array of the message
   */

  /*
  public static byte[] getBytesForMessage(String msg) {
    return msg.getBytes(Charset.forName("cp437")); //See 4.4.3 and annex B of ISO/IEC 15438:2001(E)
  }
   */

  private static char randomize253State(char ch, int codewordPosition) {
    int pseudoRandom = ((149 * codewordPosition) % 253) + 1;
    int tempVariable = ch + pseudoRandom;
    return (char) (tempVariable <= 254 ? tempVariable : tempVariable - 254);
  }

  /**
   * Performs message encoding of a DataMatrix message using the algorithm described in annex P
   * of ISO/IEC 16022:2000(E).
   *
   * @param msg the message
   * @return the encoded message (the char values range from 0 to 255)
   */
  public static String encodeHighLevel(String msg) {
    return encodeHighLevel(msg, SymbolShapeHint.FORCE_NONE, null, null);
  }

  /**
   * Performs message encoding of a DataMatrix message using the algorithm described in annex P
   * of ISO/IEC 16022:2000(E).
   *
   * @param msg     the message
   * @param shape   requested shape. May be {@code SymbolShapeHint.FORCE_NONE},
   *                {@code SymbolShapeHint.FORCE_SQUARE} or {@code SymbolShapeHint.FORCE_RECTANGLE}.
   * @param minSize the minimum symbol size constraint or null for no constraint
   * @param maxSize the maximum symbol size constraint or null for no constraint
   * @return the encoded message (the char values range from 0 to 255)
   */
  public static String encodeHighLevel(String msg,
                                       SymbolShapeHint shape,
                                       Dimension minSize,
                                       Dimension maxSize) {
    //the codewords 0..255 are encoded as Unicode characters
    Encoder[] encoders = {
        new ASCIIEncoder(), new C40Encoder(), new TextEncoder(),
        new X12Encoder(), new EdifactEncoder(),  new Base256Encoder()
    };

    EncoderContext context = new EncoderContext(msg);
    context.setSymbolShape(shape);
    context.setSizeConstraints(minSize, maxSize);

    if (msg.startsWith(MACRO_05_HEADER) && msg.endsWith(MACRO_TRAILER)) {
      context.writeCodeword(MACRO_05);
      context.setSkipAtEnd(2);
      context.pos += MACRO_05_HEADER.length();
    } else if (msg.startsWith(MACRO_06_HEADER) && msg.endsWith(MACRO_TRAILER)) {
      context.writeCodeword(MACRO_06);
      context.setSkipAtEnd(2);
      context.pos += MACRO_06_HEADER.length();
    }

    int encodingMode = ASCII_ENCODATION; //Default mode
    while (context.hasMoreCharacters()) {
      encoders[encodingMode].encode(context);
      if (context.getNewEncoding() >= 0) {
        encodingMode = context.getNewEncoding();
        context.resetEncoderSignal();
      }
    }
    int len = context.getCodewordCount();
    context.updateSymbolInfo();
    int capacity = context.getSymbolInfo().getDataCapacity();
    if (len < capacity &&
        encodingMode != ASCII_ENCODATION &&
        encodingMode != BASE256_ENCODATION &&
        encodingMode != EDIFACT_ENCODATION) {
      context.writeCodeword('\u00fe'); //Unlatch (254)
    }
    //Padding
    StringBuilder codewords = context.getCodewords();
    if (codewords.length() < capacity) {
      codewords.append(PAD);
    }
    while (codewords.length() < capacity) {
      codewords.append(randomize253State(PAD, codewords.length() + 1));
    }

    return context.getCodewords().toString();
  }

  static int lookAheadTest(CharSequence msg, int startpos, int currentMode) {
    if (startpos >= msg.length()) {
      return currentMode;
    }
    float[] charCounts;
    //step J
    if (currentMode == ASCII_ENCODATION) {
      charCounts = new float[]{0, 1, 1, 1, 1, 1.25f};
    } else {
      charCounts = new float[]{1, 2, 2, 2, 2, 2.25f};
      charCounts[currentMode] = 0;
    }

    int charsProcessed = 0;
    while (true) {
      //step K
      if ((startpos + charsProcessed) == msg.length()) {
        int min = Integer.MAX_VALUE;
        byte[] mins = new byte[6];
        int[] intCharCounts = new int[6];
        min = findMinimums(charCounts, intCharCounts, min, mins);
        int minCount = getMinimumCount(mins);

        if (intCharCounts[ASCII_ENCODATION] == min) {
          return ASCII_ENCODATION;
        }
        if (minCount == 1 && mins[BASE256_ENCODATION] > 0) {
          return BASE256_ENCODATION;
        }
        if (minCount == 1 && mins[EDIFACT_ENCODATION] > 0) {
          return EDIFACT_ENCODATION;
        }
        if (minCount == 1 && mins[TEXT_ENCODATION] > 0) {
          return TEXT_ENCODATION;
        }
        if (minCount == 1 && mins[X12_ENCODATION] > 0) {
          return X12_ENCODATION;
        }
        return C40_ENCODATION;
      }

      char c = msg.charAt(startpos + charsProcessed);
      charsProcessed++;

      //step L
      if (isDigit(c)) {
        charCounts[ASCII_ENCODATION] += 0.5f;
      } else if (isExtendedASCII(c)) {
        charCounts[ASCII_ENCODATION] = (float) Math.ceil(charCounts[ASCII_ENCODATION]);
        charCounts[ASCII_ENCODATION] += 2.0f;
      } else {
        charCounts[ASCII_ENCODATION] = (float) Math.ceil(charCounts[ASCII_ENCODATION]);
        charCounts[ASCII_ENCODATION]++;
      }

      //step M
      if (isNativeC40(c)) {
        charCounts[C40_ENCODATION] += 2.0f / 3.0f;
      } else if (isExtendedASCII(c)) {
        charCounts[C40_ENCODATION] += 8.0f / 3.0f;
      } else {
        charCounts[C40_ENCODATION] += 4.0f / 3.0f;
      }

      //step N
      if (isNativeText(c)) {
        charCounts[TEXT_ENCODATION] += 2.0f / 3.0f;
      } else if (isExtendedASCII(c)) {
        charCounts[TEXT_ENCODATION] += 8.0f / 3.0f;
      } else {
        charCounts[TEXT_ENCODATION] += 4.0f / 3.0f;
      }

      //step O
      if (isNativeX12(c)) {
        charCounts[X12_ENCODATION] += 2.0f / 3.0f;
      } else if (isExtendedASCII(c)) {
        charCounts[X12_ENCODATION] += 13.0f / 3.0f;
      } else {
        charCounts[X12_ENCODATION] += 10.0f / 3.0f;
      }

      //step P
      if (isNativeEDIFACT(c)) {
        charCounts[EDIFACT_ENCODATION] += 3.0f / 4.0f;
      } else if (isExtendedASCII(c)) {
        charCounts[EDIFACT_ENCODATION] += 17.0f / 4.0f;
      } else {
        charCounts[EDIFACT_ENCODATION] += 13.0f / 4.0f;
      }

      // step Q
      if (isSpecialB256(c)) {
        charCounts[BASE256_ENCODATION] += 4.0f;
      } else {
        charCounts[BASE256_ENCODATION]++;
      }

      //step R
      if (charsProcessed >= 4) {
        int[] intCharCounts = new int[6];
        byte[] mins = new byte[6];
        findMinimums(charCounts, intCharCounts, Integer.MAX_VALUE, mins);
        int minCount = getMinimumCount(mins);

        if (intCharCounts[ASCII_ENCODATION] < intCharCounts[BASE256_ENCODATION]
            && intCharCounts[ASCII_ENCODATION] < intCharCounts[C40_ENCODATION]
            && intCharCounts[ASCII_ENCODATION] < intCharCounts[TEXT_ENCODATION]
            && intCharCounts[ASCII_ENCODATION] < intCharCounts[X12_ENCODATION]
            && intCharCounts[ASCII_ENCODATION] < intCharCounts[EDIFACT_ENCODATION]) {
          return ASCII_ENCODATION;
        }
        if (intCharCounts[BASE256_ENCODATION] < intCharCounts[ASCII_ENCODATION]
            || (mins[C40_ENCODATION] + mins[TEXT_ENCODATION] + mins[X12_ENCODATION] + mins[EDIFACT_ENCODATION]) == 0) {
          return BASE256_ENCODATION;
        }
        if (minCount == 1 && mins[EDIFACT_ENCODATION] > 0) {
          return EDIFACT_ENCODATION;
        }
        if (minCount == 1 && mins[TEXT_ENCODATION] > 0) {
          return TEXT_ENCODATION;
        }
        if (minCount == 1 && mins[X12_ENCODATION] > 0) {
          return X12_ENCODATION;
        }
        if (intCharCounts[C40_ENCODATION] + 1 < intCharCounts[ASCII_ENCODATION]
            && intCharCounts[C40_ENCODATION] + 1 < intCharCounts[BASE256_ENCODATION]
            && intCharCounts[C40_ENCODATION] + 1 < intCharCounts[EDIFACT_ENCODATION]
            && intCharCounts[C40_ENCODATION] + 1 < intCharCounts[TEXT_ENCODATION]) {
          if (intCharCounts[C40_ENCODATION] < intCharCounts[X12_ENCODATION]) {
            return C40_ENCODATION;
          }
          if (intCharCounts[C40_ENCODATION] == intCharCounts[X12_ENCODATION]) {
            int p = startpos + charsProcessed + 1;
            while (p < msg.length()) {
              char tc = msg.charAt(p);
              if (isX12TermSep(tc)) {
                return X12_ENCODATION;
              }
              if (!isNativeX12(tc)) {
                break;
              }
              p++;
            }
            return C40_ENCODATION;
          }
        }
      }
    }
  }

  private static int findMinimums(float[] charCounts, int[] intCharCounts, int min, byte[] mins) {
    Arrays.fill(mins, (byte) 0);
    for (int i = 0; i < 6; i++) {
      intCharCounts[i] = (int) Math.ceil(charCounts[i]);
      int current = intCharCounts[i];
      if (min > current) {
        min = current;
        Arrays.fill(mins, (byte) 0);
      }
      if (min == current) {
        mins[i]++;

      }
    }
    return min;
  }

  private static int getMinimumCount(byte[] mins) {
    int minCount = 0;
    for (int i = 0; i < 6; i++) {
      minCount += mins[i];
    }
    return minCount;
  }

  static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  static boolean isExtendedASCII(char ch) {
    return ch >= 128 && ch <= 255;
  }

  private static boolean isNativeC40(char ch) {
    return (ch == ' ') || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z');
  }

  private static boolean isNativeText(char ch) {
    return (ch == ' ') || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z');
  }

  private static boolean isNativeX12(char ch) {
    return isX12TermSep(ch) || (ch == ' ') || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z');
  }

  private static boolean isX12TermSep(char ch) {
    return (ch == '\r') //CR
        || (ch == '*')
        || (ch == '>');
  }

  private static boolean isNativeEDIFACT(char ch) {
    return ch >= ' ' && ch <= '^';
  }

  private static boolean isSpecialB256(char ch) {
    return false; //TODO NOT IMPLEMENTED YET!!!
  }

  /**
   * Determines the number of consecutive characters that are encodable using numeric compaction.
   *
   * @param msg      the message
   * @param startpos the start position within the message
   * @return the requested character count
   */
  public static int determineConsecutiveDigitCount(CharSequence msg, int startpos) {
    int count = 0;
    int len = msg.length();
    int idx = startpos;
    if (idx < len) {
      char ch = msg.charAt(idx);
      while (isDigit(ch) && idx < len) {
        count++;
        idx++;
        if (idx < len) {
          ch = msg.charAt(idx);
        }
      }
    }
    return count;
  }

  static void illegalCharacter(char c) {
    String hex = Integer.toHexString(c);
    hex = "0000".substring(0, 4 - hex.length()) + hex;
    throw new IllegalArgumentException("Illegal character: " + c + " (0x" + hex + ')');
  }

}
