/*
 *  Copyright 2010-2011 ZXing authors
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

#include "MagickBitmapSource.h"

#include <iostream>

using namespace Magick;

namespace zxing {

MagickBitmapSource::MagickBitmapSource(Image& image) : image_(image) {
  width = image.columns();
  height = image.rows();
  
}

MagickBitmapSource::~MagickBitmapSource() {
}

int MagickBitmapSource::getWidth() const {
  return width;
}

int MagickBitmapSource::getHeight() const {
  return height;
}

unsigned char* MagickBitmapSource::getRow(int y, unsigned char* row) {
  const Magick::PixelPacket* pixel_cache = image_.getConstPixels(0, y, width, 1);
  int width = getWidth();
  if (row == NULL) {
    row = new unsigned char[width];
  }
  for (int x = 0; x < width; x++) {
    const PixelPacket* p = pixel_cache + x;
    // We assume 16 bit values here
    // 0x200 = 1<<9, half an lsb of the result to force rounding
    row[x] = (unsigned char)((306 * ((int)p->red >> 8) + 601 * ((int)p->green >> 8) +
        117 * ((int)p->blue >> 8) + 0x200) >> 10);
  }
  return row;

}

/** This is a more efficient implementation. */
unsigned char* MagickBitmapSource::getMatrix() {
  const Magick::PixelPacket* pixel_cache = image_.getConstPixels(0, 0, width, height);
  int width = getWidth();
  int height =  getHeight();
  unsigned char* matrix = new unsigned char[width*height];
  unsigned char* m = matrix;
  const Magick::PixelPacket* p = pixel_cache;
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      *m = (unsigned char)((306 * ((int)p->red >> 8) + 601 * ((int)p->green >> 8) +
          117 * ((int)p->blue >> 8) + 0x200) >> 10);
      m++;
      p++;
    }
  }
  return matrix;
}

bool MagickBitmapSource::isRotateSupported() const {
  return true;
}

Ref<LuminanceSource> MagickBitmapSource::rotateCounterClockwise() {
  Magick::Image rotated(image_);
  rotated.modifyImage();
  rotated.rotate(90); // Image::rotate takes CCW degrees as an argument
  rotated.syncPixels();
  return Ref<MagickBitmapSource> (new MagickBitmapSource(rotated));
}

bool MagickBitmapSource::isCropSupported() const{
  return true;
}

Ref<LuminanceSource> MagickBitmapSource::crop(int left, int top, int width, int height){
  /* TODO Investigate memory leak:
   * This method "possibly leaks" 160 bytes in 1 block */
  Image copy(image_);
  copy.modifyImage();
  copy.crop( Geometry(width,height,left,top));
  copy.syncPixels();
  return Ref<MagickBitmapSource>(new MagickBitmapSource(copy));
}

}

