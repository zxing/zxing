// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
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

using zxing::Ref;
using zxing::ArrayRef;
using zxing::LuminanceSource;
using zxing::GreyscaleLuminanceSource;

GreyscaleLuminanceSource::
GreyscaleLuminanceSource(ArrayRef<char> greyData,
                         int dataWidth, int dataHeight,
                         int left, int top,
                         int width, int height) 
  : greyData_(greyData),
    dataWidth_(dataWidth), dataHeight_(dataHeight),
    left_(left), top_(top),
    width_(width), height_(height) {
    
  if (left + width > dataWidth || top + height > dataHeight || top < 0 || left < 0) {
    throw IllegalArgumentException("Crop rectangle does not fit within image data.");
  }
}

ArrayRef<char> GreyscaleLuminanceSource::getRow(int y, ArrayRef<char> row) {
  if (y < 0 || y >= this->getHeight()) {
    throw IllegalArgumentException("Requested row is outside the image.");
  }
  int width = getWidth();
  if (!row || row.size() < width) {
    ArrayRef<char> temp (width);
    row = temp;
  }
  int offset = (y + top_) * dataWidth_ + left_;
  memcpy(&row[0], &greyData_[offset], width);
  return row;
}

ArrayRef<char> GreyscaleLuminanceSource::getMatrix() {
  int size = width_ * height_;
  ArrayRef<char> result (size);
  if (left_ == 0 && top_ == 0 && dataWidth_ == width_ && dataHeight_ == height_) {
    memcpy(&result[0], &greyData_[0], size);
  } else {
    for (int row = 0; row < height_; row++) {
      memcpy(&result[row * width_], &greyData_[(top_ + row) * dataWidth_ + left_], width_);
    }
  }
  return result;
}

        extern "C" void abort();

Ref<LuminanceSource> GreyscaleLuminanceSource::rotateCounterClockwise() {
  // Intentionally flip the left, top, width, and height arguments as
  // needed. dataWidth and dataHeight are always kept unrotated.
  Ref<LuminanceSource> result ( 
    new GreyscaleRotatedLuminanceSource(greyData_,
                                        dataWidth_, dataHeight_,
                                        top_, left_, height_, width_)
    );

  /*
  // testing code ... remove when we trust everything.
  // NB: very slow, so don't forget and leave on.
  for(int r = top_; r < top_+height_; ++r) {
    for(int c = left_; c < left_+width_; ++c) {
      int rot_r = r;
      int rot_c = c;
      // transpose
      int tmp = rot_r;
      rot_r = rot_c;
      rot_c = tmp;
      // flip vert
      rot_r = width_-rot_r-1;
      if (getRow(r, ArrayRef<char>())[c] !=
          result->getRow(rot_r, ArrayRef<char>())[rot_c]) {
        using namespace std;
        cerr << r << "," << c << " "
             << rot_r << "," << rot_c << " "
             << (0+getRow(r, ArrayRef<char>())[c]) << " " 
             << (0+result->getRow(rot_r, ArrayRef<char>())[rot_c]) << " " 
             << endl;
        abort();
      }
    }
  }
  */
  
  return result;
}
