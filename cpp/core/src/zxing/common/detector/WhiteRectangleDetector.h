#pragma once

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

#include <zxing/common/BitMatrix.h>  // for BitMatrix
#include <zxing/common/Counted.h>    // for Ref, Counted
#include "zxing/common/Error.hpp"

#include <vector>                    // for vector


namespace pping {

class ResultPoint;

class WhiteRectangleDetector : public Counted {
  private:
    static int INIT_SIZE;
    static int CORR;
    Ref<BitMatrix> image_;
    int width_;
    int height_;
    int leftInit_;
    int rightInit_;
    int downInit_;
    int upInit_;

  public:
    static FallibleRef<WhiteRectangleDetector> createWhiteRectangleDetector(Ref<BitMatrix> image) MB_NOEXCEPT_EXCEPT_BADALLOC;
    static FallibleRef<WhiteRectangleDetector> createWhiteRectangleDetector(Ref<BitMatrix> image, int initSize, int x, int y) MB_NOEXCEPT_EXCEPT_BADALLOC;

    Fallible<std::vector<Ref<ResultPoint> > > detect() MB_NOEXCEPT_EXCEPT_BADALLOC;

  private: 
    WhiteRectangleDetector(Ref<BitMatrix> image, int leftInit, int rightInit, int upInit, int downInit) noexcept;

    Ref<ResultPoint> getBlackPointOnSegment(float aX, float aY, float bX, float bY);
    std::vector<Ref<ResultPoint> > centerEdges(Ref<ResultPoint> y, Ref<ResultPoint> z,
                                    Ref<ResultPoint> x, Ref<ResultPoint> t);
    bool containsBlackPoint(int a, int b, int fixed, bool horizontal);
};
}

