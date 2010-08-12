#ifndef __FINDER_PATTERN_FINDER_H__
#define __FINDER_PATTERN_FINDER_H__

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

#include <zxing/qrcode/detector/FinderPattern.h>
#include <zxing/qrcode/detector/FinderPatternInfo.h>
#include <zxing/common/Counted.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/ResultPointCallback.h>
#include <vector>

namespace zxing {

class DecodeHints;

namespace qrcode {

class FinderPatternFinder {
private:
  static int CENTER_QUORUM;
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
  std::vector<Ref<FinderPattern> > selectBestPatterns();
  static std::vector<Ref<FinderPattern> > orderBestPatterns(std::vector<Ref<FinderPattern> > patterns);
public:
  static float distance(Ref<ResultPoint> p1, Ref<ResultPoint> p2);
  FinderPatternFinder(Ref<BitMatrix> image, Ref<ResultPointCallback>const&);
  Ref<FinderPatternInfo> find(DecodeHints const& hints);
};
}
}

#endif // __FINDER_PATTERN_FINDER_H__
