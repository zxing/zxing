#pragma once

/*
 *  DetectorResult.h
 *  zxing
 *
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

#include <zxing/ResultPoint.h>                  // for ResultPoint
#include <zxing/common/BitMatrix.h>             // for BitMatrix
#include <zxing/common/Counted.h>               // for Ref, Counted
#include <zxing/common/PerspectiveTransform.h>  // for PerspectiveTransform
#include <zxing/common/Error.hpp>
#include <vector>                               // for vector

namespace pping {

class DetectorResult : public Counted {
private:
  Ref<BitMatrix> bits_;
  std::vector<Ref<ResultPoint>> points_;
  pping::Ref<pping::PerspectiveTransform> perspectiveTransform_;

public:
        DetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint>> points,
          pping::Ref<pping::PerspectiveTransform> perspectiveTransform);

        DetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint>> points);

  auto const & getBits                () const noexcept { return bits_                ; }
  auto const & getPoints              () const noexcept { return points_              ; }
  auto const & getPerspectiveTransform() const noexcept { return perspectiveTransform_; }
};
}

