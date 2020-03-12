/*
 *  BinaryBitmap.cpp
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

#include <zxing/BinaryBitmap.h>

#include "zxing/Binarizer.h"         // for Binarizer
#include "zxing/LuminanceSource.h"   // for LuminanceSource
#include "zxing/common/BitArray.h"   // for BitArray
#include "zxing/common/BitMatrix.h"  // for BitMatrix
#include "zxing/common/Counted.h"    // for Ref

namespace pping {

    BinaryBitmap::BinaryBitmap(Ref<Binarizer> binarizer) : binarizer_(binarizer) {

    }

    FallibleRef<BitArray > BinaryBitmap::getBlackRow(int y, Ref<BitArray> row) const MB_NOEXCEPT_EXCEPT_BADALLOC {
        return binarizer_->getBlackRow(y, row);
    }

    FallibleRef<BitMatrix> BinaryBitmap::getBlackMatrix() const MB_NOEXCEPT_EXCEPT_BADALLOC {
        return binarizer_->getBlackMatrix();
    }

    int BinaryBitmap::getWidth() const {
        return getLuminanceSource()->getWidth();
    }

    int BinaryBitmap::getHeight() const {
        return getLuminanceSource()->getHeight();
    }

    Ref<LuminanceSource> BinaryBitmap::getLuminanceSource() const {
        return binarizer_->getLuminanceSource();
    }

    bool BinaryBitmap::isRotateSupported() const {
      return getLuminanceSource()->isRotateSupported();
    }

    Ref<BinaryBitmap> BinaryBitmap::rotateCounterClockwise() {
      return Ref<BinaryBitmap> (new BinaryBitmap(binarizer_->createBinarizer(getLuminanceSource()->rotateCounterClockwise())));
    }
}
