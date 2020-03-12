// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  DecodedBitStreamParser.cpp
 *  zxing
 *
 *  Created by Luiz Silva on 09/02/2010.
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include "ZXingDataMatrixDecodedBitStreamParser.h"

#include <zxing/FormatException.h>
#include <zxing/common/DecoderResult.h>


#include "zxing/common/Array.h"
#include "zxing/common/BitSource.h"
#include "zxing/common/Counted.h"
#include "zxing/common/Str.h"

#include <memory>
#include <string>

namespace pping {
namespace datamatrix {

using namespace std;

const int DecodedBitStreamParser::PAD_ENCODE = 0;  // Not really an encoding
const int DecodedBitStreamParser::ASCII_ENCODE = 1;
const int DecodedBitStreamParser::C40_ENCODE = 2;
const int DecodedBitStreamParser::TEXT_ENCODE = 3;
const int DecodedBitStreamParser::ANSIX12_ENCODE = 4;
const int DecodedBitStreamParser::EDIFACT_ENCODE = 5;
const int DecodedBitStreamParser::BASE256_ENCODE = 6;

const char DecodedBitStreamParser::C40_BASIC_SET_CHARS[] = {
    '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
};
  
const char DecodedBitStreamParser::C40_SHIFT2_SET_CHARS[] = {
    '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.',
    '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_'
};
  
const char DecodedBitStreamParser::TEXT_BASIC_SET_CHARS[] = {
    '*', '*', '*', ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
};
  
const char DecodedBitStreamParser::TEXT_SHIFT3_SET_CHARS[] = {
    '\'', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '{', '|', '}', '~', (char) 127
};

FallibleRef<DecoderResult> DecodedBitStreamParser::decode(ArrayRef<unsigned char> bytes) MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<BitSource> bits(new BitSource(bytes));
  mb::stringstreamlite result;
  mb::stringstreamlite resultTrailer;
  vector<unsigned char> byteSegments;
  int mode = ASCII_ENCODE;
  do {
    if (mode == ASCII_ENCODE) {
        auto const tryDecode(decodeAsciiSegment(bits, result, resultTrailer));
        if(!tryDecode)
            return tryDecode.error();

        mode = *tryDecode;
    } else {
      switch (mode) {
        case C40_ENCODE:
        {
          auto const tryDecodeC40(decodeC40Segment(bits, result));
          if(!tryDecodeC40)
              return tryDecodeC40.error();

          break;
        }
        case TEXT_ENCODE:
        {
          auto const tryDecodeText(decodeTextSegment(bits, result));
          if(!tryDecodeText)
              return tryDecodeText.error();

          break;
        }
        case ANSIX12_ENCODE:
        {
          auto const tryDecodeX12(decodeAnsiX12Segment(bits, result));
          if(!tryDecodeX12)
              return tryDecodeX12.error();

          break;
        }
        case EDIFACT_ENCODE:
        {
          auto const tryDecodeEdifact(decodeEdifactSegment(bits, result));
          if(!tryDecodeEdifact)
              return tryDecodeEdifact.error();

          break;
        }
        case BASE256_ENCODE:
        {
          auto const tryDecode256(decodeBase256Segment(bits, result, byteSegments));
          if(!tryDecode256)
              return tryDecode256.error();

          break;
        }
        default:
          return failure<FormatException>("Unsupported mode indicator");
      }
      mode = ASCII_ENCODE;
    }
  } while (mode != PAD_ENCODE && bits->available() > 0);

