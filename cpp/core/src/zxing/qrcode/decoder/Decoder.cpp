// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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

#include <zxing/qrcode/decoder/Decoder.h>
#include <zxing/qrcode/decoder/BitMatrixParser.h>
#include <zxing/qrcode/ErrorCorrectionLevel.h>
#include <zxing/qrcode/Version.h>
#include <zxing/qrcode/decoder/DataBlock.h>
#include <zxing/qrcode/decoder/DecodedBitStreamParser.h>
#include <zxing/ReaderException.h>
#include <zxing/ChecksumException.h>
#include <zxing/common/reedsolomon/ReedSolomonException.h>

using zxing::qrcode::Decoder;
using zxing::DecoderResult;
using zxing::Ref;

// VC++
using zxing::ArrayRef;
using zxing::BitMatrix;

Decoder::Decoder() :
  rsDecoder_(GenericGF::QR_CODE_FIELD_256) {
}

void Decoder::correctErrors(ArrayRef<char> codewordBytes, int numDataCodewords) {
  int numCodewords = codewordBytes->size();
  ArrayRef<int> codewordInts(numCodewords);
  for (int i = 0; i < numCodewords; i++) {
    codewordInts[i] = codewordBytes[i] & 0xff;
  }
  int numECCodewords = numCodewords - numDataCodewords;

  try {
    rsDecoder_.decode(codewordInts, numECCodewords);
  } catch (ReedSolomonException const& ignored) {
    (void)ignored;
    throw ChecksumException();
  }

  for (int i = 0; i < numDataCodewords; i++) {
    codewordBytes[i] = (char)codewordInts[i];
  }
}

Ref<DecoderResult> Decoder::decode(Ref<BitMatrix> bits) {
  // Construct a parser and read version, error-correction level
  BitMatrixParser parser(bits);

  // std::cerr << *bits << std::endl;

  Version *version = parser.readVersion();
  ErrorCorrectionLevel &ecLevel = parser.readFormatInformation()->getErrorCorrectionLevel();


  // Read codewords
  ArrayRef<char> codewords(parser.readCodewords());


  // Separate into data blocks
  std::vector<Ref<DataBlock> > dataBlocks(DataBlock::getDataBlocks(codewords, version, ecLevel));


  // Count total number of data bytes
  int totalBytes = 0;
  for (size_t i = 0; i < dataBlocks.size(); i++) {
    totalBytes += dataBlocks[i]->getNumDataCodewords();
  }
  ArrayRef<char> resultBytes(totalBytes);
  int resultOffset = 0;


  // Error-correct and copy data blocks together into a stream of bytes
  for (size_t j = 0; j < dataBlocks.size(); j++) {
    Ref<DataBlock> dataBlock(dataBlocks[j]);
    ArrayRef<char> codewordBytes = dataBlock->getCodewords();
    int numDataCodewords = dataBlock->getNumDataCodewords();
    correctErrors(codewordBytes, numDataCodewords);
    for (int i = 0; i < numDataCodewords; i++) {
      resultBytes[resultOffset++] = codewordBytes[i];
    }
  }

  return DecodedBitStreamParser::decode(resultBytes,
                                        version,
                                        ecLevel,
                                        DecodedBitStreamParser::Hashtable());
}

