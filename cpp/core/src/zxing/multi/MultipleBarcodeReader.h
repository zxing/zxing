#pragma once

/*
 *  Copyright 2011 ZXing authors All rights reserved.
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

#include <zxing/DecodeHints.h>     // for DecodeHints
#include <zxing/common/Counted.h>  // for Counted
#include <zxing/common/Error.hpp>

#include <vector>                  // for vector

namespace pping {
class BinaryBitmap;
class Result;
}  // namespace pping

namespace pping {
namespace multi {
class MultipleBarcodeReader : public Counted {
  protected:
    MultipleBarcodeReader() {}
  public:
    virtual Fallible<std::vector<Ref<Result>>> decodeMultiple(Ref<BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC;
    virtual Fallible<std::vector<Ref<Result>>> decodeMultiple(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC = 0;
    virtual ~MultipleBarcodeReader();
};
} // End zxing::multi namespace
} // End zxing namespace

