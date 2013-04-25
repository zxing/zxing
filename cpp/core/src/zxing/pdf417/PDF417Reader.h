// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __PDF417_READER_H__
#define __PDF417_READER_H__

/*
 *  PDF417Reader.h
 *  zxing
 *
 *  Copyright 2010,2012 ZXing authors All rights reserved.
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

#include <zxing/Reader.h>
#include <zxing/pdf417/decoder/Decoder.h>
#include <zxing/DecodeHints.h>

namespace zxing {
namespace pdf417 {


class PDF417Reader : public Reader {
 private:
  decoder::Decoder decoder;
			
  static Ref<BitMatrix> extractPureBits(Ref<BitMatrix> image);
  static int moduleSize(ArrayRef<int> leftTopBlack, Ref<BitMatrix> image);
  static int findPatternStart(int x, int y, Ref<BitMatrix> image);
  static int findPatternEnd(int x, int y, Ref<BitMatrix> image);

 public:
  Ref<Result> decode(Ref<BinaryBitmap> image, DecodeHints hints);
  void reset();
};

}
}

#endif // __PDF417_READER_H__
