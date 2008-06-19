#ifndef __MONOCHROME_BITMAP_SOURCE_H__
#define __MONOCHROME_BITMAP_SOURCE_H__

/*
 *  MonochromeBitmapSource.h
 *  zxing
 *
 *  Created by Christian Brunschen on 12/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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

#include "common/Counted.h"
#include "common/Array.h"
#include "common/BitArray.h"
#include "BlackPointEstimationMethod.h"
#include <valarray>

using namespace common;

class MonochromeBitmapSource : public Counted {
protected:
  unsigned char blackPoint_;
  BlackPointEstimationMethod lastEstimationMethod_;
  int lastEstimationArgument_;
  
public:
  MonochromeBitmapSource();
  virtual ~MonochromeBitmapSource() { }
  Ref<BitArray> getBlackRow(size_t y, Ref<BitArray> row, size_t startX, 
                            size_t getWidth);
  void estimateBlackPoint(BlackPointEstimationMethod method, int arg);
  BlackPointEstimationMethod getLastEstimationMethod();
  bool isBlack(size_t x, size_t y);
  
  virtual size_t getWidth() = 0;
  virtual size_t getHeight() = 0;
  virtual unsigned char getPixel(size_t x, size_t y) = 0;
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise() = 0;
  virtual bool isRotateSupported() = 0;
};

#endif // __MONOCHROME_BITMAP_SOURCE_H__
