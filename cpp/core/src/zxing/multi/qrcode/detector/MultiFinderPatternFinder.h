#ifndef __MULTI_FINDER_PATTERN_FINDER_H__
#define __MULTI_FINDER_PATTERN_FINDER_H__

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

#include <zxing/qrcode/detector/FinderPattern.h>
#include <zxing/qrcode/detector/FinderPatternFinder.h>
#include <zxing/qrcode/detector/FinderPatternInfo.h>

namespace zxing {
namespace multi {

class MultiFinderPatternFinder : zxing::qrcode::FinderPatternFinder {
  private:
    std::vector<std::vector<Ref<zxing::qrcode::FinderPattern> > > selectBestPatterns();

    static const float MAX_MODULE_COUNT_PER_EDGE;
    static const float MIN_MODULE_COUNT_PER_EDGE;
    static const float DIFF_MODSIZE_CUTOFF_PERCENT;
    static const float DIFF_MODSIZE_CUTOFF;

  public:
    MultiFinderPatternFinder(Ref<BitMatrix> image, Ref<ResultPointCallback> resultPointCallback);
    virtual ~MultiFinderPatternFinder();
    virtual std::vector<Ref<zxing::qrcode::FinderPatternInfo> > findMulti(DecodeHints const& hints);


};

}
}

#endif // __MULTI_FINDER_PATTERN_FINDER_H__
