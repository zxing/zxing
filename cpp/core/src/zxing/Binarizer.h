#ifndef BINARIZER_H_
#define BINARIZER_H_

/*
 *  Binarizer.h
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

#include <zxing/LuminanceSource.h>
#include <zxing/common/BitArray.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/Counted.h>

namespace zxing {

class Binarizer : public Counted {
 private:
  Ref<LuminanceSource> source_;

 public:
  Binarizer(Ref<LuminanceSource> source);
  virtual ~Binarizer();

  virtual Ref<BitArray> getBlackRow(int y, Ref<BitArray> row) = 0;
  virtual Ref<BitMatrix> getBlackMatrix() = 0;

  Ref<LuminanceSource> getLuminanceSource() const ;
  virtual Ref<Binarizer> createBinarizer(Ref<LuminanceSource> source) = 0;
};

}
#endif /* BINARIZER_H_ */
