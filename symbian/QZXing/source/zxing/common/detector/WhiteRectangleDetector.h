#ifndef __WHITERECTANGLEDETECTOR_H__
#define __WHITERECTANGLEDETECTOR_H__

/*
 *  WhiteRectangleDetector.h
 *
 *
 *  Created by Luiz Silva on 09/02/2010.
 *  Copyright 2010  authors All rights reserved.
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
#include <zxing/ResultPoint.h>


namespace zxing {

class WhiteRectangleDetector : public Counted {
  private:
    static int INIT_SIZE;
    static int CORR;
    Ref<BitMatrix> image_;
    int width_;
    int height_;

  public:
    WhiteRectangleDetector(Ref<BitMatrix> image);
    std::vector<Ref<ResultPoint> > detect();

  private: 
    int round(float a);
    Ref<ResultPoint> getBlackPointOnSegment(float aX, float aY, float bX, float bY);
    int distanceL2(float aX, float aY, float bX, float bY);
    std::vector<Ref<ResultPoint> > centerEdges(Ref<ResultPoint> y, Ref<ResultPoint> z,
                                    Ref<ResultPoint> x, Ref<ResultPoint> t);
    bool containsBlackPoint(int a, int b, int fixed, bool horizontal);
};
}

#endif