  if (resultTrailer.str().size() > 0) {
    result << resultTrailer.str();
  }
  ArrayRef<unsigned char> rawBytes(bytes);
  Ref<String> text(new String(result.str()));
  return Ref<DecoderResult>(new DecoderResult(rawBytes, text));
}

Fallible<int> DecodedBitStreamParser::decodeAsciiSegment(Ref<BitSource> bits, mb::stringstreamlite & result,
  mb::stringstreamlite & resultTrailer) MB_NOEXCEPT_EXCEPT_BADALLOC {
  bool upperShift = false;
  do {
      auto const tryReadBits(bits->readBits(8));
      if(!tryReadBits)
          return tryReadBits.error();

    int oneByte = *tryReadBits;
    if (oneByte == 0) {
      return failure<FormatException>("Not enough bits to decode");
    } else if (oneByte <= 128) {  // ASCII data (ASCII value + 1)
      oneByte = upperShift ? (oneByte + 128) : oneByte;
      // upperShift = false;
      result << (char) (oneByte - 1);
      return ASCII_ENCODE;
    } else if (oneByte == 129) {  // Pad
      return PAD_ENCODE;
    } else if (oneByte <= 229) {  // 2-digit data 00-99 (Numeric Value + 130)
      int value = oneByte - 130;
      if (value < 10) { // padd with '0' for single digit values
        result << '0';
      }
      result << value;
    } else if (oneByte == 230) {  // Latch to C40 encodation
      return C40_ENCODE;
    } else if (oneByte == 231) {  // Latch to Base 256 encodation
      return BASE256_ENCODE;
    } else if (oneByte == 232) {  // FNC1
      result << ((char) 29); // translate as ASCII 29
    } else if (oneByte == 233 || oneByte == 234) {
      // Structured Append, Reader Programming
      // Ignore these symbols for now
      // throw FormatException.getInstance();
    } else if (oneByte == 235) {  // Upper Shift (shift to Extended ASCII)
      upperShift = true;
    } else if (oneByte == 236) {  // 05 Macro
        result << ("[)>RS05GS");
        resultTrailer << ("RSEOT");
    } else if (oneByte == 237) {  // 06 Macro
      result << ("[)>RS06GS");
      resultTrailer <<  ("RSEOT");
    } else if (oneByte == 238) {  // Latch to ANSI X12 encodation
      return ANSIX12_ENCODE;
    } else if (oneByte == 239) {  // Latch to Text encodation
      return TEXT_ENCODE;
    } else if (oneByte == 240) {  // Latch to EDIFACT encodation
      return EDIFACT_ENCODE;
    } else if (oneByte == 241) {  // ECI Character
      // TODO(bbrown): I think we need to support ECI
      // throw FormatException.getInstance();
      // Ignore this symbol for now
    } else if (oneByte >= 242) { // Not to be used in ASCII encodation
      // ... but work around encoders that end with 254, latch back to ASCII
      if (oneByte != 254 || bits->available() != 0) {
        return failure<FormatException>("Not to be used in ASCII encodation");
      }
    }
  } while (bits->available() > 0);
  return ASCII_ENCODE;
}

Fallible<void> DecodedBitStreamParser::decodeC40Segment(Ref<BitSource> bits, mb::stringstreamlite & result) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Three C40 values are encoded in a 16-bit value as
  // (1600 * C1) + (40 * C2) + C3 + 1
  // TODO(bbrown): The Upper Shift with C40 doesn't work in the 4 value scenario all the time
  bool upperShift = false;

  auto cValuesUnique = std::make_unique< int[] >( 3 );
  int* cValues = cValuesUnique.get(); // necessary because parseTwoBytes expects int*&

  int shift = 0;
  do {
    // If there is only one byte left then it will be encoded as ASCII
    if (bits->available() == 8) {
      return success();
    }
    auto const tryReadBits(bits->readBits(8));
    if(!tryReadBits)
        return tryReadBits.error();

    int firstByte = *tryReadBits;
    if (firstByte == 254) {  // Unlatch codeword
      return success();
    }

    auto const tryReadBytes(bits->readBits(8));
    if(!tryReadBytes)
        return tryReadBytes.error();

    parseTwoBytes(firstByte, *tryReadBytes, cValues);

    for (int i = 0; i < 3; i++) {
      int cValue = cValues[i];
      switch (shift) {
        case 0:
          if (cValue < 3) {
            shift = cValue + 1;
          } else {
            if (upperShift) {
              result << (char) (C40_BASIC_SET_CHARS[cValue] + 128);
              upperShift = false;
            } else {
              result << C40_BASIC_SET_CHARS[cValue];
            }
          }
          break;
        case 1:
          if (upperShift) {
            result << (char) (cValue + 128);
            upperShift = false;
          } else {
            result << (char) cValue;
          }
          shift = 0;
          break;
        case 2:
          if (cValue < 27) {
            if (upperShift) {
              result << (char) (C40_SHIFT2_SET_CHARS[cValue] + 128);
              upperShift = false;
            } else {
              result << C40_SHIFT2_SET_CHARS[cValue];
            }
          } else if (cValue == 27) {  // FNC1
            result << ((char) 29); // translate as ASCII 29
          } else if (cValue == 30) {  // Upper Shift
            upperShift = true;
          } else {
            return failure<FormatException>("decodeC40Segment: Upper Shift");
          }
          shift = 0;
          break;
        case 3:
          if (upperShift) {
            result << (char) (cValue + 224);
            upperShift = false;
          } else {
            result << (char) (cValue + 96);
          }
          shift = 0;
          break;
        default:
          return failure<FormatException>("decodeC40Segment: no case");
      }
    }
  } while (bits->available() > 0);

