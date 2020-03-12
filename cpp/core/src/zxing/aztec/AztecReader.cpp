// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  AztecReader.cpp
 *  zxing
 *
 *  Created by Lukas Stabe on 08/02/2012.
 *  Copyright 2012 ZXing authors All rights reserved.
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

#include <zxing/aztec/AztecReader.h>
#include <zxing/aztec/detector/ZxingAztecDetector.h>    // for Detector
#include <vector>                                       // for allocator, vector

#include "zxing/BarcodeFormat.h"                        // for BarcodeFormat::AZTEC_BARCODE
#include "zxing/BinaryBitmap.h"                         // for BinaryBitmap
#include "zxing/DecodeHints.h"                          // for DecodeHints
#include "zxing/Result.h"                               // for Result
#include "zxing/ResultPoint.h"                          // for ResultPoint
#include "zxing/aztec/AztecDetectorResult.h"            // for AztecDetectorResult
#include "zxing/aztec/decoder/Decoder.h"                // for Decoder
#include "zxing/common/BitMatrix.h"                     // for BitMatrix
#include "zxing/common/DecoderResult.h"                 // for DecoderResult
#include "zxing/common/Str.h"                           // for String

namespace pping {
  namespace aztec {
        
    AztecReader::AztecReader() : decoder_() {
      // nothing
    }
        
    FallibleRef<Result> AztecReader::decode(Ref<pping::BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC {
      auto const blackMatrix(image->getBlackMatrix());
      if (!blackMatrix)
          return blackMatrix.error();
      Detector detector(*blackMatrix);
            

      auto const detectorResult(detector.detect());
      if(!detectorResult)
          return detectorResult.error();
            
      std::vector<Ref<ResultPoint> > points(detectorResult->getPoints());
            
      auto const getDecoderResult(decoder_.decode(*detectorResult));
      if(!getDecoderResult)
          return getDecoderResult.error();

      Ref<DecoderResult> decoderResult(*getDecoderResult);

      Ref<Result> result(new Result(decoderResult->getText(),
                                    decoderResult->getRawBytes(),
                                    points,
                                    BarcodeFormat::AZTEC_BARCODE,
                                    decoderResult->getByteSegments()));
            
      return result;
    }
        
    FallibleRef<Result> AztecReader::decode(Ref<BinaryBitmap> image, DecodeHints) MB_NOEXCEPT_EXCEPT_BADALLOC {
      //cout << "decoding with hints not supported for aztec" << "\n" << flush;
      return this->decode(image);
    }
        
    AztecReader::~AztecReader() {
      // nothing
    }
        
    Decoder& AztecReader::getDecoder() {
      return decoder_;
    }
        
  }
}
