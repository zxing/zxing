// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#pragma once
/*
 *  HybridBinarizer.h
 *  zxing
 *
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

#include <zxing/common/GlobalHistogramBinarizer.h>  // for GlobalHistogramBinarizer

#include "zxing/common/Counted.h"                   // for Ref

namespace pping {
    
class Binarizer;
class BitArray;
class BitMatrix;
class LuminanceSource;

    class HybridBinarizer : public GlobalHistogramBinarizer {
    private:
      mutable Ref<BitMatrix> matrix_;
      mutable Ref<BitArray > cached_row_;

    public:
        HybridBinarizer(Ref<LuminanceSource> source) noexcept;
        ~HybridBinarizer();
        
        virtual FallibleRef<BitMatrix> getBlackMatrix() const MB_NOEXCEPT_EXCEPT_BADALLOC override;
        Ref<Binarizer> createBinarizer(Ref<LuminanceSource> source) const MB_NOEXCEPT_EXCEPT_BADALLOC override;
  private:
    // We'll be using one-D arrays because C++ can't dynamically allocate 2D
    // arrays
    int* calculateBlackPoints(unsigned char* luminances,
                              int subWidth,
                              int subHeight,
                              int width,
                              int height) const MB_NOEXCEPT_EXCEPT_BADALLOC;
    void calculateThresholdForBlock(unsigned char* luminances,
                                    int subWidth,
                                    int subHeight,
                                    int width,
                                    int height,
                                    int blackPoints[],
                                    Ref<BitMatrix> const& matrix) const noexcept;
    void thresholdBlock(unsigned char* luminances,
                        int xoffset,
                        int yoffset,
                        int threshold,
                        int stride,
                        Ref<BitMatrix> const& matrix) const noexcept;
    };

}

