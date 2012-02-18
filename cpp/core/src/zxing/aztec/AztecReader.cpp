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
#include <zxing/aztec/detector/Detector.h>
#include <iostream>

namespace zxing {
  namespace aztec {
        
    AztecReader::AztecReader() : decoder_() {
      // nothing
    };
        
    Ref<Result> AztecReader::decode(Ref<zxing::BinaryBitmap> image) {
      Detector detector(image->getBlackMatrix());
            
      Ref<AztecDetectorResult> detectorResult(detector.detect());
            
      std::vector<Ref<ResultPoint> > points(detectorResult->getPoints());
            
      Ref<DecoderResult> decoderResult(decoder_.decode(detectorResult));
            
      Ref<Result> result(new Result(decoderResult->getText(),
                                    decoderResult->getRawBytes(),
                                    points,
                                    BarcodeFormat_AZTEC));
            
      return result;
    }
        
    Ref<Result> AztecReader::decode(Ref<BinaryBitmap> image, DecodeHints) {
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