  return success();
}

Fallible<void> DecodedBitStreamParser::decodeTextSegment(Ref<BitSource> bits, mb::stringstreamlite & result) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Three Text values are encoded in a 16-bit value as
  // (1600 * C1) + (40 * C2) + C3 + 1
  // TODO(bbrown): The Upper Shift with Text doesn't work in the 4 value scenario all the time
  bool upperShift = false;

  auto cValuesUnique = std::make_unique< int[] >( 3 );
  int* cValues = cValuesUnique.get(); // necessary because parseTwoBytes expects int*&

  int shift = 0;
  do {
    // If there is only one byte left then it will be encoded as ASCII
    if (bits->available() == 8) {
      return success();
    }
    auto const tryReadFirstByte(bits->readBits(8));
    if(!tryReadFirstByte)
        return tryReadFirstByte.error();

    int firstByte = *tryReadFirstByte;
    if (firstByte == 254) {  // Unlatch codeword
      return success();
    }

    auto const tryReadTwoBytes(bits->readBits(8));
    if(!tryReadTwoBytes)
        return tryReadTwoBytes.error();

    parseTwoBytes(firstByte, *tryReadTwoBytes, cValues);

    for (int i = 0; i < 3; i++) {
      int cValue = cValues[i];
      switch (shift) {
        case 0:
          if (cValue < 3) {
            shift = cValue + 1;
          } else {
            if (upperShift) {
              result << (char) (TEXT_BASIC_SET_CHARS[cValue] + 128);
              upperShift = false;
            } else {
              result << (TEXT_BASIC_SET_CHARS[cValue]);
            }
          }
          break;
        case 1:
          if (upperShift) {
            result << (char) (cValue + 128);
            upperShift = false;
          } else {
            result << (char) (cValue);
          }
          shift = 0;
          break;
        case 2:
          // Shift 2 for Text is the same encoding as C40
          if (cValue < 27) {
            if (upperShift) {
              result << (char) (C40_SHIFT2_SET_CHARS[cValue] + 128);
              upperShift = false;
            } else {
              result << (C40_SHIFT2_SET_CHARS[cValue]);
            }
          } else if (cValue == 27) {  // FNC1
            result << ((char) 29); // translate as ASCII 29
          } else if (cValue == 30) {  // Upper Shift
            upperShift = true;
          } else {
            return failure<FormatException>("decodeTextSegment: Upper Shift");
          }
          shift = 0;
          break;
        case 3:
          if (upperShift) {
            result << (char) (TEXT_SHIFT3_SET_CHARS[cValue] + 128);
            upperShift = false;
          } else {
            result << (TEXT_SHIFT3_SET_CHARS[cValue]);
          }
          shift = 0;
          break;
        default:
          return failure<FormatException>("decodeTextSegment: no case");
      }
    }
  } while (bits->available() > 0);

  return success();
}

