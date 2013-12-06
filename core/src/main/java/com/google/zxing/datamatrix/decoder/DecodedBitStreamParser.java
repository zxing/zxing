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

package com.google.zxing.datamatrix.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.common.DecoderResult;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Data Matrix Codes can encode text as bits in one of several modes, and can use multiple modes
 * in one Data Matrix Code. This class decodes the bits back into text.</p>
 *
 * <p>See ISO 16022:2006, 5.2.1 - 5.2.9.2</p>
 *
 * @author bbrown@google.com (Brian Brown)
 * @author Sean Owen
 */
final class DecodedBitStreamParser {

  private enum Mode {
    PAD_ENCODE, // Not really a mode
    ASCII_ENCODE,
    C40_ENCODE,
    TEXT_ENCODE,
    ANSIX12_ENCODE,
    EDIFACT_ENCODE,
    BASE256_ENCODE
  }

  /**
   * See ISO 16022:2006, Annex C Table C.1
   * The C40 Basic Character Set (*'s used for placeholders for the shift values)
   */
  private static final char[] C40_BASIC_SET_CHARS = {
    '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
  };

  private static final char[] C40_SHIFT2_SET_CHARS = {
    '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*',  '+', ',', '-', '.',
    '/', ':', ';', '<', '=', '>', '?',  '@', '[', '\\', ']', '^', '_'
  };

  /**
   * See ISO 16022:2006, Annex C Table C.2
   * The Text Basic Character Set (*'s used for placeholders for the shift values)
   */
  private static final char[] TEXT_BASIC_SET_CHARS = {
    '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
  };

  private static final char[] TEXT_SHIFT3_SET_CHARS = {
    '`', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O',  'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', (char) 127
  };

  private DecodedBitStreamParser() {
  }

  static DecoderResult decode(byte[] bytes) throws FormatException {
    BitSource bits = new BitSource(bytes);
    StringBuilder result = new StringBuilder(100);
    StringBuilder resultTrailer = new StringBuilder(0);
    List<byte[]> byteSegments = new ArrayList<>(1);
    Mode mode = Mode.ASCII_ENCODE;
    do {
      if (mode == Mode.ASCII_ENCODE) {
        mode = decodeAsciiSegment(bits, result, resultTrailer);
      } else {
        switch (mode) {
          case C40_ENCODE:
            decodeC40Segment(bits, result);
            break;
          case TEXT_ENCODE:
            decodeTextSegment(bits, result);
            break;
          case ANSIX12_ENCODE:
            decodeAnsiX12Segment(bits, result);
            break;
          case EDIFACT_ENCODE:
            decodeEdifactSegment(bits, result);
            break;
          case BASE256_ENCODE:
            decodeBase256Segment(bits, result, byteSegments);
            break;
          default:
            throw FormatException.getFormatInstance();
        }
        mode = Mode.ASCII_ENCODE;
      }
    } while (mode != Mode.PAD_ENCODE && bits.available() > 0);
    if (resultTrailer.length() > 0) {
      result.append(resultTrailer);
    }
    return new DecoderResult(bytes, result.toString(), byteSegments.isEmpty() ? null : byteSegments, null);
  }

  /**
   * See ISO 16022:2006, 5.2.3 and Annex C, Table C.2
   */
  private static Mode decodeAsciiSegment(BitSource bits,
                                         StringBuilder result,
                                         StringBuilder resultTrailer) throws FormatException {
    boolean upperShift = false;
    do {
      int oneByte = bits.readBits(8);
      if (oneByte == 0) {
        throw FormatException.getFormatInstance();
      } else if (oneByte <= 128) {  // ASCII data (ASCII value + 1)
        if (upperShift) {
          oneByte += 128;
          //upperShift = false;
        }
        result.append((char) (oneByte - 1));
        return Mode.ASCII_ENCODE;
      } else if (oneByte == 129) {  // Pad
        return Mode.PAD_ENCODE;
      } else if (oneByte <= 229) {  // 2-digit data 00-99 (Numeric Value + 130)
        int value = oneByte - 130;
        if (value < 10) { // padd with '0' for single digit values
          result.append('0');
        }
        result.append(value);
      } else if (oneByte == 230) {  // Latch to C40 encodation
        return Mode.C40_ENCODE;
      } else if (oneByte == 231) {  // Latch to Base 256 encodation
        return Mode.BASE256_ENCODE;
      } else if (oneByte == 232) {
        // FNC1
        result.append((char) 29); // translate as ASCII 29
      } else if (oneByte == 233 || oneByte == 234) {
        // Structured Append, Reader Programming
        // Ignore these symbols for now
        //throw ReaderException.getInstance();
      } else if (oneByte == 235) {  // Upper Shift (shift to Extended ASCII)
        upperShift = true;
      } else if (oneByte == 236) {  // 05 Macro
        result.append("[)>\u001E05\u001D");
        resultTrailer.insert(0, "\u001E\u0004");
      } else if (oneByte == 237) {  // 06 Macro
        result.append("[)>\u001E06\u001D");
        resultTrailer.insert(0, "\u001E\u0004");
      } else if (oneByte == 238) {  // Latch to ANSI X12 encodation
        return Mode.ANSIX12_ENCODE;
      } else if (oneByte == 239) {  // Latch to Text encodation
        return Mode.TEXT_ENCODE;
      } else if (oneByte == 240) {  // Latch to EDIFACT encodation
        return Mode.EDIFACT_ENCODE;
      } else if (oneByte == 241) {  // ECI Character
        // TODO(bbrown): I think we need to support ECI
        //throw ReaderException.getInstance();
        // Ignore this symbol for now
      } else if (oneByte >= 242) {  // Not to be used in ASCII encodation
        // ... but work around encoders that end with 254, latch back to ASCII
        if (oneByte != 254 || bits.available() != 0) {
          throw FormatException.getFormatInstance();
        }
      }
    } while (bits.available() > 0);
    return Mode.ASCII_ENCODE;
  }

