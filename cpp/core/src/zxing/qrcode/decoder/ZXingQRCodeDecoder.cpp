/*
 *  Decoder.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 20/05/2008.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <stddef.h>                                                  // for size_t
#include <zxing/ReaderException.h>                                   // for ReaderException
#include <zxing/common/reedsolomon/ReedSolomonException.h>           // for ReedSolomonException
#include <zxing/qrcode/decoder/ZXingQRCodeBitMatrixParser.h>         // for BitMatrixParser
#include <zxing/qrcode/decoder/ZXingQRCodeDataBlock.h>               // for DataBlock
#include <zxing/qrcode/decoder/ZXingQRCodeDecodedBitStreamParser.h>  // for DecodedBitStreamParser, DecodedBitStreamParser::Hashtable
#include <zxing/qrcode/decoder/ZXingQRCodeDecoder.h>
#include <vector>                                                    // for vector, allocator

#include "zxing/common/Array.h"                                      // for ArrayRef, Array
#include "zxing/common/BitMatrix.h"                                  // for BitMatrix
#include "zxing/common/Counted.h"                                    // for Ref
#include "zxing/common/DecoderResult.h"                              // for DecoderResult
#include "zxing/common/reedsolomon/GenericGF.h"                      // for GenericGF, GenericGF::QR_CODE_FIELD_256
#include "zxing/common/reedsolomon/ReedSolomonDecoder.h"             // for ReedSolomonDecoder
#include "zxing/qrcode/FormatInformation.h"                          // for FormatInformation

namespace pping {
namespace qrcode {

class ErrorCorrectionLevel;
class Version;

using namespace std;

Decoder::Decoder() noexcept:
    rsDecoder_(GenericGF::QR_CODE_FIELD_256) {
}

Fallible<void> Decoder::correctErrors(ArrayRef<unsigned char> codewordBytes, int numDataCodewords) MB_NOEXCEPT_EXCEPT_BADALLOC {
  int numCodewords = (int)codewordBytes->size();
  ArrayRef<int> codewordInts(numCodewords);
  for (int i = 0; i < numCodewords; i++) {
    codewordInts[i] = codewordBytes[i] & 0xff;
  }
  int numECCodewords = numCodewords - numDataCodewords;

  const auto decoderResult(rsDecoder_.decode(codewordInts, numECCodewords));
  if(!decoderResult) {
      return decoderResult.error();
  }

  for (int i = 0; i < numDataCodewords; i++) {
    codewordBytes[i] = (unsigned char)codewordInts[i];
  }

  return success();
}

FallibleRef<DecoderResult> Decoder::decodeWithParser(Ref<BitMatrixParser> parser)
{
    auto const version(parser->readVersion());
    if (!version)
        return version.error();

    auto const getFormatInfo(parser->readFormatInformation());
    if(!getFormatInfo)
        return getFormatInfo.error();

    ErrorCorrectionLevel &ecLevel = (*getFormatInfo)->getErrorCorrectionLevel();

    // Read codewords
    auto const codewords(parser->readCodewords());
    if (!codewords)
        return codewords.error();

    // Separate into data blocks
    auto const dataBlocks(DataBlock::getDataBlocks(*codewords, *version, ecLevel));
    if(!dataBlocks)
        return dataBlocks.error();

    // Count total number of data bytes
  int totalBytes = 0;
  for (size_t i = 0; i < (*dataBlocks).size(); i++) {
    totalBytes += (*dataBlocks)[i]->getNumDataCodewords();
    }
    ArrayRef<unsigned char> resultBytes(totalBytes);
    int resultOffset = 0;


    // Error-correct and copy data blocks together into a stream of bytes
  for (size_t j = 0; j < (*dataBlocks).size(); j++) {
    Ref<DataBlock> dataBlock((*dataBlocks)[j]);
      ArrayRef<unsigned char> codewordBytes = dataBlock->getCodewords();
      int numDataCodewords = dataBlock->getNumDataCodewords();
      auto const success(correctErrors(codewordBytes, numDataCodewords));
      if (!success)
          return success.error();

      for (int i = 0; i < numDataCodewords; i++) {
        resultBytes[resultOffset++] = codewordBytes[i];
      }
    }

    return DecodedBitStreamParser::decode(resultBytes,
                                          *version,
                                          ecLevel,
                                          DecodedBitStreamParser::Hashtable());
}

FallibleRef<DecoderResult> Decoder::decode(Ref<BitMatrix> bits) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Construct a parser and read version, error-correction level
  auto const tryCreateParser(BitMatrixParser::createBitMatrixParser(bits));

  if(!tryCreateParser)
      return tryCreateParser.error();

  auto const parser(*tryCreateParser);

  auto const tryDecode(decodeWithParser(parser));
  if(tryDecode)
      return *tryDecode;

  auto const tryRemask(parser->remask());
  if(!tryRemask)
      return tryRemask.error();

  parser->mirror();

  return decodeWithParser(parser);

}

} // namespace qrcode

} // namespace pping
