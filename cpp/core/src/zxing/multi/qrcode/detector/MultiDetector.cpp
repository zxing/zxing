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

#include <zxing/ReaderException.h>                                 // for ReaderException
#include <zxing/multi/qrcode/detector/MultiDetector.h>
#include <zxing/multi/qrcode/detector/MultiFinderPatternFinder.h>  // for MultiFinderPatternFinder

#include "zxing/DecodeHints.h"                                     // for DecodeHints
#include "zxing/ResultPointCallback.h"                             // for ResultPointCallback
#include "zxing/common/BitMatrix.h"                                // for BitMatrix
#include "zxing/common/Counted.h"                                  // for Ref
#include "zxing/common/DetectorResult.h"                           // for DetectorResult
#include "zxing/qrcode/detector/FinderPatternInfo.h"               // for FinderPatternInfo
#include "zxing/qrcode/detector/ZXingQRCodeDetector.h"             // for Detector

namespace pping {
namespace multi {
using namespace pping::qrcode;

MultiDetector::MultiDetector(Ref<BitMatrix> image) : Detector(image) {}

MultiDetector::~MultiDetector(){}

Fallible<std::vector<Ref<DetectorResult>>> MultiDetector::detectMulti(DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC {
  Ref<BitMatrix> image = getImage();
  MultiFinderPatternFinder finder = MultiFinderPatternFinder(image, hints.getResultPointCallback());
  auto const tryFindMulti(finder.findMulti(hints));
  if(!tryFindMulti)
      return tryFindMulti.error();

  std::vector<Ref<FinderPatternInfo> > info = *tryFindMulti;
  std::vector<Ref<DetectorResult> > result;
  for(unsigned int i = 0; i < info.size(); i++) {

      auto const processInfo(processFinderPatternInfo(info[i]));
      if(processInfo)
          result.push_back(*processInfo);
  }

  return result;
}

} // End zxing::multi namespace
} // End zxing namespace
