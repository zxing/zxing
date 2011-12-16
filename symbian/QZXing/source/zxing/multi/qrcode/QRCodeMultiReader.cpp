// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Copyright 2011 ZXing authors All rights reserved.
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

#include <zxing/multi/qrcode/QRCodeMultiReader.h>
#include <zxing/ReaderException.h>
#include <zxing/multi/qrcode/detector/MultiDetector.h>
#include <zxing/BarcodeFormat.h>

namespace zxing {
namespace multi {
QRCodeMultiReader::QRCodeMultiReader(){}

QRCodeMultiReader::~QRCodeMultiReader(){}

std::vector<Ref<Result> > QRCodeMultiReader::decodeMultiple(Ref<BinaryBitmap> image, 
  DecodeHints hints)
{
  std::vector<Ref<Result> > results;
  MultiDetector detector(image->getBlackMatrix());

  std::vector<Ref<DetectorResult> > detectorResult =  detector.detectMulti(hints);
  for (unsigned int i = 0; i < detectorResult.size(); i++) {
    try {
      Ref<DecoderResult> decoderResult = getDecoder().decode(detectorResult[i]->getBits());
      std::vector<Ref<ResultPoint> > points = detectorResult[i]->getPoints();
      Ref<Result> result = Ref<Result>(new Result(decoderResult->getText(),
      decoderResult->getRawBytes(), 
      points, BarcodeFormat_QR_CODE));
      // result->putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult->getByteSegments());
      // result->putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult->getECLevel().toString());
      results.push_back(result);
    } catch (ReaderException re) {
    // ignore and continue 
    }
  }
  if (results.empty()){
    throw ReaderException("No code detected");
  }
  return results;
}

} // End zxing::multi namespace
} // End zxing namespace
