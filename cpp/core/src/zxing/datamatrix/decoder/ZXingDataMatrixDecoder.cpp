/*
 *  Decoder.cpp
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

#include <zxing/ReaderException.h>                                           // for ReaderException
#include <zxing/common/reedsolomon/ReedSolomonException.h>                   // for ReedSolomonException
#include <zxing/datamatrix/decoder/ZXingDataMatrixBitMatrixParser.h>         // for BitMatrixParser
#include <zxing/datamatrix/decoder/ZXingDataMatrixDataBlock.h>               // for DataBlock
#include <zxing/datamatrix/decoder/ZXingDataMatrixDecodedBitStreamParser.h>  // for DecodedBitStreamParser
#include <zxing/datamatrix/decoder/ZXingDataMatrixDecoder.h>
#include <vector>                                                            // for vector, allocator

#include "zxing/common/Array.h"                                              // for ArrayRef, Array
#include "zxing/common/BitMatrix.h"                                          // for BitMatrix
#include "zxing/common/Counted.h"                                            // for Ref
#include "zxing/common/DecoderResult.h"                                      // for DecoderResult
#include "zxing/common/reedsolomon/GenericGF.h"                              // for GenericGF, GenericGF::DATA_MATRIX_FIELD_256
#include "zxing/common/reedsolomon/ReedSolomonDecoder.h"                     // for ReedSolomonDecoder

namespace pping {
namespace datamatrix {

class Version;

using namespace std;

Decoder::Decoder() :
    rsDecoder_(GenericGF::DATA_MATRIX_FIELD_256) {
}


Fallible<void> Decoder::correctErrors(ArrayRef<unsigned char> codewordBytes, int numDataCodewords) {
  int numCodewords = (int)codewordBytes->size();
  ArrayRef<int> codewordInts(numCodewords);
  for (int i = 0; i < numCodewords; i++) {
    codewordInts[i] = codewordBytes[i] & 0xff;
  }
  int numECCodewords = numCodewords - numDataCodewords;

  auto const decodingResult = rsDecoder_.decode(codewordInts, numECCodewords);
  if(!decodingResult)
      return decodingResult.error();

  // Copy back into array of bytes -- only need to worry about the bytes that were data
  // We don't care about errors in the error-correction codewords
  for (int i = 0; i < numDataCodewords; i++) {
    codewordBytes[i] = (unsigned char)codewordInts[i];
  }
  return success();
}

FallibleRef<DecoderResult> Decoder::decode(Ref<BitMatrix> bits) MB_NOEXCEPT_EXCEPT_BADALLOC {
  // Construct a parser and read version, error-correction level
  auto const createParser(BitMatrixParser::createBitMatrixParser(bits));
  if(!createParser)
      return createParser.error();

  auto const parser = *createParser;

  auto const tryReadVersion(parser->readVersion(bits));
  if(!tryReadVersion)
      return tryReadVersion.error();

  Version *version = *tryReadVersion;

  // Read codewords
  auto const tryReadCodewords(parser->readCodewords());
  if(!tryReadCodewords)
      return tryReadCodewords.error();

  ArrayRef<unsigned char> codewords(*tryReadCodewords);

  // Separate into data blocks
  auto const getDataBlocks(DataBlock::getDataBlocks(codewords, version));
  if(!getDataBlocks)
      return getDataBlocks.error();

  std::vector<Ref<DataBlock> > dataBlocks = *getDataBlocks;

  int dataBlocksCount = (int)dataBlocks.size();

  // Count total number of data bytes
  int totalBytes = 0;
  for (int i = 0; i < dataBlocksCount; i++) {
    totalBytes += dataBlocks[i]->getNumDataCodewords();
  }
  ArrayRef<unsigned char> resultBytes(totalBytes);

  // Error-correct and copy data blocks together into a stream of bytes
  for (int j = 0; j < dataBlocksCount; j++) {
    Ref<DataBlock> dataBlock(dataBlocks[j]);
    ArrayRef<unsigned char> codewordBytes = dataBlock->getCodewords();
    int numDataCodewords = dataBlock->getNumDataCodewords();

    auto const errorCorrection = correctErrors(codewordBytes, numDataCodewords);
    if(!errorCorrection)
        return errorCorrection.error();

    for (int i = 0; i < numDataCodewords; i++) {
      // De-interlace data blocks.
      resultBytes[i * dataBlocksCount + j] = codewordBytes[i];
    }
  }
  // Decode the contents of that stream of bytes
  DecodedBitStreamParser decodedBSParser;
  return decodedBSParser.decode(resultBytes);
}
}
}
