#ifndef __FINDER_PATTERN_FINDER_H__
#define __FINDER_PATTERN_FINDER_H__

/*
 *  FinderPatternFinder.h
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
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

#include "FinderPattern.h"
#include "FinderPatternInfo.h"
#include "../../common/Counted.h"
#include "../../MonochromeBitmapSource.h"
#include <vector>

namespace qrcode {
  namespace detector {
    using namespace std;
    using namespace common;
    
    class FinderPatternFinder {
    private:
      static int CENTER_QUORUM;
      static int MIN_SKIP;
      static int MAX_MODULES;
      
      Ref<MonochromeBitmapSource> image_;
      vector<Ref<FinderPattern> > possibleCenters_;
      bool hasSkipped_;
      
      static float centerFromEnd(valarray<int> &stateCount, int end);
      static bool foundPatternCross(valarray<int> &stateCount);
      
      float crossCheckVertical(size_t startI, size_t centerJ, int maxCount,
                               int originalStateCountTotal);
      float crossCheckHorizontal(size_t startJ, size_t centerI, int maxCount,
                                 int originalStateCountTotal);
      
      bool handlePossibleCenter(valarray<int> &stateCount, size_t i, size_t j);
      int findRowSkip();
      bool haveMultiplyConfirmedCenters();
      ArrayRef<Ref<FinderPattern> > selectBestPatterns();
      static ArrayRef<Ref<FinderPattern> > orderBestPatterns
      (ArrayRef<Ref<FinderPattern> > patterns);
            
    public:
      static float distance(Ref<ResultPoint> p1, Ref<ResultPoint> p2);
      FinderPatternFinder(Ref<MonochromeBitmapSource> image);
      Ref<FinderPatternInfo> find();
    };
  }
}

#endif // __FINDER_PATTERN_FINDER_H__
