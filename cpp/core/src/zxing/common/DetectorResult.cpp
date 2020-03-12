// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  DetectorResult.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 14/05/2008.
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

#include <zxing/common/DetectorResult.h>

#include "zxing/ResultPoint.h"                  // for ResultPoint
#include "zxing/common/BitMatrix.h"             // for BitMatrix
#include "zxing/common/Counted.h"               // for Ref
#include "zxing/common/PerspectiveTransform.h"  // for PerspectiveTransform

namespace pping {

DetectorResult::DetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint> > points, 
    pping::Ref<pping::PerspectiveTransform> perspectiveTransform) :
  bits_(bits), points_(points), perspectiveTransform_(perspectiveTransform) {
}

DetectorResult::DetectorResult(Ref<BitMatrix> bits, std::vector<Ref<ResultPoint> > points) :
  bits_(bits), points_(points) {
}

}
