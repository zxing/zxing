// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#pragma once

/*
 *  Detector.h
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

#include "zxing/ResultPointCallback.h"  // for ResultPointCallback
#include "zxing/common/BitMatrix.h"     // for BitMatrix
#include "zxing/common/Counted.h"       // for Ref, Counted
#include "zxing/common/Error.hpp"

namespace pping {

class DecodeHints;
class DetectorResult;
class PerspectiveTransform;
class ResultPoint;
namespace qrcode {
class AlignmentPattern;
class FinderPatternInfo;
}  // namespace qrcode

namespace qrcode {

class Detector : public Counted {
private:
  Ref<BitMatrix> image_;
  Ref<ResultPointCallback> callback_;

protected:
  Ref<BitMatrix> getImage() const;
  Ref<ResultPointCallback> getResultPointCallback() const;

  static FallibleRef<BitMatrix> sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform>) MB_NOEXCEPT_EXCEPT_BADALLOC;
  static Fallible<int> computeDimension(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft,
                              float moduleSize) MB_NOEXCEPT_EXCEPT_BADALLOC;
  float calculateModuleSize(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft);
  float calculateModuleSizeOneWay(Ref<ResultPoint> pattern, Ref<ResultPoint> otherPattern);
  float sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY);
  float sizeOfBlackWhiteBlackRun(int fromX, int fromY, int toX, int toY);
  FallibleRef<AlignmentPattern> findAlignmentInRegion(float overallEstModuleSize, int estAlignmentX, int estAlignmentY,
      float allowanceFactor);
  FallibleRef<DetectorResult> processFinderPatternInfo(Ref<FinderPatternInfo> info);
public:
  virtual Ref<PerspectiveTransform> createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
      ResultPoint > bottomLeft, Ref<ResultPoint> alignmentPattern, int dimension);

  Detector(Ref<BitMatrix> image) noexcept;
  FallibleRef<DetectorResult> detect(DecodeHints const& hints) MB_NOEXCEPT_EXCEPT_BADALLOC;


};
}
}

