/*
 *  Binarizer.h
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
 *  Modified by Lukasz Warchol on 02/02/2010.
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

#ifndef BINARIZER_H_
#define BINARIZER_H_

#include <zxing/LuminanceSource.h>
#include <zxing/common/BitArray.h>
#include <zxing/common/BitMatrix.h>
#include <zxing/common/Counted.h>

namespace zxing {

class Binarizer : public Counted {
 private:
  Ref<LuminanceSource> source_;
  Ref<BitArray> array_;
  Ref<BitMatrix> matrix_;
  int cached_y_;

 public:
  Binarizer(Ref<LuminanceSource> source);
  virtual ~Binarizer();

  virtual Ref<BitArray> estimateBlackRow(int y, Ref<BitArray> row)=0;
  Ref<BitArray> getBlackRow(int y, Ref<BitArray> row);

  virtual Ref<BitMatrix> estimateBlackMatrix() = 0;
  Ref<BitMatrix> getBlackMatrix();
  Ref<LuminanceSource> getSource();
};

}
#endif /* BINARIZER_H_ */
