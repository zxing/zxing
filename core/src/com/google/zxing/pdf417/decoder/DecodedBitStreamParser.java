/*
 * Copyright 2009 ZXing authors
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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.DecoderResult;

/**
 * <p>This class contains the methods for decoding the PDF417 codewords.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
final class DecodedBitStreamParser {

  private static final int TEXT_COMPACTION_MODE_LATCH = 900;
  private static final int BYTE_COMPACTION_MODE_LATCH = 901;
  private static final int NUMERIC_COMPACTION_MODE_LATCH = 902;
  private static final int BYTE_COMPACTION_MODE_LATCH_6 = 924;
  private static final int BEGIN_MACRO_PDF417_CONTROL_BLOCK = 928;
  private static final int BEGIN_MACRO_PDF417_OPTIONAL_FIELD = 923;
  private static final int MACRO_PDF417_TERMINATOR = 922;
  private static final int MODE_SHIFT_TO_BYTE_COMPACTION_MODE = 913;
  private static final int MAX_NUMERIC_CODEWORDS = 15;

  private static final int ALPHA = 0;
  private static final int LOWER = 1;
  private static final int MIXED = 2;
  private static final int PUNCT = 3;
  private static final int PUNCT_SHIFT = 4;

  private static final int PL = 25;
  private static final int LL = 27;
  private static final int AS = 27;
  private static final int ML = 28;
  private static final int AL = 28;
  private static final int PS = 29;
  private static final int PAL = 29;

  private static final char[] PUNCT_CHARS = {';', '<', '>', '@', '[', 92, '}', '_', 96, '~', '!',
      13, 9, ',', ':', 10, '-', '.', '$', '/', 34, '|', '*',
      '(', ')', '?', '{', '}', 39};

  private static final char[] MIXED_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '&',
      13, 9, ',', ':', '#', '-', '.', '$', '/', '+', '%', '*',
      '=', '^'};

  // Table containing values for the exponent of 900.
  // This is used in the numeric compaction decode algorithm.
  private static final String[] EXP900 =
      {   "000000000000000000000000000000000000000000001",
          "000000000000000000000000000000000000000000900",
          "000000000000000000000000000000000000000810000",
          "000000000000000000000000000000000000729000000",
          "000000000000000000000000000000000656100000000",
          "000000000000000000000000000000590490000000000",
          "000000000000000000000000000531441000000000000",
          "000000000000000000000000478296900000000000000",
          "000000000000000000000430467210000000000000000",
          "000000000000000000387420489000000000000000000",
          "000000000000000348678440100000000000000000000",
          "000000000000313810596090000000000000000000000",
          "000000000282429536481000000000000000000000000",
          "000000254186582832900000000000000000000000000",
          "000228767924549610000000000000000000000000000",
          "205891132094649000000000000000000000000000000"};

  private DecodedBitStreamParser() {
  }

  static DecoderResult decode(int[] codewords) throws FormatException {
    StringBuffer result = new StringBuffer(100);
    // Get compaction mode
    int codeIndex = 1;
    int code = codewords[codeIndex++];
    while (codeIndex < codewords[0]) {
      switch (code) {
        case TEXT_COMPACTION_MODE_LATCH: {
          codeIndex = textCompaction(codewords, codeIndex, result);
          break;
        }
        case BYTE_COMPACTION_MODE_LATCH: {
          codeIndex = byteCompaction(code, codewords, codeIndex, result);
          break;
        }
        case NUMERIC_COMPACTION_MODE_LATCH: {
          codeIndex = numericCompaction(codewords, codeIndex, result);
          break;
        }
        case MODE_SHIFT_TO_BYTE_COMPACTION_MODE: {
          codeIndex = byteCompaction(code, codewords, codeIndex, result);
          break;
        }
        case BYTE_COMPACTION_MODE_LATCH_6: {
          codeIndex = byteCompaction(code, codewords, codeIndex, result);
          break;
        }
        default: {
          // Default to text compaction. During testing numerous barcodes
          // appeared to be missing the starting mode. In these cases defaulting
          // to text compaction seems to work.
          codeIndex--;
          codeIndex = textCompaction(codewords, codeIndex, result);
          break;
        }
      }
      if (codeIndex < codewords.length) {
        code = codewords[codeIndex++];
      } else {
        throw FormatException.getFormatInstance();
      }
    }
    return new DecoderResult(null, result.toString(), null, null);
  }

  /**
   * Text Compaction mode (see 5.4.1.5) permits all printable ASCII characters to be
   * encoded, i.e. values 32 - 126 inclusive in accordance with ISO/IEC 646 (IRV), as
   * well as selected control characters.
   *
   * @param codewords The array of codewords (data + error)
   * @param codeIndex The current index into the codeword array.
   * @param result    The decoded data is appended to the result.
   * @return The next index into the codeword array.
   */
  private static int textCompaction(int[] codewords, int codeIndex, StringBuffer result) {
    // 2 character per codeword
    int[] textCompactionData = new int[codewords[0] << 1];
    // Used to hold the byte compaction value if there is a mode shift
    int[] byteCompactionData = new int[codewords[0] << 1];

    int index = 0;
    boolean end = false;
    while ((codeIndex < codewords[0]) && !end) {
      int code = codewords[codeIndex++];
      if (code < TEXT_COMPACTION_MODE_LATCH) {
        textCompactionData[index] = code / 30;
        textCompactionData[index + 1] = code % 30;
        index += 2;
      } else {
        switch (code) {
          case TEXT_COMPACTION_MODE_LATCH: {
            codeIndex--;
            end = true;
            break;
          }
          case BYTE_COMPACTION_MODE_LATCH: {
            codeIndex--;
            end = true;
            break;
          }
          case NUMERIC_COMPACTION_MODE_LATCH: {
            codeIndex--;
            end = true;
            break;
          }
          case MODE_SHIFT_TO_BYTE_COMPACTION_MODE: {
            // The Mode Shift codeword 913 shall cause a temporary
            // switch from Text Compaction mode to Byte Compaction mode.
            // This switch shall be in effect for only the next codeword,
            // after which the mode shall revert to the prevailing sub-mode
            // of the Text Compaction mode. Codeword 913 is only available
            // in Text Compaction mode; its use is described in 5.4.2.4.
            textCompactionData[index] = MODE_SHIFT_TO_BYTE_COMPACTION_MODE;
            byteCompactionData[index] = code; //Integer.toHexString(code);
            index++;
            break;
          }
          case BYTE_COMPACTION_MODE_LATCH_6: {
            codeIndex--;
            end = true;
            break;
          }
        }
      }
    }
    decodeTextCompaction(textCompactionData, byteCompactionData, index, result);
    return codeIndex;
  }

  /**
   * The Text Compaction mode includes all the printable ASCII characters
   * (i.e. values from 32 to 126) and three ASCII control characters: HT or tab
   * (ASCII value 9), LF or line feed (ASCII value 10), and CR or carriage
   * return (ASCII value 13). The Text Compaction mode also includes various latch
   * and shift characters which are used exclusively within the mode. The Text
   * Compaction mode encodes up to 2 characters per codeword. The compaction rules
   * for converting data into PDF417 codewords are defined in 5.4.2.2. The sub-mode
   * switches are defined in 5.4.2.3.
   *
   * @param textCompactionData The text compaction data.
   * @param byteCompactionData The byte compaction data if there
   *                           was a mode shift.
   * @param length             The size of the text compaction and byte compaction data.
   * @param result             The decoded data is appended to the result.
   */
  private static void decodeTextCompaction(int[] textCompactionData,
                                           int[] byteCompactionData,
                                           int length,
                                           StringBuffer result) {
    // Beginning from an initial state of the Alpha sub-mode
    // The default compaction mode for PDF417 in effect at the start of each symbol shall always be Text
    // Compaction mode Alpha sub-mode (uppercase alphabetic). A latch codeword from another mode to the Text
    // Compaction mode shall always switch to the Text Compaction Alpha sub-mode.
    int subMode = ALPHA;
    int priorToShiftMode = ALPHA;
    int i = 0;
    while (i < length) {
      int subModeCh = textCompactionData[i];
      char ch = 0;
      switch (subMode) {
        case ALPHA:
          // Alpha (uppercase alphabetic)
          if (subModeCh < 26) {
            // Upper case Alpha Character
            ch = (char) ('A' + subModeCh);
          } else {
            if (subModeCh == 26) {
              ch = ' ';
            } else if (subModeCh == LL) {
              subMode = LOWER;
            } else if (subModeCh == ML) {
              subMode = MIXED;
            } else if (subModeCh == PS) {
              // Shift to punctuation
              priorToShiftMode = subMode;
              subMode = PUNCT_SHIFT;
            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
              result.append((char) byteCompactionData[i]);
            }
          }
          break;

        case LOWER:
          // Lower (lowercase alphabetic)
          if (subModeCh < 26) {
            ch = (char) ('a' + subModeCh);
          } else {
            if (subModeCh == 26) {
              ch = ' ';
            } else if (subModeCh == AL) {
              subMode = ALPHA;
            } else if (subModeCh == ML) {
              subMode = MIXED;
            } else if (subModeCh == PS) {
              // Shift to punctuation
              priorToShiftMode = subMode;
              subMode = PUNCT_SHIFT;
            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
              result.append((char) byteCompactionData[i]);
            }
          }
          break;

        case MIXED:
          // Mixed (numeric and some punctuation)
          if (subModeCh < PL) {
            ch = MIXED_CHARS[subModeCh];
          } else {
            if (subModeCh == PL) {
              subMode = PUNCT;
            } else if (subModeCh == 26) {
              ch = ' ';
            } else if (subModeCh == AS) {
              //mode_change = true;
            } else if (subModeCh == AL) {
              subMode = ALPHA;
            } else if (subModeCh == PS) {
              // Shift to punctuation
              priorToShiftMode = subMode;
              subMode = PUNCT_SHIFT;
            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
              result.append((char) byteCompactionData[i]);
            }
          }
          break;

        case PUNCT:
          // Punctuation
          if (subModeCh < PS) {
            ch = PUNCT_CHARS[subModeCh];
          } else {
            if (subModeCh == PAL) {
              subMode = ALPHA;
            } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
              result.append((char) byteCompactionData[i]);
            }
          }
          break;

        case PUNCT_SHIFT:
          // Restore sub-mode
          subMode = priorToShiftMode;
          if (subModeCh < PS) {
            ch = PUNCT_CHARS[subModeCh];
          } else {
            if (subModeCh == PAL) {
              subMode = ALPHA;
            }
          }
          break;
      }
      if (ch != 0) {
        // Append decoded character to result
        result.append(ch);
      }
      i++;
    }
  }

  /**
   * Byte Compaction mode (see 5.4.3) permits all 256 possible 8-bit byte values to be encoded.
   * This includes all ASCII characters value 0 to 127 inclusive and provides for international
   * character set support.
   *
   * @param mode      The byte compaction mode i.e. 901 or 924
   * @param codewords The array of codewords (data + error)
   * @param codeIndex The current index into the codeword array.
   * @param result    The decoded data is appended to the result.
   * @return The next index into the codeword array.
   */
  private static int byteCompaction(int mode, int[] codewords, int codeIndex, StringBuffer result) {
    if (mode == BYTE_COMPACTION_MODE_LATCH) {
      // Total number of Byte Compaction characters to be encoded
      // is not a multiple of 6
      int count = 0;
      long value = 0;
      char[] decodedData = new char[6];
      int[] byteCompactedCodewords = new int[6];
      boolean end = false;
      while ((codeIndex < codewords[0]) && !end) {
        int code = codewords[codeIndex++];
        if (code < TEXT_COMPACTION_MODE_LATCH) {
          byteCompactedCodewords[count] = code;
          count++;
          // Base 900
          value = 900 * value + code;
        } else {
          if (code == TEXT_COMPACTION_MODE_LATCH ||
              code == BYTE_COMPACTION_MODE_LATCH ||
              code == NUMERIC_COMPACTION_MODE_LATCH ||
              code == BYTE_COMPACTION_MODE_LATCH_6 ||
              code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
              code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
              code == MACRO_PDF417_TERMINATOR) {
            codeIndex--;
            end = true;
          }
        }
        if ((count % 5 == 0) && (count > 0)) {
          // Decode every 5 codewords
          // Convert to Base 256
          for (int j = 0; j < 6; ++j) {
            decodedData[5 - j] = (char) (value % 256);
            value >>= 8;
          }
          result.append(decodedData);
          count = 0;
        }
      }
      // If Byte Compaction mode is invoked with codeword 901,
      // the final group of codewords is interpreted directly
      // as one byte per codeword, without compaction.
      for (int i = ((count / 5) * 5); i < count; i++) {
        result.append((char) byteCompactedCodewords[i]);
      }

    } else if (mode == BYTE_COMPACTION_MODE_LATCH_6) {
      // Total number of Byte Compaction characters to be encoded
      // is an integer multiple of 6
      int count = 0;
      long value = 0;
      boolean end = false;
      while (codeIndex < codewords[0] && !end) {
        int code = codewords[codeIndex++];
        if (code < TEXT_COMPACTION_MODE_LATCH) {
          count++;
          // Base 900
          value = 900 * value + code;
        } else {
          if (code == TEXT_COMPACTION_MODE_LATCH ||
              code == BYTE_COMPACTION_MODE_LATCH ||
              code == NUMERIC_COMPACTION_MODE_LATCH ||
              code == BYTE_COMPACTION_MODE_LATCH_6 ||
              code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
              code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
              code == MACRO_PDF417_TERMINATOR) {
            codeIndex--;
            end = true;
          }
        }
        if ((count % 5 == 0) && (count > 0)) {
          // Decode every 5 codewords
          // Convert to Base 256
          char[] decodedData = new char[6];
          for (int j = 0; j < 6; ++j) {
            decodedData[5 - j] = (char) (value & 0xFF);
            value >>= 8;
          }
          result.append(decodedData);
        }
      }
    }
    return codeIndex;
  }

  /**
   * Numeric Compaction mode (see 5.4.4) permits efficient encoding of numeric data strings.
   *
   * @param codewords The array of codewords (data + error)
   * @param codeIndex The current index into the codeword array.
   * @param result    The decoded data is appended to the result.
   * @return The next index into the codeword array.
   */
  private static int numericCompaction(int[] codewords, int codeIndex, StringBuffer result) {
    int count = 0;
    boolean end = false;

    int[] numericCodewords = new int[MAX_NUMERIC_CODEWORDS];

    while (codeIndex < codewords[0] && !end) {
      int code = codewords[codeIndex++];
      if (codeIndex == codewords[0]) {
        end = true;
      }
      if (code < TEXT_COMPACTION_MODE_LATCH) {
        numericCodewords[count] = code;
        count++;
      } else {
        if (code == TEXT_COMPACTION_MODE_LATCH ||
            code == BYTE_COMPACTION_MODE_LATCH ||
            code == BYTE_COMPACTION_MODE_LATCH_6 ||
            code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
            code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
            code == MACRO_PDF417_TERMINATOR) {
          codeIndex--;
          end = true;          
        }
      }
      if (count % MAX_NUMERIC_CODEWORDS == 0 ||
          code == NUMERIC_COMPACTION_MODE_LATCH ||
          end) {
        // Re-invoking Numeric Compaction mode (by using codeword 902
        // while in Numeric Compaction mode) serves  to terminate the
        // current Numeric Compaction mode grouping as described in 5.4.4.2,
        // and then to start a new one grouping.
        String s = decodeBase900toBase10(numericCodewords, count);
        result.append(s);
        count = 0;
      }
    }
    return codeIndex;
  }

  /**
   * Convert a list of Numeric Compacted codewords from Base 900 to Base 10.
   *
   * @param codewords The array of codewords
   * @param count     The number of codewords
   * @return The decoded string representing the Numeric data.
   */
  /*
     EXAMPLE
     Encode the fifteen digit numeric string 000213298174000
     Prefix the numeric string with a 1 and set the initial value of
     t = 1 000 213 298 174 000
     Calculate codeword 0
     d0 = 1 000 213 298 174 000 mod 900 = 200

     t = 1 000 213 298 174 000 div 900 = 1 111 348 109 082
     Calculate codeword 1
     d1 = 1 111 348 109 082 mod 900 = 282

     t = 1 111 348 109 082 div 900 = 1 234 831 232
     Calculate codeword 2
     d2 = 1 234 831 232 mod 900 = 632

     t = 1 234 831 232 div 900 = 1 372 034
     Calculate codeword 3
     d3 = 1 372 034 mod 900 = 434

     t = 1 372 034 div 900 = 1 524
     Calculate codeword 4
     d4 = 1 524 mod 900 = 624

     t = 1 524 div 900 = 1
     Calculate codeword 5
     d5 = 1 mod 900 = 1
     t = 1 div 900 = 0
     Codeword sequence is: 1, 624, 434, 632, 282, 200

     Decode the above codewords involves
       1 x 900 power of 5 + 624 x 900 power of 4 + 434 x 900 power of 3 +
     632 x 900 power of 2 + 282 x 900 power of 1 + 200 x 900 power of 0 = 1000213298174000

     Remove leading 1 =>  Result is 000213298174000

     As there are huge numbers involved here we must use fake out the maths using string
     tokens for the numbers.
     BigDecimal is not supported by J2ME.
   */
  private static String decodeBase900toBase10(int[] codewords, int count) {
    StringBuffer accum = null;
    for (int i = 0; i < count; i++) {
      StringBuffer value = multiply(EXP900[count - i - 1], codewords[i]);
      if (accum == null) {
        // First time in accum=0
        accum = value;
      } else {
        accum = add(accum.toString(), value.toString());
      }
    }
    String result = null;
    // Remove leading '1' which was inserted to preserve
    // leading zeros
    for (int i = 0; i < accum.length(); i++) {
      if (accum.charAt(i) == '1') {
        //result = accum.substring(i + 1);
        result = accum.toString().substring(i + 1);
        break;
      }
    }
    if (result == null) {
      // No leading 1 => just write the converted number.
      result = accum.toString();
    }
    return result;
  }

  /**
   * Multiplies two String numbers
   *
   * @param value1 Any number represented as a string.
   * @param value2 A number <= 999.
   * @return the result of value1 * value2.
   */
  private static StringBuffer multiply(String value1, int value2) {
    StringBuffer result = new StringBuffer(value1.length());
    for (int i = 0; i < value1.length(); i++) {
      // Put zeros into the result.
      result.append('0');
    }
    int hundreds = value2 / 100;
    int tens = (value2 / 10) % 10;
    int ones = value2 % 10;
    // Multiply by ones
    for (int j = 0; j < ones; j++) {
      result = add(result.toString(), value1);
    }
    // Multiply by tens
    for (int j = 0; j < tens; j++) {
      result = add(result.toString(), (value1 + '0').substring(1));
    }
    // Multiply by hundreds
    for (int j = 0; j < hundreds; j++) {
      result = add(result.toString(), (value1 + "00").substring(2));
    }
    return result;
  }

  /**
   * Add two numbers which are represented as strings.
   *
   * @param value1
   * @param value2
   * @return the result of value1 + value2
   */
  private static StringBuffer add(String value1, String value2) {
    StringBuffer temp1 = new StringBuffer(5);
    StringBuffer temp2 = new StringBuffer(5);
    StringBuffer result = new StringBuffer(value1.length());
    for (int i = 0; i < value1.length(); i++) {
      // Put zeros into the result.
      result.append('0');
    }
    int carry = 0;
    for (int i = value1.length() - 3; i > -1; i -= 3) {

      temp1.setLength(0);
      temp1.append(value1.charAt(i));
      temp1.append(value1.charAt(i + 1));
      temp1.append(value1.charAt(i + 2));

      temp2.setLength(0);
      temp2.append(value2.charAt(i));
      temp2.append(value2.charAt(i + 1));
      temp2.append(value2.charAt(i + 2));

      int intValue1 = Integer.parseInt(temp1.toString());
      int intValue2 = Integer.parseInt(temp2.toString());

      int sumval = (intValue1 + intValue2 + carry) % 1000;
      carry = (intValue1 + intValue2 + carry) / 1000;

      result.setCharAt(i + 2, (char) ((sumval % 10) + '0'));
      result.setCharAt(i + 1, (char) (((sumval / 10) % 10) + '0'));
      result.setCharAt(i, (char) ((sumval / 100) + '0'));
    }
    return result;
  }

  /*
   private static String decodeBase900toBase10(int codewords[], int count) {
     BigInteger accum = BigInteger.valueOf(0);
     BigInteger value = null;
     for (int i = 0; i < count; i++) {
       value = BigInteger.valueOf(900).pow(count - i - 1);
       value = value.multiply(BigInteger.valueOf(codewords[i]));
       accum = accum.add(value);
     }
     if (debug) System.out.println("Big Integer " + accum);
     String result = accum.toString().substring(1);
     return result;
   }
   */
}
