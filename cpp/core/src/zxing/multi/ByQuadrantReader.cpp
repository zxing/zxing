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

#include <zxing/ReaderException.h>  // for ReaderException
#include <zxing/multi/ByQuadrantReader.h>

#include "zxing/BinaryBitmap.h"     // for BinaryBitmap
#include "zxing/DecodeHints.h"      // for DecodeHints, DecodeHints::DEFAULT_HINT
#include "zxing/Reader.h"           // for Reader
#include "zxing/Result.h"           // for Result

namespace pping {
namespace multi {

FallibleRef<Result> ByQuadrantReader::decode(Ref<BinaryBitmap> image) MB_NOEXCEPT_EXCEPT_BADALLOC {
  return decode(image, DecodeHints::DEFAULT_HINT);
}

FallibleRef<Result> ByQuadrantReader::decode(Ref<BinaryBitmap> image, DecodeHints hints) MB_NOEXCEPT_EXCEPT_BADALLOC {
  int width = image->getWidth();
  int height = image->getHeight();
  int halfWidth = width / 2;
  int halfHeight = height / 2;
  {
    Ref<BinaryBitmap> topLeft = image->crop(0, 0, halfWidth, halfHeight);
    auto result(delegate_.decode(topLeft, hints));
    if (result && !(*result).empty())
      return result;
  }
  {
    Ref<BinaryBitmap> topRight = image->crop(halfWidth, 0, halfWidth, halfHeight);
    auto result(delegate_.decode(topRight, hints));
    if (result && !(*result).empty())
      return result;
  }
  {
    Ref<BinaryBitmap> bottomLeft = image->crop(0, halfHeight, halfWidth, halfHeight);
    auto result(delegate_.decode(bottomLeft, hints));
    if (result && !(*result).empty())
      return result;
  }
  {
    Ref<BinaryBitmap> bottomRight = image->crop(halfWidth, halfHeight, halfWidth, halfHeight);
    auto result(delegate_.decode(bottomRight, hints));
    if (result && !(*result).empty())
        return result;
  }

  int quarterWidth = halfWidth / 2;
  int quarterHeight = halfHeight / 2;
  Ref<BinaryBitmap> center = image->crop(quarterWidth, quarterHeight, halfWidth, halfHeight);
  return delegate_.decode(center, hints);
}

} // End zxing::multi namespace
} // End zxing namespace