Fallible<void> DecodedBitStreamParser::decodeAnsiX12Segment(Ref<BitSource> bits, mb::stringstreamlite & result) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Three ANSI X12 values are encoded in a 16-bit value as
  // (1600 * C1) + (40 * C2) + C3 + 1

  auto cValuesUnique = std::make_unique< int[] >( 3 );
  int* cValues = cValuesUnique.get(); // necessary because parseTwoBytes expects int*&

  do {
    // If there is only one byte left then it will be encoded as ASCII
    if (bits->available() == 8) {
      return success();
    }
    auto const tryReadFirstByte(bits->readBits(8));
    if(!tryReadFirstByte)
        return tryReadFirstByte.error();

    int firstByte = *tryReadFirstByte;
    if (firstByte == 254) {  // Unlatch codeword
      return success();
    }
    auto const tryReadTwoBytes(bits->readBits(8));
    if(!tryReadTwoBytes)
        return tryReadTwoBytes.error();

    parseTwoBytes(firstByte, *tryReadTwoBytes, cValues);

    for (int i = 0; i < 3; i++) {
      int cValue = cValues[i];
      if (cValue == 0) {  // X12 segment terminator <CR>
        result << '\r';
      } else if (cValue == 1) {  // X12 segment separator *
        result << '*';
      } else if (cValue == 2) {  // X12 sub-element separator >
        result << '>';
      } else if (cValue == 3) {  // space
        result << ' ';
      } else if (cValue < 14) {  // 0 - 9
        result << (char) (cValue + 44);
      } else if (cValue < 40) {  // A - Z
        result << (char) (cValue + 51);
      } else {
        return failure<FormatException>("decodeAnsiX12Segment: no case");
      }
    }
  } while (bits->available() > 0);

  return success();
}

void DecodedBitStreamParser::parseTwoBytes(int firstByte, int secondByte, int*& result) {
  int fullBitValue = (firstByte << 8) + secondByte - 1;
  int temp = fullBitValue / 1600;
  result[0] = temp;
  fullBitValue -= temp * 1600;
  temp = fullBitValue / 40;
  result[1] = temp;
  result[2] = fullBitValue - temp * 40;
}
  
Fallible<void> DecodedBitStreamParser::decodeEdifactSegment(Ref<BitSource> bits, mb::stringstreamlite & result) MB_NOEXCEPT_EXCEPT_BADALLOC {
  bool unlatch = false;
  do {
    // If there is only two or less bytes left then it will be encoded as ASCII
    if (bits->available() <= 16) {
      return success();
    }

    for (int i = 0; i < 4; i++) {
        auto const tryReadEdifactValue(bits->readBits(6));
        if(!tryReadEdifactValue)
            return tryReadEdifactValue.error();

        int edifactValue = *tryReadEdifactValue;

      // Check for the unlatch character
      if (edifactValue == 0x2B67) {  // 011111
        unlatch = true;
        // If we encounter the unlatch code then continue reading because the Codeword triple
        // is padded with 0's
      }

      if (!unlatch) {
        if ((edifactValue & 0x20) == 0) {  // no 1 in the leading (6th) bit
          edifactValue |= 0x40;  // Add a leading 01 to the 6 bit binary value
        }
        result << (char)(edifactValue);
      }
    }
  } while (!unlatch && bits->available() > 0);

  return success();
}
  
Fallible<void> DecodedBitStreamParser::decodeBase256Segment(Ref<BitSource> bits, mb::stringstreamlite& result, vector<unsigned char> byteSegments) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Figure out how long the Base 256 Segment is.
  int codewordPosition = 1 + bits->getByteOffset(); // position is 1-indexed

  auto const tryReadBits(bits->readBits(8));
  if(!tryReadBits)
      return tryReadBits.error();

  int d1 = unrandomize255State(*tryReadBits, codewordPosition++);
  int count;
  if (d1 == 0) {  // Read the remainder of the symbol
    count = bits->available() / 8;
  } else if (d1 < 250) {
    count = d1;
  } else {
    auto const tryReadByte(bits->readBits(8));
    if(!tryReadByte)
        return tryReadByte.error();

    count = 250 * (d1 - 249) + unrandomize255State(*tryReadByte, codewordPosition++);
  }

  // We're seeing NegativeArraySizeException errors from users.
  if (count < 0) {
    return failure<FormatException>("NegativeArraySizeException");
  }

  auto bytes = std::make_unique< unsigned char[] >(static_cast<size_t>( count ));

  for (int i = 0; i < count; i++) {
    // Have seen this particular error in the wild, such as at
    // http://www.bcgen.com/demo/IDAutomationStreamingDataMatrix.aspx?MODE=3&D=Fred&PFMT=3&PT=F&X=0.3&O=0&LM=0.2
    if (bits->available() < 8) {
      return failure<FormatException>("byteSegments");
    }
    auto const tryReadState(bits->readBits(8));
    if(!tryReadState)
        return tryReadState.error();

    bytes[i] = unrandomize255State(*tryReadState, codewordPosition++);
    byteSegments.push_back(bytes[i]);
    result << (char)bytes[i];
  }
  return success();
}
}
}

