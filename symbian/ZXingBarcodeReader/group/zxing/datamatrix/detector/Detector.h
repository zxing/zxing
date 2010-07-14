#ifndef __DETECTOR_H__
#define __DETECTOR_H__

/*
 *  Detector.h
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


#include <zxing/common/Counted.h>
#include <zxing/common/DetectorResult.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/PerspectiveTransform.h>
#include <zxing/datamatrix/detector/MonochromeRectangleDetector.h>


namespace zxing {
namespace datamatrix {

class ResultPointsAndTransitions : public Counted {
private:
  Ref<CornerPoint> to_;
  Ref<CornerPoint> from_;
  int transitions_;

public:
  ResultPointsAndTransitions();
  ResultPointsAndTransitions(Ref<CornerPoint> from, Ref<CornerPoint> to, int transitions);
  Ref<CornerPoint> getFrom();
  Ref<CornerPoint> getTo();
  int getTransitions();
};

class Detector : public Counted {
private:
  Ref<BitMatrix> image_;

protected:
  Ref<BitMatrix> sampleGrid(Ref<BitMatrix> image, int dimension, Ref<PerspectiveTransform> transform);

  void insertionSort(std::vector<Ref<ResultPointsAndTransitions> >& vector);

  Ref<ResultPointsAndTransitions> transitionsBetween(Ref<CornerPoint> from, Ref<CornerPoint> to);
  int min(int a, int b) { return a > b ? b : a; };

public:
  Ref<BitMatrix> getImage();
  Detector(Ref<BitMatrix> image);

  virtual Ref<PerspectiveTransform> createTransform(Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, Ref <
      ResultPoint > bottomLeft, Ref<ResultPoint> bottomRight, int dimension);

  Ref<DetectorResult> detect();
  void orderBestPatterns(std::vector<Ref<CornerPoint> > &patterns);
  float distance(float x1, float x2, float y1, float y2);
private:
  int compare(Ref<ResultPointsAndTransitions> a, Ref<ResultPointsAndTransitions> b);
  float crossProductZ(Ref<ResultPoint> pointA, Ref<ResultPoint> pointB, Ref<ResultPoint> pointC);
};

}
}

#endif // __DETECTOR_H__
