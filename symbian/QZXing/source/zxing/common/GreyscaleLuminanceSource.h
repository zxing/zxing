#ifndef __GREYSCALE_LUMINANCE_SOURCE__
#define __GREYSCALE_LUMINANCE_SOURCE__
/*
 *  GreyscaleLuminanceSource.h
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

namespace zxing {

class GreyscaleLuminanceSource : public LuminanceSource {

 private:
  unsigned char* greyData_;
  int dataWidth_;
  int dataHeight_;
  int left_;
  int top_;
  int width_;
  int height_;

 public:
  GreyscaleLuminanceSource(unsigned char* greyData, int dataWidth, int dataHeight, int left,
      int top, int width, int height);

  unsigned char* getRow(int y, unsigned char* row);
  unsigned char* getMatrix();

  bool isRotateSupported() const {
    return true;
  }

  int getWidth() const {
    return width_;
  }

  int getHeight() const {
    return height_;
  }

  Ref<LuminanceSource> rotateCounterClockwise();

};

} /* namespace */

#endif
