#ifndef __FINDER_PATTERN_INFO_H__
#define __FINDER_PATTERN_INFO_H__

/*
 *  FinderPatternInfo.h
 *  zxing
 *
 *  Created by Christian Brunschen on 13/05/2008.
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

#include "FinderPattern.h"
#include "../../common/Counted.h"
#include "../../common/Array.h"
#include <vector>

namespace qrcode {
  namespace detector {
    using namespace std;
    using namespace common;
    
    class FinderPatternInfo : public Counted {
    private:
      Ref<FinderPattern> bottomLeft_;
      Ref<FinderPattern> topLeft_;
      Ref<FinderPattern> topRight_;
      
    public:
      FinderPatternInfo(ArrayRef<Ref<FinderPattern> > patternCenters) :
      bottomLeft_(patternCenters[0]),
      topLeft_(patternCenters[1]),
      topRight_(patternCenters[2]) { }
      
      Ref<FinderPattern> getBottomLeft() { return bottomLeft_; }
      Ref<FinderPattern> getTopLeft() { return topLeft_; }
      Ref<FinderPattern> getTopRight() { return topRight_; }
    };
  }
}

#endif // __FINDER_PATTERN_INFO_H__
