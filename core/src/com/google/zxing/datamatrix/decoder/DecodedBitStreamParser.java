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

import com.google.zxing.ReaderException;
import com.google.zxing.common.BitSource;

/**
 * <p>Data Matrix Codes can encode text as bits in one of several modes, and can use multiple modes
 * in one Data Matrix Code. This class decodes the bits back into text.</p>
 *
 * <p>See ISO 16022:2006, 5.2.1 - 5.2.9.2</p>
 *
 * @author bbrown@google.com (Brian Brown)
 */
final class DecodedBitStreamParser {

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
    '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.',
    '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'
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
    '\'', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', 127
  };
  
  private static final int PAD_ENCODE = 0;  // Not really an encoding
  private static final int ASCII_ENCODE = 1;
  private static final int C40_ENCODE = 2;
  private static final int TEXT_ENCODE = 3;
  private static final int ANSIX12_ENCODE = 4;
  private static final int EDIFACT_ENCODE = 5;
  private static final int BASE256_ENCODE = 6;

  private DecodedBitStreamParser() {
  }

  static String decode(byte[] bytes) throws ReaderException {
    BitSource bits = new BitSource(bytes);
    StringBuffer result = new StringBuffer();
    
    int mode = ASCII_ENCODE;
    do {
      if (mode != PAD_ENCODE) {
        if (mode == ASCII_ENCODE) {
          mode = decodeAsciiSegment(bits, result);
        } else if (mode == C40_ENCODE) {
          mode = decodeC40Segment(bits, result);
        } else if (mode == TEXT_ENCODE) {
          mode = decodeTextSegment(bits, result);
        } else if (mode == ANSIX12_ENCODE) {
          mode = decodeAnsiX12Segment(bits, result);
        } else if (mode == EDIFACT_ENCODE) {
          mode = decodeEdifactSegment(bits, result);
        } else if (mode == BASE256_ENCODE) {
          mode = decodeBase256Segment(bits, result);
        } else {
          throw new ReaderException("Unsupported mode indicator");
        }
      }
    } while (mode != PAD_ENCODE && bits.available() > 0);

    return result.toString();
  }
  
  /**
  * See ISO 16022:2006, 5.2.3 and Annex C, Table C.2
  */
  private static int decodeAsciiSegment(BitSource bits,
                                        StringBuffer result) throws ReaderException {
    boolean upperShift = false;
    do {
      char oneByte = (char) bits.readBits(8);
      if (oneByte == '\0') {
	    	// TODO(bbrown): I think this would be a bug, not sure
	    	throw new ReaderException("0 is an invalid ASCII codeword");
	    } else if (oneByte <= 128) {  // ASCII data (ASCII value + 1)
	    	oneByte = upperShift ? (char) (oneByte + 128) : oneByte;
	    	upperShift = false;
	    	result.append((char)(oneByte - 1));
	    	return ASCII_ENCODE;
	    } else if (oneByte == 129) {  // Pad
	    	return PAD_ENCODE;
	    } else if (oneByte <= 229) {  // 2-digit data 00-99 (Numeric Value + 130)
	      // TODO(bbrown): Iassume there is some easier way to do this:
	      if (oneByte - 130 < 10) {
	        result.append('0');
	      }
	    	result.append(Integer.toString(oneByte - 130));
	    } else if (oneByte == 230) {  // Latch to C40 encodation
	    	return C40_ENCODE;
	    } else if (oneByte == 231) {  // Latch to Base 256 encodation
	    	return BASE256_ENCODE;
	    } else if (oneByte == 232) {  // FNC1
	    	throw new ReaderException("Currently not supporting FNC1");
	    } else if (oneByte == 233) {  // Structured Append
	    	throw new ReaderException("Currently not supporting Structured Append");
	    } else if (oneByte == 234) {  // Reader Programming
	    	throw new ReaderException("Currently not supporting Reader Programming");
	    } else if (oneByte == 235) {  // Upper Shift (shift to Extended ASCII)
	    	upperShift = true;
	    } else if (oneByte == 236) {  // 05 Macro
	    	throw new ReaderException("Currently not supporting 05 Macro");
	    } else if (oneByte == 237) {  // 06 Macro
	    	throw new ReaderException("Currently not supporting 06 Macro");
	    } else if (oneByte == 238) {  // Latch to ANSI X12 encodation
	    	return ANSIX12_ENCODE;
	    } else if (oneByte == 239) {  // Latch to Text encodation
	    	return TEXT_ENCODE;
	    } else if (oneByte == 240) {  // Latch to EDIFACT encodation
	    	return EDIFACT_ENCODE;
	    } else if (oneByte == 241) {  // ECI Character
	    	// TODO(bbrown): I think we need to support ECI
	    	throw new ReaderException("Currently not supporting ECI Character");
	    } else if (oneByte >= 242) {  // Not to be used in ASCII encodation
	    	throw new ReaderException(Integer.toString(oneByte) + " should not be used in ASCII encodation");
	    }
    } while (bits.available() > 0);
    return ASCII_ENCODE;
  }

  /**
  * See ISO 16022:2006, 5.2.5 and Annex C, Table C.1
  */
  private static int decodeC40Segment(BitSource bits,
                                      StringBuffer result) throws ReaderException {
    // Three C40 values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1
    int shift = 0;
    // TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
    boolean upperShift = false;

    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return ASCII_ENCODE;
      }

      char firstByte = (char) bits.readBits(8);

      if (firstByte == 254) {  // Unlatch codeword
        return ASCII_ENCODE;
      }

      int fullBitValue = (firstByte << 8) + bits.readBits(8) - 1;

      char[] cValues = new char[3];
      cValues[0] = (char) (fullBitValue / 1600);
      fullBitValue -= cValues[0] * 1600;
      cValues[1] = (char) (fullBitValue / 40);
      fullBitValue -= cValues[1] * 40;
      cValues[2] = (char) fullBitValue;

      for (int i = 0; i < 3; i++) {
        if (shift == 0) {
          if (cValues[i] == 0) {  // Shift 1
            shift = 1;
            continue;
          } else if (cValues[i] == 1) {  // Shift 2
            shift = 2;
            continue;
          } else if (cValues[i] == 2) {  // Shift 3
            shift = 3;
            continue;
          }
          if (upperShift) {
            result.append((char)(C40_BASIC_SET_CHARS[cValues[i]] + 128));
            upperShift = false;
          } else {
            result.append(C40_BASIC_SET_CHARS[cValues[i]]);
          }
        } else if (shift == 1) {
          if (upperShift) {
            result.append((char) (cValues[i] + 128));
            upperShift = false;
          } else {
            result.append(cValues[i]);
          }
        } else if (shift == 2) {
          if (cValues[i] < 27) {
            if(upperShift) {
              result.append((char)(C40_SHIFT2_SET_CHARS[cValues[i]] + 128));
              upperShift = false;
            } else {
              result.append(C40_SHIFT2_SET_CHARS[cValues[i]]);
            }
          } else if (cValues[i] == 27) {  // FNC1
            throw new ReaderException("Currently not supporting FNC1");
          } else if (cValues[i] == 30) {  // Upper Shirt
            upperShift = true;
          } else {
            throw new ReaderException(Integer.toString(cValues[i]) + " is not valid in the C40 Shift 2 set");
          }
        } else if (shift == 3) {
          if (upperShift) {
            result.append((char) (cValues[i] + 224));
            upperShift = false;
          } else {
            result.append((char) cValues[i] + 96);
          }
        } else {
          throw new ReaderException("Invalid shift value");
        }
      }
    } while (bits.available() > 0);
    return ASCII_ENCODE;
  }
  
  /**
  * See ISO 16022:2006, 5.2.6 and Annex C, Table C.2
  */
  private static int decodeTextSegment(BitSource bits,
                                       StringBuffer result) throws ReaderException {
    // Three Text values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1
    int shift = 0;
    // TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
    boolean upperShift = false;

    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return ASCII_ENCODE;
      }

      char firstByte = (char) bits.readBits(8);

      if (firstByte == 254) {  // Unlatch codeword
        return ASCII_ENCODE;
      }

      int fullBitValue = (firstByte << 8) + bits.readBits(8) - 1;

      char[] cValues = new char[3];
      cValues[0] = (char) (fullBitValue / 1600);
      fullBitValue -= cValues[0] * 1600;
      cValues[1] = (char) (fullBitValue / 40);
      fullBitValue -= cValues[1] * 40;
      cValues[2] = (char) fullBitValue;

      for (int i = 0; i < 3; i++) {
        if (shift == 0) {
          if (cValues[i] == 0) {  // Shift 1
            shift = 1;
            continue;
          } else if (cValues[i] == 1) {  // Shift 2
            shift = 2;
            continue;
          } else if (cValues[i] == 2) {  // Shift 3
            shift = 3;
            continue;
          }
          if (upperShift) {
            result.append((char)(TEXT_BASIC_SET_CHARS[cValues[i]] + 128));
            upperShift = false;
          } else {
            result.append(TEXT_BASIC_SET_CHARS[cValues[i]]);
          }
        } else if (shift == 1) {
          if (upperShift) {
            result.append((char) (cValues[i] + 128));
            upperShift = false;
          } else {
            result.append((char) cValues[i]);
          }
        } else if (shift == 2) {
          // Shift 2 for Text is the same encoding as C40
          if (cValues[i] < 27) {
            if(upperShift) {
              result.append((char)(C40_SHIFT2_SET_CHARS[cValues[i]] + 128));
              upperShift = false;
            } else {
              result.append(C40_SHIFT2_SET_CHARS[cValues[i]]);
            }
          } else if (cValues[i] == 27) {  // FNC1
            throw new ReaderException("Currently not supporting FNC1");
          } else if (cValues[i] == 30) {  // Upper Shirt
            upperShift = true;
          } else {
            throw new ReaderException(Integer.toString(cValues[i]) + " is not valid in the C40 Shift 2 set");
          }
        } else if (shift == 3) {
          if (upperShift) {
            result.append((char)(TEXT_SHIFT3_SET_CHARS[cValues[i]] + 128));
            upperShift = false;
          } else {
            result.append(TEXT_SHIFT3_SET_CHARS[cValues[i]]);
          }
        } else {
          throw new ReaderException("Invalid shift value");
        }
      }
    } while (bits.available() > 0);
    return ASCII_ENCODE;
  }
  
  /**
  * See ISO 16022:2006, 5.2.7
  */
  private static int decodeAnsiX12Segment(BitSource bits,
                                          StringBuffer result) throws ReaderException {
    // Three ANSI X12 values are encoded in a 16-bit value as
    // (1600 * C1) + (40 * C2) + C3 + 1

    do {
      // If there is only one byte left then it will be encoded as ASCII
      if (bits.available() == 8) {
        return ASCII_ENCODE;
      }

      char firstByte = (char) bits.readBits(8);

      if (firstByte == 254) {  // Unlatch codeword
        return ASCII_ENCODE;
      }

      int fullBitValue = (firstByte << 8) + bits.readBits(8) - 1;

      char[] cValues = new char[3];
      cValues[0] = (char) (fullBitValue / 1600);
      fullBitValue -= cValues[0] * 1600;
      cValues[1] = (char) (fullBitValue / 40);
      fullBitValue -= cValues[1] * 40;
      cValues[2] = (char) fullBitValue;

      for (int i = 0; i < 3; i++) {
        // TODO(bbrown): These really aren't X12 symbols, we are converting to ASCII chars
        if (cValues[i] == 0) {  // X12 segment terminator <CR>
          result.append("<CR>");
        } else if (cValues[i] == 1) {  // X12 segment separator *
          result.append('*');
        } else if (cValues[i] == 2) {  // X12 sub-element separator >
          result.append('>');
        } else if (cValues[i] == 3) {  // space
          result.append(' ');
        } else if (cValues[i] < 14) {  // 0 - 9
          result.append((char) (cValues[i] + 44));
        } else if (cValues[i] < 40) {  // A - Z
          result.append((char) (cValues[i] + 51));
        } else {
          throw new ReaderException(Integer.toString(cValues[i]) + " is not valid in the ANSI X12 set");
        }
      }
    } while (bits.available() > 0);
    
    return ASCII_ENCODE;
  }
  
  /**
  * See ISO 16022:2006, 5.2.8 and Annex C Table C.3
  */
  private static int decodeEdifactSegment(BitSource bits, StringBuffer result) {
    boolean unlatch = false;
    do {
      // If there is only two or less bytes left then it will be encoded as ASCII
      if (bits.available() <= 16) {
        return ASCII_ENCODE;
      }

      for (int i = 0; i < 4; i++) {
        char edifactValue = (char) bits.readBits(6);

        // Check for the unlatch character
        if (edifactValue == 0x2B67) {  // 011111
          unlatch = true;
          // If we encounter the unlatch code then continue reading because the Codeword triple
          // is padded with 0's
        }
        
        if (!unlatch) {
          if ((edifactValue & 32) == 0) {  // no 1 in the leading (6th) bit
            edifactValue |= 64;  // Add a leading 01 to the 6 bit binary value
          }
          result.append((char) edifactValue);
        }
      }
    } while (!unlatch && bits.available() > 0);

    return ASCII_ENCODE;
  }
  
  /**
  * See ISO 16022:2006, 5.2.9 and Annex B, B.2
  */
  private static int decodeBase256Segment(BitSource bits, StringBuffer result) {
    // Figure out how long the Base 256 Segment is.
    char d1 = (char) bits.readBits(8);
    int count;
    if (d1 == 0) {  // Read the remainder of the symbol
      count = bits.available() / 8;
    } else if (d1 < 250) {
      count = d1;
    } else {
      count = 250 * (d1 - 249) + bits.readBits(8);
    }
    char[] readBytes = new char[count];
    for (int i = 0; i < count; i++) {
      result.append(unrandomize255State((char) bits.readBits(8), count));
    }
    
    return ASCII_ENCODE;
  }
  
  /**
  * See ISO 16022:2006, Annex B, B.2
  */
  private static char unrandomize255State(char randomizedBase256Codeword,
                                          int base256CodewordPosition) {
    char pseudoRandomNumber = (char) (((149 * base256CodewordPosition) % 255) + 1);
    int tempVariable = randomizedBase256Codeword - pseudoRandomNumber;
    if (tempVariable >= 0) {
      return (char) tempVariable;
    } else {
      return (char) (tempVariable + 256);
    }
  }
  
}
