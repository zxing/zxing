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

#include <zxing/multi/qrcode/detector/MultiDetector.h>
#include <zxing/multi/qrcode/detector/MultiFinderPatternFinder.h>
#include <zxing/ReaderException.h>

namespace zxing {
namespace multi {
using namespace zxing::qrcode;

MultiDetector::MultiDetector(Ref<BitMatrix> image) : Detector(image) {}

MultiDetector::~MultiDetector(){}

std::vector<Ref<DetectorResult> > MultiDetector::detectMulti(DecodeHints hints){
  Ref<BitMatrix> image = getImage();
  MultiFinderPatternFinder finder = MultiFinderPatternFinder(image, hints.getResultPointCallback());
  std::vector<Ref<FinderPatternInfo> > info = finder.findMulti(hints);
  std::vector<Ref<DetectorResult> > result;
  for(unsigned int i = 0; i < info.size(); i++){
    try{
      result.push_back(processFinderPatternInfo(info[i]));
    } catch (ReaderException const& e){
      (void)e;
      // ignore
    }
  }

  return result;
}

} // End zxing::multi namespace
} // End zxing namespace
