#pragma once

/*
 *  Copyright 2011 ZXing authors
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

#include <zxing/DecodeHints.h>                          // for DecodeHints
#include <zxing/qrcode/detector/ZXingQRCodeDetector.h>  // for Detector
#include <vector>                                       // for vector

namespace pping {
class BitMatrix;
class DetectorResult;
template <typename T> class Ref;
}  // namespace pping

namespace pping {
namespace multi {
class MultiDetector : public pping::qrcode::Detector {
  public:
    MultiDetector(Ref<BitMatrix> image);
    virtual ~MultiDetector();
    virtual Fallible<std::vector<Ref<DetectorResult> > > detectMulti(DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC;
};
} // End zxing::multi namespace
} // End zxing namespace

