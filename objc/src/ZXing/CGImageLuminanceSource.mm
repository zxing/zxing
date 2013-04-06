// -*- mode:objc; c-basic-offset:2; indent-tabs-mode:nil -*-
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

#import <ZXing/ZXCGImageLuminanceSource.h>
#include <sstream>
#include <zxing/common/IllegalArgumentException.h>

using std::string;
using std::ostringstream;
using zxing::CGImageLuminanceSource;
using zxing::ArrayRef;
using zxing::Ref;
using zxing::LuminanceSource;

CGImageRef CGImageLuminanceSource::createImageFromBuffer
  (CVPixelBufferRef buffer, int left, int top, int width, int height)
{
  int bytesPerRow = (int)CVPixelBufferGetBytesPerRow(buffer); 
  int dataWidth = (int)CVPixelBufferGetWidth(buffer); 
  int dataHeight = (int)CVPixelBufferGetHeight(buffer); 
    
  if (left + width > dataWidth ||
      top + height > dataHeight || 
      top < 0 ||
      left < 0) {
    throw
      IllegalArgumentException("Crop rectangle does not fit within image data.");
  }

  int newBytesPerRow = ((width*4+0xf)>>4)<<4;

  CVPixelBufferLockBaseAddress(buffer,0); 

  unsigned char* baseAddress =
    (unsigned char*)CVPixelBufferGetBaseAddress(buffer); 

  int size = newBytesPerRow*height;
  unsigned char* bytes = (unsigned char*)malloc(size);
  if (newBytesPerRow == bytesPerRow) {
    memcpy(bytes, baseAddress+top*bytesPerRow, size);
  } else {
    for(int y=0; y<height; y++) {
      memcpy(bytes+y*newBytesPerRow,
             baseAddress+left*4+(top+y)*bytesPerRow,
             newBytesPerRow);
    }
  }
  CVPixelBufferUnlockBaseAddress(buffer, 0);

  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB(); 
  CGContextRef newContext = CGBitmapContextCreate(bytes,
                                                  width,
                                                  height,
                                                  8,
                                                  newBytesPerRow,
                                                  colorSpace,
                                                  kCGBitmapByteOrder32Little|
                                                  kCGImageAlphaNoneSkipFirst);
  CGColorSpaceRelease(colorSpace);

  CGImageRef result = CGBitmapContextCreateImage(newContext); 

  CGContextRelease(newContext); 

  free(bytes);

  return result;
}

CGImageRef CGImageLuminanceSource::createImageFromBuffer
  (CVPixelBufferRef buffer)
{
  return createImageFromBuffer
    (buffer,
     0,
     0,
     (int)CVPixelBufferGetWidth(buffer),
     (int)CVPixelBufferGetHeight(buffer));
}

CGImageLuminanceSource* CGImageLuminanceSource::create(CVPixelBufferRef buffer) {
  // Will leak if there's an exception ...
  CGImageRef image = createImageFromBuffer(buffer);
  CGImageLuminanceSource* source = new CGImageLuminanceSource(image);
  CGImageRelease(image);
  return source;
}

CGImageLuminanceSource::CGImageLuminanceSource(CVPixelBufferRef buffer,
                                               int left, int top, int width, int height)
: Super(width, height) { 
  CGImageRef image = createImageFromBuffer(buffer, left, top, width, height);
  init(image);
  CGImageRelease(image);
}

CGImageLuminanceSource::CGImageLuminanceSource(CGImageRef cgimage)
: Super((int)CGImageGetWidth(cgimage), (int)CGImageGetHeight(cgimage)) {
  init(cgimage);
}

CGImageLuminanceSource::CGImageLuminanceSource(CGImageRef cgimage,
                                               int left,
                                               int top,
                                               int width,
                                               int height)
: Super(width, height) {
  init(cgimage, left, top);
}

void
CGImageLuminanceSource::init(CGImageRef cgimage) {
  init(cgimage, 0, 0);
}

void
CGImageLuminanceSource::init(CGImageRef cgimage, int left, int top) {
  data_ = 0;
  image_ = cgimage;
  left_ = left;
  top_ = top;
  dataWidth_ = (int)CGImageGetWidth(image_);
  dataHeight_ = (int)CGImageGetHeight(image_);

  if (left_ + getWidth() > dataWidth_ ||
      top_ + getHeight() > dataHeight_ ||
      top_ < 0 ||
      left_ < 0) {
    throw IllegalArgumentException
      ("Crop rectangle does not fit within image data.");
  }

  CGColorSpaceRef space = CGImageGetColorSpace(image_);
  CGColorSpaceModel model = CGColorSpaceGetModel(space);

  if (model != kCGColorSpaceModelMonochrome ||
      CGImageGetBitsPerComponent(image_) != 8 ||
      CGImageGetBitsPerPixel(image_) != 8) {

    CGColorSpaceRef gray = CGColorSpaceCreateDeviceGray();

    CGContextRef ctx = CGBitmapContextCreate(0,
                                             getWidth(),
                                             getHeight(), 
                                             8,
                                             getWidth(),
                                             gray, 
                                             kCGImageAlphaNone);

    CGColorSpaceRelease(gray);

    if (top || left) {
      CGContextClipToRect(ctx, CGRectMake(0, 0, getWidth(), getHeight()));
    }

    CGContextDrawImage(ctx, CGRectMake(-left, -top, getWidth(), getHeight()), image_);
    
    image_ = CGBitmapContextCreateImage(ctx); 

    bytesPerRow_ = getWidth();
    top_ = 0;
    left_ = 0;
    dataWidth_ = getWidth();
    dataHeight_ = getHeight();

    CGContextRelease(ctx);
  } else {
    CGImageRetain(image_);
  }

  CGDataProviderRef provider = CGImageGetDataProvider(image_);
  data_ = CGDataProviderCopyData(provider);
}

CGImageLuminanceSource::~CGImageLuminanceSource() {
  if (image_) {
    CGImageRelease(image_);
  }
  if (data_) {
    CFRelease(data_);
  }
}

ArrayRef<char> CGImageLuminanceSource::getRow(int y, ArrayRef<char> row) const {
  if (y < 0 || y >= this->getHeight()) {
    ostringstream msg;
    msg << "Requested row is outside the image: " << y;
    throw IllegalArgumentException(msg.str().c_str());
  }
  int width = getWidth();
  // TODO(flyashi): determine if row has enough size.
  if (!row) {
    row = ArrayRef<char>(getWidth());
  }
  int offset = (y + top_) * dataWidth_ + left_;
  char* v = &row->values()[0];
  CFDataGetBytes(data_, CFRangeMake(offset, width), (unsigned char*)v);
  return row;
}

ArrayRef<char> CGImageLuminanceSource::getMatrix() const {
  int size = getWidth() * getHeight();
  ArrayRef<char> result (size);
  if (left_ == 0 &&
      top_ == 0 &&
      dataWidth_ == getWidth() &&
      dataHeight_ == getHeight()) {
    char* v = &result->values()[0];
    CFDataGetBytes(data_, CFRangeMake(0, size), (unsigned char*)v);
  } else {
    for (int row = 0; row < getHeight(); row++) {
      char* v = &result->values()[0];
      CFDataGetBytes(data_,
                     CFRangeMake((top_ + row) * dataWidth_ + left_, getWidth()),
                     (unsigned char*)v + row * getWidth());
    }
  }
  return result;
}
