/*
 *  GreyscaleRotatedLuminanceSource.cpp
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


#include <zxing/common/GreyscaleRotatedLuminanceSource.h>
#include <zxing/common/IllegalArgumentException.h>

namespace zxing {

// Note that dataWidth and dataHeight are not reversed, as we need to be able to traverse the
// greyData correctly, which does not get rotated.
GreyscaleRotatedLuminanceSource::GreyscaleRotatedLuminanceSource(unsigned char* greyData,
    int dataWidth, int dataHeight, int left, int top, int width, int height) : greyData_(greyData),
    dataWidth_(dataWidth), dataHeight_(dataHeight), left_(left), top_(top), width_(width),
    height_(height) {

  // Intentionally comparing to the opposite dimension since we're rotated.
  if (left + width > dataHeight || top + height > dataWidth) {
    throw IllegalArgumentException("Crop rectangle does not fit within image data.");
  }
}

// The API asks for rows, but we're rotated, so we return columns.
unsigned char* GreyscaleRotatedLuminanceSource::getRow(int y, unsigned char* row) {
  if (y < 0 || y >= getHeight()) {
    throw IllegalArgumentException("Requested row is outside the image: " + y);
  }
  int width = getWidth();
  if (row == NULL) {
    row = new unsigned char[width];
  }
  int offset = (left_ * dataWidth_) + (dataWidth_ - (y + top_));
  for (int x = 0; x < width; x++) {
    row[x] = greyData_[offset];
    offset += dataWidth_;
  }
  return row;
}

unsigned char* GreyscaleRotatedLuminanceSource::getMatrix() {
  unsigned char* result = new unsigned char[width_ * height_];
  // This depends on getRow() honoring its second parameter.
  for (int y = 0; y < height_; y++) {
    getRow(y, &result[y * width_]);
  }
  return result;
}

} // namespace
