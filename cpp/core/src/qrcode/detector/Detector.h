#ifndef __DETECTOR_H__
#define __DETECTOR_H__

/*
 *  Detector.h
 *  zxing
 *
 *  Created by Christian Brunschen on 14/05/2008.
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

#include "../../common/Counted.h"
#include "../../common/DetectorResult.h"
#include "../../MonochromeBitmapSource.h"
#include "AlignmentPattern.h"

namespace qrcode {
  namespace detector {
    
    using namespace std;
    using namespace common;
    
    class Detector : public Counted {
    private:
      Ref<MonochromeBitmapSource> image_;
      static Ref<BitMatrix> sampleGrid(Ref<MonochromeBitmapSource> image,
                                       Ref<ResultPoint> topLeft,
                                       Ref<ResultPoint> topRight,
                                       Ref<ResultPoint> bottomLeft,
                                       Ref<ResultPoint> alignmentPattern,
                                       int dimension);
      static int computeDimension(Ref<ResultPoint> topLeft,
                                  Ref<ResultPoint> topRight,
                                  Ref<ResultPoint> bottomLeft,
                                  float moduleSize);
      float calculateModuleSize(Ref<ResultPoint> topLeft, 
                                Ref<ResultPoint> topRight, 
                                Ref<ResultPoint> bottomLeft);
      float calculateModuleSizeOneWay(Ref<ResultPoint> pattern, 
                                      Ref<ResultPoint> otherPattern);
      float sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, 
                                             int toX, int toY);
      float sizeOfBlackWhiteBlackRun(int fromX, int fromY, 
                                     int toX, int toY);
      Ref<AlignmentPattern>  findAlignmentInRegion(float overallEstModuleSize,
                                                   int estAlignmentX,
                                                   int estAlignmentY,
                                                   float allowanceFactor);
    public:
      Detector(Ref<MonochromeBitmapSource> image);
      Ref<DetectorResult> detect();
    };
  }
}

#endif // __DETECTOR_H__
