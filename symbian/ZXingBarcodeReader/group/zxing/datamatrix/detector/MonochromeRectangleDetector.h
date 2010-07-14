#ifndef __MONOCHROMERECTANGLEDETECTOR_H__
#define __MONOCHROMERECTANGLEDETECTOR_H__

/*
 *  MonochromeRectangleDetector.h
 *  zxing
 *
 *  Created by Luiz Silva on 09/02/2010.
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

#include <vector>
#include <zxing/ReaderException.h>
#include <zxing/ResultPoint.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/Counted.h>
#include <zxing/datamatrix/detector/CornerPoint.h>

namespace zxing {
namespace datamatrix {

struct TwoInts: public Counted {
	int start;
	int end;
};

class MonochromeRectangleDetector : public Counted {
private:
  static const int MAX_MODULES = 32;
  Ref<BitMatrix> image_;

public:
  MonochromeRectangleDetector(Ref<BitMatrix> image) : image_(image) {  };

  std::vector<Ref<CornerPoint> > detect();

private:
  Ref<CornerPoint> findCornerFromCenter(int centerX, int deltaX, int left, int right,
      int centerY, int deltaY, int top, int bottom, int maxWhiteRun);

  Ref<TwoInts> blackWhiteRange(int fixedDimension, int maxWhiteRun, int minDim, int maxDim,
      bool horizontal);

  int max(int a, float b) { return (float) a > b ? a : (int) b;};
};
}
}

#endif // __MONOCHROMERECTANGLEDETECTOR_H__
