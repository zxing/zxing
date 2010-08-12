#ifndef __ALIGNMENT_PATTERN_FINDER_H__
#define __ALIGNMENT_PATTERN_FINDER_H__

/*
 *  AlignmentPatternFinder.h
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

#include "AlignmentPattern.h"
#include <zxing/common/Counted.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/ResultPointCallback.h>
#include <vector>

namespace zxing {
namespace qrcode {

class AlignmentPatternFinder : public Counted {
private:
  static int CENTER_QUORUM;
  static int MIN_SKIP;
  static int MAX_MODULES;

  Ref<BitMatrix> image_;
  std::vector<AlignmentPattern *> *possibleCenters_;
  size_t startX_;
  size_t startY_;
  size_t width_;
  size_t height_;
  float moduleSize_;

  static float centerFromEnd(std::vector<int> &stateCount, int end);
  bool foundPatternCross(std::vector<int> &stateCount);

  float crossCheckVertical(size_t startI, size_t centerJ, int maxCount, int originalStateCountTotal);

  Ref<AlignmentPattern> handlePossibleCenter(std::vector<int> &stateCount, size_t i, size_t j);

public:
  AlignmentPatternFinder(Ref<BitMatrix> image, size_t startX, size_t startY, size_t width, size_t height,
                         float moduleSize, Ref<ResultPointCallback>const& callback);
  ~AlignmentPatternFinder();
  Ref<AlignmentPattern> find();
  
private:
  AlignmentPatternFinder(const AlignmentPatternFinder&);
  AlignmentPatternFinder& operator =(const AlignmentPatternFinder&);
  
  Ref<ResultPointCallback> callback_;
};
}
}

#endif // __ALIGNMENT_PATTERN_FINDER_H__
