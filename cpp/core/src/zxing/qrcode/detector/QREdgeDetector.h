/*
 *  QREdgeDetector.h
 *  zxing
 *
 *  Created by Ralf Kistner on 7/12/2009.
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

#ifndef QREDGEDETECTOR_H_
#define QREDGEDETECTOR_H_

#include <zxing/qrcode/detector/Detector.h>
#include <zxing/common/Point.h>

namespace zxing {
namespace qrcode {

class QREdgeDetector : public Detector {
public:
  QREdgeDetector(Ref<BitMatrix> image);

  virtual Ref<PerspectiveTransform> createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
      ResultPoint > bottomLeft, Ref<ResultPoint> alignmentPattern, int dimension);

private:
  Point findCorner(const BitMatrix& image, Point topLeft, Point topRight, Point bottomLeft, int dimension);
  Line findPatternEdge(const BitMatrix& image, Point pattern, Point opposite, Point direction, bool invert);

  Point endOfReverseBlackWhiteBlackRun(const BitMatrix& image, Point from, Point to);

  Ref<PerspectiveTransform> get1CornerTransform(Point topLeft, Point topRight, Point bottomLeft, Point corner, int dimension);
};

}
}
#endif // QREDGEDETECTOR_H_
