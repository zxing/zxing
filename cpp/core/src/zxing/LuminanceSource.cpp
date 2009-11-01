/*
 *  LuminanceSource.cpp
 *  zxing
 *
 *  Created by Ralf Kistner on 16/10/2009.
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

#include <zxing/LuminanceSource.h>

namespace zxing {

LuminanceSource::LuminanceSource() {
}

LuminanceSource::~LuminanceSource() {
}

unsigned char* LuminanceSource::copyMatrix() {
  int width = getWidth();
  int height =  getHeight();
  unsigned char* matrix = new unsigned char[width*height];
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      matrix[y*width+x] = getPixel(x, y);
    }
  }
  return matrix;
}

}
