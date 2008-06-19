#ifndef __ALIGNMENT_PATTERN_FINDER_H__
#define __ALIGNMENT_PATTERN_FINDER_H__

/*
 *  AlignmentPatternFinder.h
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

#include "AlignmentPattern.h"
#include "../../common/Counted.h"
#include "../../MonochromeBitmapSource.h"
#include <vector>

namespace qrcode {
  namespace detector {
    using namespace std;
    using namespace common;
    
    class AlignmentPatternFinder : public Counted {
    private:
      static int CENTER_QUORUM;
      static int MIN_SKIP;
      static int MAX_MODULES;
      
      Ref<MonochromeBitmapSource> image_;
      vector<AlignmentPattern *> *possibleCenters_;
      size_t startX_;
      size_t startY_;
      size_t width_;
      size_t height_;
      float moduleSize_;
      
      static float centerFromEnd(valarray<int> &stateCount, int end);
      bool foundPatternCross(valarray<int> &stateCount);
      
      float crossCheckVertical(size_t startI, size_t centerJ, int maxCount,
                               int originalStateCountTotal);
      
      Ref<AlignmentPattern> handlePossibleCenter(valarray<int> &stateCount, 
                                                 size_t i, size_t j);
      
    public:
      AlignmentPatternFinder(Ref<MonochromeBitmapSource> image,
                             size_t startX, size_t startY,
                             size_t width, size_t height, 
                             float moduleSize);
      ~AlignmentPatternFinder();
      Ref<AlignmentPattern> find();
    };
  }
}

#endif // __ALIGNMENT_PATTERN_FINDER_H__
