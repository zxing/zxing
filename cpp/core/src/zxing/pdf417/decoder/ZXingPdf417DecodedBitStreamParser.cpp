/*
 *  DecodedBitStreamParser.cpp
 *  zxing
 *
 *  Created by Hartmut Neubauer 2012-05-24 from Java sources.
 *  Copyright 2010,2012 ZXing authors All rights reserved.
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

#include <stddef.h>                        // for size_t, NULL
#include <zxing/FormatException.h>         // for FormatException
#include <zxing/common/DecoderResult.h>    // for DecoderResult, SegmentsMetadata::TEXT_DATA, SegmentsMetadata::BYTE_DATA
#include <zxing/pdf417/decoder/ZXingPdf417DecodedBitStreamParser.h>
#include <cstdint>                         // for int64_t, uint8_t, uint32_t
#include <string>                          // for string
#include <vector>                          // for vector, allocator

#include "zxing/bigint/BigInteger.h"       // for BigInteger
#include "zxing/bigint/BigIntegerUtils.h"  // for bigIntegerToString
#include "zxing/common/Array.h"            // for ArrayRef, Array
#include "zxing/common/Counted.h"          // for Ref
#include "zxing/common/Str.h"              // for String

namespace pping {
namespace pdf417 {

using namespace std;
using namespace bigInteger;

const int DecodedBitStreamParser::TEXT_COMPACTION_MODE_LATCH = 900;
const int DecodedBitStreamParser::BYTE_COMPACTION_MODE_LATCH = 901;
const int DecodedBitStreamParser::NUMERIC_COMPACTION_MODE_LATCH = 902;
const int DecodedBitStreamParser::BYTE_COMPACTION_MODE_LATCH_6 = 924;
const int DecodedBitStreamParser::BEGIN_MACRO_PDF417_CONTROL_BLOCK = 928;
const int DecodedBitStreamParser::BEGIN_MACRO_PDF417_OPTIONAL_FIELD = 923;
const int DecodedBitStreamParser::MACRO_PDF417_TERMINATOR = 922;
const int DecodedBitStreamParser::MODE_SHIFT_TO_BYTE_COMPACTION_MODE = 913;
const int DecodedBitStreamParser::MAX_NUMERIC_CODEWORDS = 15;

const int DecodedBitStreamParser::PL = 25;
const int DecodedBitStreamParser::LL = 27;
const int DecodedBitStreamParser::AS = 27;
const int DecodedBitStreamParser::ML = 28;
const int DecodedBitStreamParser::AL = 28;
const int DecodedBitStreamParser::PS = 29;
const int DecodedBitStreamParser::PAL = 29;

const int DecodedBitStreamParser::EXP900_SIZE = 16;

const char DecodedBitStreamParser::PUNCT_CHARS[] = {
      ';', '<', '>', '@', '[', '\\', ']', '_', '`', '~', '!',
      '\r', '\t', ',', ':', '\n', '-', '.', '$', '/', '"', '|', '*',
      '(', ')', '?', '{', '}', '\''};

const char DecodedBitStreamParser::MIXED_CHARS[] = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '&',
      '\r', '\t', ',', ':', '#', '-', '.', '$', '/', '+', '%', '*',
      '=', '^'};
      
ArrayRef<BigInteger> DecodedBitStreamParser::AExp900_;

  /**
   * Table containing values for the exponent of 900.
   * This is used in the numeric compaction decode algorithm.
   * Hint: will be initialized only once (because of zero check), so it can be
   * called by the constructor.
   */
void DecodedBitStreamParser::InitExp900()
{
  if(AExp900_.array_ == NULL) {
    BigInteger nineHundred(900);
    AExp900_ = new Array<BigInteger>(EXP900_SIZE);
    AExp900_[0] = BigInteger(1);
    for (size_t i=1;i<AExp900_->size();i++) {
      AExp900_[i] = AExp900_[i-1] * nineHundred;
    }
  }
}

/**
* Constructor will initialize exp900 table the first time.
*/
DecodedBitStreamParser::DecodedBitStreamParser()
{
  InitExp900();
}

