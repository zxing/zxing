/*
 *  GreyscaleLuminanceSource.cpp
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

#include <zxing/common/GreyscaleLuminanceSource.h>
#include <zxing/common/GreyscaleRotatedLuminanceSource.h>
#include <zxing/common/IllegalArgumentException.h>

namespace zxing {

GreyscaleLuminanceSource::GreyscaleLuminanceSource(unsigned char* greyData, int dataWidth,
    int dataHeight, int left, int top, int width, int height) : greyData_(greyData),
    dataWidth_(dataWidth), dataHeight_(dataHeight), left_(left), top_(top), width_(width),
    height_(height) {

  if (left + width > dataWidth || top + height > dataHeight || top < 0 || left < 0) {
    throw IllegalArgumentException("Crop rectangle does not fit within image data.");
  }
}

unsigned char* GreyscaleLuminanceSource::getRow(int y, unsigned char* row) {
  if (y < 0 || y >= this->getHeight()) {
    throw IllegalArgumentException("Requested row is outside the image.");
  }
  int width = getWidth();
  // TODO(flyashi): determine if row has enough size.
  if (row == NULL) {
    row = new unsigned char[width_];
  }
  int offset = (y + top_) * dataWidth_ + left_;
  memcpy(row, &greyData_[offset], width);
  return row;
}

unsigned char* GreyscaleLuminanceSource::getMatrix() {
  int size = width_ * height_;
  unsigned char* result = new unsigned char[size];
  if (left_ == 0 && top_ == 0 && dataWidth_ == width_ && dataHeight_ == height_) {
    memcpy(result, greyData_, size);
  } else {
    for (int row = 0; row < height_; row++) {
      memcpy(result + row * width_, greyData_ + (top_ + row) * dataWidth_ + left_, width_);
    }
  }
  return result;
}

Ref<LuminanceSource> GreyscaleLuminanceSource::rotateCounterClockwise() {
  // Intentionally flip the left, top, width, and height arguments as needed. dataWidth and
  // dataHeight are always kept unrotated.
  return Ref<LuminanceSource> (new GreyscaleRotatedLuminanceSource(greyData_, dataWidth_,
      dataHeight_, top_, left_, height_, width_));
}

} /* namespace */
