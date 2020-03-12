#pragma once

/*
 *  Copyright 2011 ZXing authors
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

#include <zxing/qrcode/detector/ZXingQRCodeFinderPatternFinder.h>  // for FinderPatternFinder
#include <vector>                                                  // for vector

#include "zxing/DecodeHints.h"                                     // for DecodeHints

namespace pping {
class BitMatrix;
class ResultPointCallback;
namespace qrcode {
class FinderPattern;
class FinderPatternInfo;
}  // namespace qrcode
template <typename T> class Ref;
}  // namespace pping

namespace pping {
namespace multi {
class MultiFinderPatternFinder : pping::qrcode::FinderPatternFinder {
  private:
    Fallible<std::vector<std::vector<Ref<qrcode::FinderPattern> > > > selectBestPatterns() MB_NOEXCEPT_EXCEPT_BADALLOC;

    static const float MAX_MODULE_COUNT_PER_EDGE;
    static const float MIN_MODULE_COUNT_PER_EDGE;
    static const float DIFF_MODSIZE_CUTOFF_PERCENT;
    static const float DIFF_MODSIZE_CUTOFF;

  public:
    MultiFinderPatternFinder(Ref<BitMatrix> image, Ref<ResultPointCallback> resultPointCallback);
    virtual ~MultiFinderPatternFinder();
    virtual Fallible<std::vector<Ref<qrcode::FinderPatternInfo> > > findMulti(DecodeHints const& hints) MB_NOEXCEPT_EXCEPT_BADALLOC;


};
}
}

