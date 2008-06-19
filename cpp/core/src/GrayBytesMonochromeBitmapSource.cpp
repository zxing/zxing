/*
 *  GrayBytesMonochromeBitmapSource.cpp
 *  ZXing
 *
 *  Created by Christian Brunschen on 03/06/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
 *
 */

#include "GrayBytesMonochromeBitmapSource.h"
#include "ReaderException.h"
#include "TransformingMonochromeBitmapSource.h"

GrayBytesMonochromeBitmapSource::
GrayBytesMonochromeBitmapSource(const unsigned char *bytes, 
                                size_t width, 
                                size_t height,
                                size_t bytesPerRow) 
: MonochromeBitmapSource(),
width_(width), 
height_(height),
bytes_(bytes), 
bytesPerRow_(bytesPerRow) { }


size_t GrayBytesMonochromeBitmapSource::getWidth() {
  return width_;
}

size_t GrayBytesMonochromeBitmapSource::getHeight() {
  return height_;
}

unsigned char GrayBytesMonochromeBitmapSource::getPixel(size_t x, size_t y) {
  if (x >= width_ || y >= height_) {
    throw new ReaderException("bitmap coordinate out of bounds");
  }
  size_t index = y * bytesPerRow_ + x;
  return bytes_[index];
}

// create a new bitmap source with the same data but rotated counter-clockwise
Ref<MonochromeBitmapSource> 
GrayBytesMonochromeBitmapSource::rotateCounterClockwise() {
  Ref<MonochromeBitmapSource> self(this);
  Ref<MonochromeBitmapSource> result(new TMBS90(self, 1.0));
  return result;
}

bool GrayBytesMonochromeBitmapSource::isRotateSupported() {
  return true;
}