  /**
   * See ISO 16022:2006, 5.2.5 and Annex C, Table C.1
   */
  private static void decodeC40Segment(BitSource bits, StringBuilder result) throws FormatException {
    // Three C40 values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1
    // TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
    boolean upperShift = false;

    int[] cValues = new int[3];
    int shift = 0;

    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return;
      }
      int firstByte = bits.readBits(8);
      if (firstByte == 254) {  // Unlatch codeword
        return;
      }

      parseTwoBytes(firstByte, bits.readBits(8), cValues);

      for (int i = 0; i < 3; i++) {
        int cValue = cValues[i];
        switch (shift) {
          case 0:
            if (cValue < 3) {
              shift = cValue + 1;
            } else if (cValue < C40_BASIC_SET_CHARS.length) {
              char c40char = C40_BASIC_SET_CHARS[cValue];
              if (upperShift) {
                result.append((char) (c40char + 128));
                upperShift = false;
              } else {
                result.append(c40char);
              }
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 1:
            if (upperShift) {
              result.append((char) (cValue + 128));
              upperShift = false;
            } else {
              result.append((char) cValue);
            }
            shift = 0;
            break;
          case 2:
            if (cValue < C40_SHIFT2_SET_CHARS.length) {
              char c40char = C40_SHIFT2_SET_CHARS[cValue];
              if (upperShift) {
                result.append((char) (c40char + 128));
                upperShift = false;
              } else {
                result.append(c40char);
              }
            } else if (cValue == 27) {  // FNC1
              result.append((char) 29); // translate as ASCII 29
            } else if (cValue == 30) {  // Upper Shift
              upperShift = true;
            } else {
              throw FormatException.getFormatInstance();
            }
            shift = 0;
            break;
          case 3:
            if (upperShift) {
              result.append((char) (cValue + 224));
              upperShift = false;
            } else {
              result.append((char) (cValue + 96));
            }
            shift = 0;
            break;
          default:
            throw FormatException.getFormatInstance();
        }
      }
    } while (bits.available() > 0);
  }

  /**
   * See ISO 16022:2006, 5.2.6 and Annex C, Table C.2
   */
  private static void decodeTextSegment(BitSource bits, StringBuilder result) throws FormatException {
    // Three Text values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1
    // TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
    boolean upperShift = false;

    int[] cValues = new int[3];
    int shift = 0;
    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return;
      }
      int firstByte = bits.readBits(8);
      if (firstByte == 254) {  // Unlatch codeword
        return;
      }

      parseTwoBytes(firstByte, bits.readBits(8), cValues);

      for (int i = 0; i < 3; i++) {
        int cValue = cValues[i];
        switch (shift) {
          case 0:
            if (cValue < 3) {
              shift = cValue + 1;
            } else if (cValue < TEXT_BASIC_SET_CHARS.length) {
              char textChar = TEXT_BASIC_SET_CHARS[cValue];
              if (upperShift) {
                result.append((char) (textChar + 128));
                upperShift = false;
              } else {
                result.append(textChar);
              }
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case 1:
            if (upperShift) {
              result.append((char) (cValue + 128));
              upperShift = false;
            } else {
              result.append((char) cValue);
            }
            shift = 0;
            break;
          case 2:
            // Shift 2 for Text is the same encoding as C40
            if (cValue < C40_SHIFT2_SET_CHARS.length) {
              char c40char = C40_SHIFT2_SET_CHARS[cValue];
              if (upperShift) {
                result.append((char) (c40char + 128));
                upperShift = false;
              } else {
                result.append(c40char);
              }
            } else if (cValue == 27) {  // FNC1
              result.append((char) 29); // translate as ASCII 29
            } else if (cValue == 30) {  // Upper Shift
              upperShift = true;
            } else {
              throw FormatException.getFormatInstance();
            }
            shift = 0;
            break;
          case 3:
            if (cValue < TEXT_SHIFT3_SET_CHARS.length) {
              char textChar = TEXT_SHIFT3_SET_CHARS[cValue];
              if (upperShift) {
                result.append((char) (textChar + 128));
                upperShift = false;
              } else {
                result.append(textChar);
              }
              shift = 0;
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          default:
            throw FormatException.getFormatInstance();
        }
      }
    } while (bits.available() > 0);
  }

  /**
   * See ISO 16022:2006, 5.2.7
   */
  private static void decodeAnsiX12Segment(BitSource bits,
                                           StringBuilder result) throws FormatException {
    // Three ANSI X12 values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1

    int[] cValues = new int[3];
    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return;
      }
      int firstByte = bits.readBits(8);
      if (firstByte == 254) {  // Unlatch codeword
        return;
      }

      parseTwoBytes(firstByte, bits.readBits(8), cValues);

      for (int i = 0; i < 3; i++) {
        int cValue = cValues[i];
        if (cValue == 0) {  // X12 segment terminator <CR>
          result.append('\r');
        } else if (cValue == 1) {  // X12 segment separator *
          result.append('*');
        } else if (cValue == 2) {  // X12 sub-element separator >
          result.append('>');
        } else if (cValue == 3) {  // space
          result.append(' ');
        } else if (cValue < 14) {  // 0 - 9
          result.append((char) (cValue + 44));
        } else if (cValue < 40) {  // A - Z
          result.append((char) (cValue + 51));
        } else {
          throw FormatException.getFormatInstance();
        }
      }
    } while (bits.available() > 0);
  }

  private static void parseTwoBytes(int firstByte, int secondByte, int[] result) {
    int fullBitValue = (firstByte << 8) + secondByte - 1;
    int temp = fullBitValue / 1600;
    result[0] = temp;
    fullBitValue -= temp * 1600;
    temp = fullBitValue / 40;
    result[1] = temp;
    result[2] = fullBitValue - temp * 40;
  }

  /**
   * See ISO 16022:2006, 5.2.8 and Annex C Table C.3
   */
  private static void decodeEdifactSegment(BitSource bits, StringBuilder result) {
    do {
      // If there is only two or less bytes left then it will be encoded as ASCII
      if (bits.available() <= 16) {
        return;
      }

      for (int i = 0; i < 4; i++) {
        int edifactValue = bits.readBits(6);

        // Check for the unlatch character
        if (edifactValue == 0x1F) {  // 011111
          // Read rest of byte, which should be 0, and stop
          int bitsLeft = 8 - bits.getBitOffset();
          if (bitsLeft != 8) {
            bits.readBits(bitsLeft);
          }
          return;
        }

        if ((edifactValue & 0x20) == 0) {  // no 1 in the leading (6th) bit
          edifactValue |= 0x40;  // Add a leading 01 to the 6 bit binary value
        }
        result.append((char) edifactValue);
      }
    } while (bits.available() > 0);
  }

  /**
   * See ISO 16022:2006, 5.2.9 and Annex B, B.2
   */
  private static void decodeBase256Segment(BitSource bits,
                                           StringBuilder result,
                                           Collection<byte[]> byteSegments)
      throws FormatException {
    // Figure out how long the Base 256 Segment is.
    int codewordPosition = 1 + bits.getByteOffset(); // position is 1-indexed
    int d1 = unrandomize255State(bits.readBits(8), codewordPosition++);
    int count;
    if (d1 == 0) {  // Read the remainder of the symbol
      count = bits.available() / 8;
    } else if (d1 < 250) {
      count = d1;
    } else {
      count = 250 * (d1 - 249) + unrandomize255State(bits.readBits(8), codewordPosition++);
    }

    // We're seeing NegativeArraySizeException errors from users.
    if (count < 0) {
      throw FormatException.getFormatInstance();
    }

    byte[] bytes = new byte[count];
    for (int i = 0; i < count; i++) {
      // Have seen this particular error in the wild, such as at
      // http://www.bcgen.com/demo/IDAutomationStreamingDataMatrix.aspx?MODE=3&D=Fred&PFMT=3&PT=F&X=0.3&O=0&LM=0.2
      if (bits.available() < 8) {
        throw FormatException.getFormatInstance();
      }
      bytes[i] = (byte) unrandomize255State(bits.readBits(8), codewordPosition++);
    }
    byteSegments.add(bytes);
    try {
      result.append(new String(bytes, "ISO8859_1"));
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalStateException("Platform does not support required encoding: " + uee);
    }
  }

  /**
   * See ISO 16022:2006, Annex B, B.2
   */
  private static int unrandomize255State(int randomizedBase256Codeword,
                                          int base256CodewordPosition) {
    int pseudoRandomNumber = ((149 * base256CodewordPosition) % 255) + 1;
    int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
    return tempVariable >= 0 ? tempVariable : tempVariable + 256;
  }

}