/**
* PDF417 main decoder.
**/
FallibleRef<DecoderResult> DecodedBitStreamParser::decode(ArrayRef<int> codewords) MB_NOEXCEPT_EXCEPT_BADALLOC
{
  Ref<String> result(new String(""));
  ArrayRef< ArrayRef<unsigned char> > byteSegments((size_t)0);
  auto currMode = pping::SegmentsMetadata::TEXT_DATA;
  auto newMode = pping::SegmentsMetadata::TEXT_DATA;
  // Get compaction mode
  int codeIndex = 1;
  int code = codewords[codeIndex++];
  while (codeIndex < codewords[0]){
      ArrayRef<unsigned char> rawBytes(1);
      rawBytes->values_[0] = static_cast<std::underlying_type<pping::SegmentsMetadata>::type>(currMode);
      while (codeIndex < codewords[0]) {
          switch (code) {
            case TEXT_COMPACTION_MODE_LATCH:
                newMode = pping::SegmentsMetadata::TEXT_DATA;
              break;
            case BYTE_COMPACTION_MODE_LATCH:
                newMode = pping::SegmentsMetadata::BYTE_DATA;
              break;
            case NUMERIC_COMPACTION_MODE_LATCH:
                newMode = pping::SegmentsMetadata::TEXT_DATA;
              break;
            case MODE_SHIFT_TO_BYTE_COMPACTION_MODE:
                newMode = pping::SegmentsMetadata::TEXT_DATA;
              break;
            case BYTE_COMPACTION_MODE_LATCH_6:
                newMode = pping::SegmentsMetadata::BYTE_DATA;
              break;
            default:
              // Default to text compaction. During testing numerous barcodes
              // appeared to be missing the starting mode. In these cases defaulting
              // to text compaction seems to work.
              newMode = pping::SegmentsMetadata::TEXT_DATA;
              break;
            }

            if (currMode != newMode){
                currMode = newMode;
                if (rawBytes->values_.size() != 1){
                    byteSegments->values_.push_back(rawBytes);
                }
                break;
            }

        switch (code) {
        case TEXT_COMPACTION_MODE_LATCH:
          codeIndex = textCompaction(codewords, codeIndex, result, rawBytes);
          break;
        case BYTE_COMPACTION_MODE_LATCH:
          codeIndex = byteCompaction(code, codewords, codeIndex, result, rawBytes);
          break;
        case NUMERIC_COMPACTION_MODE_LATCH:
        {
            auto const tryCompact(numericCompaction(codewords, codeIndex, result, rawBytes));
            if(!tryCompact)
                return tryCompact.error();

            codeIndex = *tryCompact;
            break;
        }
        case MODE_SHIFT_TO_BYTE_COMPACTION_MODE:
          codeIndex = byteCompaction(code, codewords, codeIndex, result, rawBytes);
          break;
        case BYTE_COMPACTION_MODE_LATCH_6:
          codeIndex = byteCompaction(code, codewords, codeIndex, result, rawBytes);
          break;
        default:
          // Default to text compaction. During testing numerous barcodes
          // appeared to be missing the starting mode. In these cases defaulting
          // to text compaction seems to work.
          codeIndex--;
          codeIndex = textCompaction(codewords, codeIndex, result, rawBytes);
          break;
        }
        if (codeIndex < (int)codewords.size()) {
          code = codewords[codeIndex++];
        } else {
          return failure<FormatException>("PDF417:DecodedBitStreamParser:decode: codeword overflow");
        }
      }
      if (codeIndex >= codewords[0]){
          if (rawBytes->values_.size() != 1){
            byteSegments->values_.push_back(rawBytes);
          }
      }
  }
  ArrayRef<unsigned char> dummybuf(1);
  dummybuf[0]= '\0';

  return Ref<DecoderResult>(new DecoderResult(dummybuf, result, byteSegments, std::string("-1")));
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
int DecodedBitStreamParser::textCompaction(ArrayRef<int> codewords, int codeIndex,
                                           Ref<String> result, ArrayRef<unsigned char> rawBytes){
  // 2 character per codeword
  ArrayRef<int> textCompactionData = new Array<int>(codewords[0] << 1);
  // Used to hold the byte compaction value if there is a mode shift
  ArrayRef<int> byteCompactionData = new Array<int>(codewords[0] << 1);
  
  int index = 0;
  bool end = false;
  while ((codeIndex < codewords[0]) && !end) {
    int code = codewords[codeIndex++];
    if (code < TEXT_COMPACTION_MODE_LATCH) {
      textCompactionData[index] = code / 30;
      textCompactionData[index + 1] = code % 30;
      index += 2;
    } else {
      switch (code) {
      case TEXT_COMPACTION_MODE_LATCH:
        codeIndex--;
        end = true;
        break;
      case BYTE_COMPACTION_MODE_LATCH:
        codeIndex--;
        end = true;
        break;
      case NUMERIC_COMPACTION_MODE_LATCH:
        codeIndex--;
        end = true;
        break;
      case MODE_SHIFT_TO_BYTE_COMPACTION_MODE:
        // The Mode Shift codeword 913 shall cause a temporary
        // switch from Text Compaction mode to Byte Compaction mode.
        // This switch shall be in effect for only the next codeword,
        // after which the mode shall revert to the prevailing sub-mode
        // of the Text Compaction mode. Codeword 913 is only available
        // in Text Compaction mode; its use is described in 5.4.2.4.
        textCompactionData[index] = MODE_SHIFT_TO_BYTE_COMPACTION_MODE;
        code = codewords[codeIndex++];
        byteCompactionData[index] = code; //Integer.toHexString(code);
        index++;
        break;
      case BYTE_COMPACTION_MODE_LATCH_6:
        codeIndex--;
        end = true;
        break;
      }
    }
  }
  decodeTextCompaction(textCompactionData, byteCompactionData, index, result, rawBytes);
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
void DecodedBitStreamParser::decodeTextCompaction(ArrayRef<int> textCompactionData,
                                                  ArrayRef<int> byteCompactionData,
                                                  int length,
                                                  Ref<String> result,
                                                  ArrayRef<unsigned char> rawBytes)
{
  // Beginning from an initial state of the Alpha sub-mode
  // The default compaction mode for PDF417 in effect at the start of each symbol shall always be Text
  // Compaction mode Alpha sub-mode (uppercase alphabetic). A latch codeword from another mode to the Text
  // Compaction mode shall always switch to the Text Compaction Alpha sub-mode.
  Mode subMode = ALPHA;
  Mode priorToShiftMode = ALPHA;
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
          result->append((char) byteCompactionData[i]);
          rawBytes->values_.push_back((unsigned char) byteCompactionData[i]);
          // 2012-11-27 hfn after fix by srowen in java code:
          // the pdf417 specs say we have to return to the last latched
          // sub-mode. But I checked different encoder implementations and
          // all of them return to alpha sub-mode after Shift-to-Byte
//		  subMode = ALPHA;
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
        } else if (subModeCh == AS) {
          // Shift to alpha
          priorToShiftMode = subMode;
          subMode = ALPHA_SHIFT;
        } else if (subModeCh == ML) {
          subMode = MIXED;
        } else if (subModeCh == PS) {
          // Shift to punctuation
          priorToShiftMode = subMode;
          subMode = PUNCT_SHIFT;
        } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
          result->append((char) byteCompactionData[i]);
          rawBytes->values_.push_back((unsigned char) byteCompactionData[i]);
          // 2012-11-27 hfn after fix by srowen in java code:
          // the pdf417 specs say we have to return to the last latched
          // sub-mode. But I checked different encoder implementations and
          // all of them return to alpha sub-mode after Shift-to-Byte
//		  subMode = ALPHA;
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
        } else if (subModeCh == LL) {
          subMode = LOWER;
        } else if (subModeCh == AL) {
          subMode = ALPHA;
        } else if (subModeCh == PS) {
          // Shift to punctuation
          priorToShiftMode = subMode;
          subMode = PUNCT_SHIFT;
        } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
          result->append((char) byteCompactionData[i]);
          rawBytes->values_.push_back((unsigned char) byteCompactionData[i]);
          // 2012-11-27 hfn after fix by srowen in java code:
          // the pdf417 specs say we have to return to the last latched
          // sub-mode. But I checked different encoder implementations and
          // all of them return to alpha sub-mode after Shift-to-Byte
//		  subMode = ALPHA;
        }
      }
      break;
      
    case PUNCT:
      // Punctuation
      if (subModeCh < PAL) {
        ch = PUNCT_CHARS[subModeCh];
      } else {
        if (subModeCh == PAL) {
          subMode = ALPHA;
        } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
          result->append((char) byteCompactionData[i]);
          rawBytes->values_.push_back((unsigned char) byteCompactionData[i]);
          // 2012-11-27 hfn after fix by srowen in java code:
          // the pdf417 specs say we have to return to the last latched
          // sub-mode. But I checked different encoder implementations and
          // all of them return to alpha sub-mode after Shift-to-Byte
//		  subMode = ALPHA;
        }
      }
      break;
      
    case ALPHA_SHIFT:
      // Restore sub-mode
      subMode = priorToShiftMode;
      if (subModeCh < 26) {
        ch = (char) ('A' + subModeCh);
      } else {
        if (subModeCh == 26) {
          ch = ' ';
        } else {
          // is this even possible?
        }
      }
      break;
      
    case PUNCT_SHIFT:
      // Restore sub-mode
      subMode = priorToShiftMode;
      if (subModeCh < PAL) {
        ch = PUNCT_CHARS[subModeCh];
      } else {
        if (subModeCh == PAL) {
          subMode = ALPHA;
        // 2012-11-27 added from recent java code:
        } else if (subModeCh == MODE_SHIFT_TO_BYTE_COMPACTION_MODE) {
          // PS before Shift-to-Byte is used as a padding character, 
          // see 5.4.2.4 of the specification
          result->append((char) byteCompactionData[i]);
          rawBytes->values_.push_back((unsigned char) byteCompactionData[i]);
          // 2012-11-27 hfn after fix by srowen in java code:
          // the pdf417 specs say we have to return to the last latched
          // sub-mode. But I checked different encoder implementations and
          // all of them return to alpha sub-mode after Shift-to-Byte
//		  subMode = ALPHA;
        } else if (subModeCh == TEXT_COMPACTION_MODE_LATCH) {
          subMode = ALPHA;
        }
      }
      break;
      }
      if (ch != 0) {
        // Append decoded character to result
        result->append(ch);
        rawBytes->values_.push_back((unsigned char) ch);
      }
      i++;
    }
}

