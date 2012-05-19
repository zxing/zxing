// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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
#include <zxing/common/detector/WhiteRectangleDetector.h>

namespace zxing {
namespace datamatrix {

class ResultPointsAndTransitions: public Counted {
  private:
    Ref<ResultPoint> to_;
    Ref<ResultPoint> from_;
    int transitions_;

  public:
    ResultPointsAndTransitions();
    ResultPointsAndTransitions(Ref<ResultPoint> from, Ref<ResultPoint> to, int transitions);
    Ref<ResultPoint> getFrom();
    Ref<ResultPoint> getTo();
    int getTransitions();
};

class Detector: public Counted {
  private:
    Ref<BitMatrix> image_;

  protected:
    Ref<BitMatrix> sampleGrid(Ref<BitMatrix> image, int dimensionX, int dimensionY,
        Ref<PerspectiveTransform> transform);

    void insertionSort(std::vector<Ref<ResultPointsAndTransitions> >& vector);

    Ref<ResultPoint> correctTopRightRectangular(Ref<ResultPoint> bottomLeft,
        Ref<ResultPoint> bottomRight, Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight,
        int dimensionTop, int dimensionRight);
    Ref<ResultPoint> correctTopRight(Ref<ResultPoint> bottomLeft, Ref<ResultPoint> bottomRight,
        Ref<ResultPoint> topLeft, Ref<ResultPoint> topRight, int dimension);
    bool isValid(Ref<ResultPoint> p);
    int distance(Ref<ResultPoint> a, Ref<ResultPoint> b);
    Ref<ResultPointsAndTransitions> transitionsBetween(Ref<ResultPoint> from, Ref<ResultPoint> to);
    int min(int a, int b) {
      return a > b ? b : a;
    }
    /**
     * Ends up being a bit faster than round(). This merely rounds its
     * argument to the nearest int, where x.5 rounds up.
     */
    int round(float d) {
      return (int) (d + 0.5f);
    }

  public:
    Ref<BitMatrix> getImage();
    Detector(Ref<BitMatrix> image);

    virtual Ref<PerspectiveTransform> createTransform(Ref<ResultPoint> topLeft,
        Ref<ResultPoint> topRight, Ref<ResultPoint> bottomLeft, Ref<ResultPoint> bottomRight,
        int dimensionX, int dimensionY);

    Ref<DetectorResult> detect();

  private:
    int compare(Ref<ResultPointsAndTransitions> a, Ref<ResultPointsAndTransitions> b);
};

}
}

#endif // __DETECTOR_H__
