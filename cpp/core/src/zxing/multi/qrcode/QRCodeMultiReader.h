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

#include <zxing/multi/MultipleBarcodeReader.h>  // for MultipleBarcodeReader
#include <zxing/qrcode/QRCodeReader.h>          // for QRCodeReader
#include <vector>                               // for vector

#include "zxing/DecodeHints.h"                  // for DecodeHints
#include "zxing/common/Error.hpp"

namespace pping {
class BinaryBitmap;
class Result;
template <typename T> class Ref;
}  // namespace pping

namespace pping {
namespace multi {
class QRCodeMultiReader: public pping::qrcode::QRCodeReader, public MultipleBarcodeReader {
  public:
    QRCodeMultiReader();
    virtual ~QRCodeMultiReader();
    virtual Fallible<std::vector<Ref<Result>>> decodeMultiple(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC override;
};
} // End zxing::multi namespace
} // End zxing namespace

