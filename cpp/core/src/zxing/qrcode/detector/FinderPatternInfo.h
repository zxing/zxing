#pragma once

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

#include <zxing/common/Counted.h>                            // for Ref, Counted
#include <zxing/qrcode/detector/ZXingQRCodeFinderPattern.h>  // for FinderPattern
#include <vector>                                            // for vector

namespace pping {
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

