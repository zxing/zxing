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

#include <zxing/BarcodeFormat.h>                        // for BarcodeFormat::QR_CODE
#include <zxing/ReaderException.h>                      // for ReaderException
#include <zxing/multi/qrcode/QRCodeMultiReader.h>
#include <zxing/multi/qrcode/detector/MultiDetector.h>  // for MultiDetector

#include "zxing/BinaryBitmap.h"                         // for BinaryBitmap
#include "zxing/Result.h"                               // for Result
#include "zxing/ResultPoint.h"                          // for ResultPoint
#include "zxing/common/BitMatrix.h"                     // for BitMatrix
#include "zxing/common/Counted.h"                       // for Ref
#include "zxing/common/DecoderResult.h"                 // for DecoderResult
#include "zxing/common/DetectorResult.h"                // for DetectorResult
#include "zxing/common/Str.h"                           // for String
#include "zxing/qrcode/decoder/ZXingQRCodeDecoder.h"    // for Decoder

namespace pping {
namespace multi {
QRCodeMultiReader::QRCodeMultiReader(){}

QRCodeMultiReader::~QRCodeMultiReader(){}

Fallible<std::vector<Ref<Result>>> QRCodeMultiReader::decodeMultiple(Ref<BinaryBitmap> image,
  DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC
{
  auto blackMatrix(image->getBlackMatrix());
  if (!blackMatrix)
      return blackMatrix.error();
  MultiDetector detector(*blackMatrix);
  std::vector<Ref<Result> > results;

  auto const tryGetDetectorResult(detector.detectMulti(hints));
  if(!tryGetDetectorResult)
      return tryGetDetectorResult.error();

  std::vector<Ref<DetectorResult> > detectorResult = *tryGetDetectorResult;
  for (unsigned int i = 0; i < detectorResult.size(); i++) {
      auto const decoderResult(getDecoder().decode(detectorResult[i]->getBits()));
      if (!decoderResult)
          continue;
      auto const & points(detectorResult[i]->getPoints());
      Ref<Result> result(Ref<Result>(new Result((*decoderResult)->getText(), (*decoderResult)->getRawBytes(),
                                        points, BarcodeFormat::QR_CODE)));
      // result->putMetadata(ResultMetadataType.BYTE_SEGMENTS, decoderResult->getByteSegments());
      // result->putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, decoderResult->getECLevel().toString());
      results.push_back(result);
  }
  if (results.empty()){
    return failure<ReaderException>("No code detected");
  }
  return results;
}

} // End zxing::multi namespace
} // End zxing namespace
