#pragma once

/*
 *  BinaryBitmap.h
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

#include <zxing/common/Counted.h>  // for Ref, Counted
#include "zxing/common/Error.hpp"

namespace pping {
    
class Binarizer;
class BitArray;
class BitMatrix;
class LuminanceSource;

    class BinaryBitmap : public Counted {
    private:
        Ref<Binarizer> binarizer_;
//		int cached_y_;
        
    public:
        BinaryBitmap(Ref<Binarizer> binarizer);
        virtual ~BinaryBitmap() = default;
        
        FallibleRef<BitArray > getBlackRow   (int y, Ref<BitArray> row) const MB_NOEXCEPT_EXCEPT_BADALLOC;
        FallibleRef<BitMatrix> getBlackMatrix(                        ) const MB_NOEXCEPT_EXCEPT_BADALLOC;
        
        Ref<LuminanceSource> getLuminanceSource() const;

        int getWidth() const;
        int getHeight() const;

        bool isRotateSupported() const;
        Ref<BinaryBitmap> rotateCounterClockwise();

        bool isCropSupported() const;
        Ref<BinaryBitmap> crop(int left, int top, int width, int height) MB_NOEXCEPT_EXCEPT_BADALLOC;

    };
    
}

