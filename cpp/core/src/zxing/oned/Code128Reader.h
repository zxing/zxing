// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#ifndef __CODE_128_READER_H__
#define __CODE_128_READER_H__
/*
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

#include <zxing/oned/OneDReader.h>
#include <zxing/common/BitArray.h>
#include <zxing/Result.h>

namespace zxing {
namespace oned {

class Code128Reader : public OneDReader {
private:
  static const int MAX_AVG_VARIANCE;
  static const int MAX_INDIVIDUAL_VARIANCE;

  static std::vector<int> findStartPattern(Ref<BitArray> row);
  static int decodeCode(Ref<BitArray> row,
                        std::vector<int>& counters,
                        int rowOffset);
			
public:
  Ref<Result> decodeRow(int rowNumber, Ref<BitArray> row);
  Code128Reader();
  ~Code128Reader();

  BarcodeFormat getBarcodeFormat();
};

}
}

#endif