bool DecodedBitStreamParser::testCompactionModeChange(int code){
    if (code == TEXT_COMPACTION_MODE_LATCH ||
            code == BYTE_COMPACTION_MODE_LATCH ||
            code == NUMERIC_COMPACTION_MODE_LATCH ||
            code == BYTE_COMPACTION_MODE_LATCH_6 ||
            code == BEGIN_MACRO_PDF417_CONTROL_BLOCK ||
            code == BEGIN_MACRO_PDF417_OPTIONAL_FIELD ||
            code == MACRO_PDF417_TERMINATOR) {
        return true;
    }else{
        return false;
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
int DecodedBitStreamParser::byteCompaction(int mode, ArrayRef<int> codewords, int codeIndex,
                                           Ref<String> result, ArrayRef<unsigned char> rawBytes)
{
  if (mode == MODE_SHIFT_TO_BYTE_COMPACTION_MODE){
      result->append((char)codewords[codeIndex]);
      rawBytes->values_.push_back((unsigned char)codewords[codeIndex]);
      codeIndex ++;
  }else if (mode == BYTE_COMPACTION_MODE_LATCH) {
    // Total number of Byte Compaction characters to be encoded
    // is not a multiple of 6
    int count = 0;
    int64_t value = 0;
    ArrayRef<char> decodedData = new Array<char>(6);
    ArrayRef<int> byteCompactedCodewords = new Array<int>(6);
    bool end = false;
    while ((codeIndex < codewords[0]) && !end) {
      int code = codewords[codeIndex++];
      if (code < TEXT_COMPACTION_MODE_LATCH) {
        byteCompactedCodewords[count] = code;
        count++;
        // Base 900
        value = 900 * value + code;
      } else {
        if (testCompactionModeChange(code) == true) {
          codeIndex--;
          end = true;
        }
      }
      if ((count % 5 == 0) && (count > 0)) {
        /*
         * Added to original zxing code
         */
        int nextCodeword = codewords[codeIndex];
        if (testCompactionModeChange(nextCodeword) == true || codeIndex == codewords[0]){
            break;
        }
        /*
         * End
         */
        // Decode every 5 codewords
        // Convert to Base 256
        for (int j = 0; j < 6; ++j) {
          decodedData[5 - j] = (char) (value % 256);
          value >>= 8;
        }
        result->append(std::string(&decodedData[0],6));
        for (int j = 0; j < 6; ++j){
            rawBytes->values_.push_back((unsigned char)decodedData->values_[j]);
        }
        count = 0;
      }
    }
    // If Byte Compaction mode is invoked with codeword 901,
    // the final group of codewords is interpreted directly
    // as one byte per codeword, without compaction.
    for (int j = 0; j < count; ++j){
        result->append((char) byteCompactedCodewords[j]);
        rawBytes->values_.push_back((unsigned char) byteCompactedCodewords[j]);
    }
    /*
     * Changed
     *
     *	for (i = (count / 5) * 5; i < count; i++) {
     *	  result->append((char) byteCompactedCodewords[i]);
     *	}
     *
     */
    
  } else if (mode == BYTE_COMPACTION_MODE_LATCH_6) {
    // Total number of Byte Compaction characters to be encoded
    // is an integer multiple of 6
    int count = 0;
    int64_t value = 0;
    bool end = false;
    while (codeIndex < codewords[0] && !end) {
      int code = codewords[codeIndex++];
      if (code < TEXT_COMPACTION_MODE_LATCH) {
        count++;
        // Base 900
        value = 900 * value + code;
      } else {
        if (testCompactionModeChange(code) == true) {
          codeIndex--;
          end = true;
        }
      }
      if ((count % 5 == 0) && (count > 0)) {
        // Decode every 5 codewords
        // Convert to Base 256
        ArrayRef<char> decodedData = new Array<char>(6);
        for (int j = 0; j < 6; ++j) {
          decodedData[5 - j] = (char) (value & 0xFF);
          value >>= 8;
        }
        result->append(std::string(&decodedData[0],6));
        for (int j = 0; j < 6; ++j){
            rawBytes->values_.push_back((unsigned char)decodedData->values_[j]);
        }
        // 2012-11-27 hfn after recent java code/fix by srowen
        count = 0;
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
Fallible<int> DecodedBitStreamParser::numericCompaction(ArrayRef<int> codewords, int codeIndex,
                                              Ref<String> result, ArrayRef<unsigned char> rawBytes) MB_NOEXCEPT_EXCEPT_BADALLOC
{
  int count = 0;
  bool end = false;
  
  ArrayRef<int> numericCodewords = new Array<int>(MAX_NUMERIC_CODEWORDS);
  
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
      if (count > 0){
          auto const tryDecode(decodeBase900toBase10(numericCodewords, count));
          if(!tryDecode)
              return tryDecode.error();

          Ref<String> s = *tryDecode;
          std::string numericResult = s->getText();
          result->append(numericResult);
          for (uint32_t i = 0; i < numericResult.size(); ++i){
              rawBytes->values_.push_back((unsigned char) numericResult[i]);
          }
      }
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
*/
FallibleRef<String> DecodedBitStreamParser::decodeBase900toBase10(ArrayRef<int> codewords, int count) MB_NOEXCEPT_EXCEPT_BADALLOC
{
  InitExp900();
  BigInteger result = BigInteger(0);
  for (int i = 0; i < count; i++) {
    result = result + (AExp900_[count - i - 1] * BigInteger(codewords[i]));
  }
  std::string resultString = bigIntegerToString(result);
  if (resultString[0] != '1') {
    return failure<FormatException>("DecodedBitStreamParser::decodeBase900toBase10: String does not begin with 1");
  }
  std::string resultString2;
  resultString2.assign(resultString.begin()+1,resultString.end());
  Ref<String> res (new String(resultString2));
  return res;
}

}
}
