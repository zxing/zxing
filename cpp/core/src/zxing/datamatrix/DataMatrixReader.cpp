// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  DataMatrixReader.cpp
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

#include <zxing/datamatrix/DataMatrixReader.h>
#include <zxing/datamatrix/detector/ZXingDataMatrixDetector.h>  // for Detector
#include <vector>                                               // for allocator, vector

#include "Log.h"                                                // for LOGV
#include "zxing/BarcodeFormat.h"                                // for BarcodeFormat::DATA_MATRIX
#include "zxing/BinaryBitmap.h"                                 // for BinaryBitmap
#include "zxing/DecodeHints.h"                                  // for DecodeHints
#include "zxing/Result.h"                                       // for Result
#include "zxing/ResultPoint.h"                                  // for ResultPoint
#include "zxing/ResultPointCallback.h"                          // for ResultPointCallback
#include "zxing/common/BitMatrix.h"                             // for BitMatrix
#include "zxing/common/DecoderResult.h"                         // for DecoderResult
#include "zxing/common/DetectorResult.h"                        // for DetectorResult
#include "zxing/common/Str.h"                                   // for String
#include "zxing/datamatrix/decoder/ZXingDataMatrixDecoder.h"    // for Decoder

#ifdef DEBUG
#include "Utils/stringstreamlite.hpp"
#endif

namespace pping {
namespace datamatrix {

using namespace std;

DataMatrixReader::DataMatrixReader() noexcept :
    decoder_() {
}

FallibleRef<Result> DataMatrixReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC {
  (void)hints;
  LOGV("decoding image %p", image.object_);

  auto const blackMatrix(image->getBlackMatrix());
  if (!blackMatrix)
      return blackMatrix.error();
  Detector detector(*blackMatrix);

  LOGV("(1) created detector %p", &detector);

  auto const detectorResult(detector.detect());
  if(!detectorResult)
      return detectorResult.error();

  LOGV("(2) detected, have detectorResult %p", detectorResult.object_);

  std::vector<Ref<ResultPoint> > points(detectorResult->getPoints());
    
    for (const auto& point : points) {
        hints.getResultPointCallback()->foundPossibleResultPoint(*point.object_);
    }
    
    hints.getResultPointCallback()->finishedFindingPoints();


#ifdef DEBUG
  LOGV("(3) extracted points %p", &points);
  LOGV("found " JL_SIZE_T_SPECIFIER " points:", points.size());
  for (size_t i = 0; i < points.size(); i++) {
    LOGV("   %f, %f", points[i]->getX(), points[i]->getY());
  }
  mb::stringstreamlite ss;
  ss << *(detectorResult->getBits());
  LOGV("bits:%s", ss.str().c_str());
#endif

  auto const decoderResult(decoder_.decode(detectorResult->getBits()));
  if(!decoderResult)
      return decoderResult.error();

  LOGV("(4) decoded, have decoderResult %p", decoderResult.object_);

  Ref<Result> result(
    new Result(decoderResult->getText(), decoderResult->getRawBytes(), points, BarcodeFormat::DATA_MATRIX));
  LOGV("(5) created result %p, returning", result.object_);

  return result;
}

DataMatrixReader::~DataMatrixReader() {
}

}
}
