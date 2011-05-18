// -*- mode:c++; c-basic-offset:2; indent-tabs-mode:nil -*-
#ifndef __CGIIMAGE_LUMINANCE_SOURCE__
#define __CGIIMAGE_LUMINANCE_SOURCE__
/*
 *  Copyright 2011 ZXing authors All rights reserved.
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

#if TARGET_OS_EMBEDDED || TARGET_IPHONE_SIMULATOR
#include <CoreVideo/CoreVideo.h>
#endif

#include <zxing/LuminanceSource.h>

namespace zxing {

class CGImageLuminanceSource : public LuminanceSource {

 private:
  CGImageRef image_;
  CFDataRef data_;
  int left_;
  int top_;
  int width_;
  int height_;
  int dataWidth_;
  int dataHeight_;
  int bytesPerRow_;

 public:

  static CGImageRef createImageFromBuffer(CVImageBufferRef);
  static CGImageRef createImageFromBuffer(CVImageBufferRef,
                                          int left,
                                          int top,
                                          int width,
                                          int height);

  CGImageLuminanceSource(CVPixelBufferRef buffer,
                         int left,
                         int top,
                         int width,
                         int height);
  CGImageLuminanceSource(CVPixelBufferRef buffer);

  CGImageLuminanceSource(CGImageRef image,
                         int left,
                         int top,
                         int width,
                         int height);
  CGImageLuminanceSource(CGImageRef image);
  ~CGImageLuminanceSource();

  CGImageRef image() { return image_; }
  CGImageRef image(size_t width, size_t height);

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

 private:
  
  void init(CGImageRef image);
  void init(CGImageRef image, int left, int top, int width, int height);
};

} /* namespace */

#endif
