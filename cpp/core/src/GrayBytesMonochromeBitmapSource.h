#ifndef __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__
#define __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__

/*
 *  GrayBytesMonochromeBitmapSource.h
 *  ZXing
 *
 *  Created by Christian Brunschen on 03/06/2008.
 *  Copyright 2008 Google Inc. All rights reserved.
 *
 */

#include "MonochromeBitmapSource.h"

class GrayBytesMonochromeBitmapSource : public MonochromeBitmapSource {
private:
  size_t width_;
  size_t height_;
  const unsigned char *bytes_;
  size_t bytesPerRow_;

protected:
  virtual unsigned char getPixel(size_t x, size_t y);

public:
  GrayBytesMonochromeBitmapSource(const unsigned char *bytes, 
                                  size_t width, 
                                  size_t height,
                                  size_t bytesPerRow);
  virtual ~GrayBytesMonochromeBitmapSource() { }
  
  virtual Ref<MonochromeBitmapSource> rotateCounterClockwise();
  virtual bool isRotateSupported();
  virtual size_t getWidth();
  virtual size_t getHeight();
  
};

#endif // __GRAY_BYTES_MONOCHROM_BITMAP_SOURCE_H__
