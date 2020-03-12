#pragma once
/*
 *  GlobalHistogramBinarizer.h
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

#include <zxing/Binarizer.h>       // for Binarizer

#include "zxing/common/Counted.h"  // for Ref
#include "zxing/common/Error.hpp"

#include <vector>                  // for vector


namespace pping {
    
class BitArray;
class BitMatrix;
class LuminanceSource;

    class GlobalHistogramBinarizer : public Binarizer {
     private:
      mutable Ref<BitMatrix> cached_matrix_;
      mutable Ref<BitArray > cached_row_;
      mutable int            cached_row_num_;

    public:
        GlobalHistogramBinarizer(Ref<LuminanceSource> source) noexcept;
        ~GlobalHistogramBinarizer();
        
        virtual FallibleRef<BitArray > getBlackRow   (int y, Ref<BitArray> row) const MB_NOEXCEPT_EXCEPT_BADALLOC override;
        virtual FallibleRef<BitMatrix> getBlackMatrix(                        ) const MB_NOEXCEPT_EXCEPT_BADALLOC override;
        virtual Ref<Binarizer> createBinarizer(Ref<LuminanceSource> source) const MB_NOEXCEPT_EXCEPT_BADALLOC override;
        static Fallible<int> estimate(std::vector<int> &histogram) noexcept;
    };
    
}

