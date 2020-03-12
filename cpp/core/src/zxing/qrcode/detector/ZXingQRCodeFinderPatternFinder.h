// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#pragma once

/*
 *  FinderPatternFinder.h
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


#include "zxing/ResultPointCallback.h"                       // for ResultPointCallback
#include "zxing/common/BitMatrix.h"                          // for BitMatrix
#include "zxing/common/Counted.h"                            // for Ref
#include "zxing/common/Error.hpp"
#include "zxing/qrcode/detector/ZXingQRCodeFinderPattern.h"  // for FinderPattern

#include <stddef.h>                                          // for size_t
#include <vector>                                            // for vector

namespace pping {

class DecodeHints;
class ResultPoint;
namespace qrcode {
class FinderPatternInfo;
}  // namespace qrcode

namespace qrcode {

class FinderPatternFinder {
private:
  static int CENTER_QUORUM;

protected:
  static int MIN_SKIP;
  static int MAX_MODULES;

  Ref<BitMatrix> image_;
  std::vector<Ref<FinderPattern> > possibleCenters_;
  bool hasSkipped_;

  Ref<ResultPointCallback> callback_;

  /** stateCount must be int[5] */
  static float centerFromEnd(int* stateCount, int end);
  static bool foundPatternCross(int* stateCount);

  float crossCheckVertical(size_t startI, size_t centerJ, int maxCount, int originalStateCountTotal);
  float crossCheckHorizontal(size_t startJ, size_t centerI, int maxCount, int originalStateCountTotal);

  /** stateCount must be int[5] */
  bool handlePossibleCenter(int* stateCount, size_t i, size_t j);
  int findRowSkip();
  bool haveMultiplyConfirmedCenters();
  Fallible<std::vector<Ref<FinderPattern> >> selectBestPatterns() noexcept;
  static std::vector<Ref<FinderPattern> > orderBestPatterns(std::vector<Ref<FinderPattern> > patterns);

  Ref<BitMatrix> getImage();
  std::vector<Ref<FinderPattern> >& getPossibleCenters();

public:
  static float distance(Ref<ResultPoint> p1, Ref<ResultPoint> p2) noexcept;
  FinderPatternFinder(Ref<BitMatrix> image, Ref<ResultPointCallback>const&) noexcept;
  FallibleRef<FinderPatternInfo> find(DecodeHints const& hints) MB_NOEXCEPT_EXCEPT_BADALLOC;
};
}
}

