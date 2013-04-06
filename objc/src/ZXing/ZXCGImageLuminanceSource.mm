// -*- mode:objc; c-basic-offset:2; indent-tabs-mode:nil -*-
/*
 * Copyright 2011 ZXing authors
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
#import <ZXing/ZXImage.h>

@implementation ZXCGImageLuminanceSource

+ (CGImageRef)createImageFromBuffer:(CVImageBufferRef)buffer {
  return zxing::CGImageLuminanceSource::createImageFromBuffer(buffer);
}

+ (CGImageRef)createImageFromBuffer:(CVImageBufferRef)buffer
                                      left:(size_t)left
                                       top:(size_t)top
                                     width:(size_t)width
                                    height:(size_t)height {
  return zxing::CGImageLuminanceSource::createImageFromBuffer(
    buffer, (int)left, (int)top, (int)width, (int)height
    );
}

- (id)initWithZXImage:(ZXImage*)image 
                 left:(size_t)left
                  top:(size_t)top
                width:(size_t)width
               height:(size_t)height {
  self = [super initWithNative:new zxing::CGImageLuminanceSource(
      image.cgimage, (int)left, (int)top, (int)width, (int)height
      )];
  return self;
}

- (id)initWithZXImage:(ZXImage*)image {
  self = [super initWithNative:new zxing::CGImageLuminanceSource(image.cgimage)];
  return self;
}

- (id)initWithCGImage:(CGImageRef)image 
                 left:(size_t)left
                  top:(size_t)top
                width:(size_t)width
               height:(size_t)height {
  self = [super initWithNative:new zxing::CGImageLuminanceSource(
      image, (int)left, (int)top, (int)width, (int)height
      )];
  return self;
}

- (id)initWithCGImage:(CGImageRef)image {
  self = [super initWithNative:new zxing::CGImageLuminanceSource(image)];
  return self;
}

- (id)initWithBuffer:(CVPixelBufferRef)buffer
                left:(size_t)left
                 top:(size_t)top
               width:(size_t)width
              height:(size_t)height {
  self = [super initWithNative:new zxing::CGImageLuminanceSource(
      buffer, (int)left, (int)top, (int)width, (int)height
      )];
  return self;
}

- (id)initWithBuffer:(CVPixelBufferRef)buffer {
  self = [super initWithNative:zxing::CGImageLuminanceSource::create(buffer)];
  return self;
}

- (CGImageRef)image {
  zxing::LuminanceSource* source = native;
  zxing::CGImageLuminanceSource* typed = (zxing::CGImageLuminanceSource*)source;
  return typed->image();
}

@end
