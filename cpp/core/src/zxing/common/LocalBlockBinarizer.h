/*
 *  LocalBlockBinarizer.h
 *  zxing
 *
 *  Created by Ralf Kistner on 17/10/2009.
 *  Copyright 2008 ZXing authors All rights reserved.
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

#ifndef LOCALBLOCKBINARIZER_H_
#define LOCALBLOCKBINARIZER_H_

#include <zxing/Binarizer.h>
#include <zxing/common/BitMatrix.h>

namespace zxing {
class LocalBlockBinarizer : public Binarizer {
public:
  LocalBlockBinarizer(Ref<LuminanceSource> source);
  virtual ~LocalBlockBinarizer();

  virtual Ref<BitMatrix> estimateBlackMatrix();
  Ref<BitArray> estimateBlackRow(int y, Ref<BitArray> row);
  
private:
  
  void calculateThresholdForBlock(const unsigned char* luminances, int subWidth, int subHeight,
                                  int stride, const unsigned char* averages, const unsigned char* types, BitMatrix& matrix);
  void sharpenRow(unsigned char* luminances, int width, int height);
  void calculateBlackPoints(const unsigned char* luminances, unsigned char* averages, unsigned char* types, int subWidth, int subHeight, int stride);
  void threshold8x8Block(const unsigned char* luminances, int xoffset, int yoffset, int threshold,
                         int stride, BitMatrix& matrix);
};
}

#endif /* LOCALBLOCKBINARIZER_H_ */
