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

#include <zxing/datamatrix/decoder/Decoder.h>
#include <zxing/datamatrix/decoder/BitMatrixParser.h>
#include <zxing/datamatrix/decoder/DataBlock.h>
#include <zxing/datamatrix/decoder/DecodedBitStreamParser.h>
#include <zxing/datamatrix/Version.h>
#include <zxing/ReaderException.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>

namespace zxing {
namespace datamatrix {

using namespace std;

Decoder::Decoder() :
    rsDecoder_(GF256::DATA_MATRIX_FIELD) {
}


void Decoder::correctErrors(ArrayRef<unsigned char> codewordBytes, int numDataCodewords) {
  int numCodewords = codewordBytes->size();
  ArrayRef<int> codewordInts(numCodewords);
  for (int i = 0; i < numCodewords; i++) {
    codewordInts[i] = codewordBytes[i] & 0xff;
  }
  int numECCodewords = numCodewords - numDataCodewords;

  try {
    rsDecoder_.decode(codewordInts, numECCodewords);
  } catch (ReedSolomonException ex) {
    ReaderException rex(ex.what());
    throw rex;
  }

  for (int i = 0; i < numDataCodewords; i++) {
    codewordBytes[i] = (unsigned char)codewordInts[i];
  }
}

Ref<DecoderResult> Decoder::decode(Ref<BitMatrix> bits) {
    // Construct a parser and read version, error-correction level
    BitMatrixParser parser(bits);
    Version *version = parser.readVersion(bits);

    // Read codewords
    ArrayRef<unsigned char> codewords(parser.readCodewords());
    // Separate into data blocks
    std::vector<Ref<DataBlock> > dataBlocks = DataBlock::getDataBlocks(codewords, version);

    // Count total number of data bytes
    int totalBytes = 0;
    for (unsigned int i = 0; i < dataBlocks.size(); i++) {
      totalBytes += dataBlocks[i]->getNumDataCodewords();
    }
    ArrayRef<unsigned char> resultBytes(totalBytes);
    int resultOffset = 0;

    // Error-correct and copy data blocks together into a stream of bytes
    for (unsigned int j = 0; j < dataBlocks.size(); j++) {
      Ref<DataBlock> dataBlock(dataBlocks[j]);
      ArrayRef<unsigned char> codewordBytes = dataBlock->getCodewords();
      int numDataCodewords = dataBlock->getNumDataCodewords();
      correctErrors(codewordBytes, numDataCodewords);
      for (int i = 0; i < numDataCodewords; i++) {
        resultBytes[resultOffset++] = codewordBytes[i];
      }
    }

  // Decode the contents of that stream of bytes
  DecodedBitStreamParser decodedBSParser;
  Ref<String> text(new String(decodedBSParser.decode(resultBytes)));

  Ref<DecoderResult> result(new DecoderResult(resultBytes, text));
  return result;
}
}
}
