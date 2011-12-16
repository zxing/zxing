#ifndef __FINDER_PATTERN_INFO_H__
#define __FINDER_PATTERN_INFO_H__

/*
 *  FinderPatternInfo.h
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
#include <zxing/common/Counted.h>
#include <zxing/common/Array.h>
#include <vector>

namespace zxing {
namespace qrcode {

class FinderPatternInfo : public Counted {
private:
  Ref<FinderPattern> bottomLeft_;
  Ref<FinderPattern> topLeft_;
  Ref<FinderPattern> topRight_;

public:
  FinderPatternInfo(std::vector<Ref<FinderPattern> > patternCenters);

  Ref<FinderPattern> getBottomLeft();
  Ref<FinderPattern> getTopLeft();
  Ref<FinderPattern> getTopRight();
};
}
}

#endif // __FINDER_PATTERN_INFO_H__
