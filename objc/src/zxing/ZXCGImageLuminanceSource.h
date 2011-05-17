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

#import <zxing/ZXLuminanceSource.h>
#import <zxing/CGImageLuminanceSource.h>

@class ZXImage;

@interface ZXCGImageLuminanceSource : ZXLuminanceSource {
}

+ (CGImageRef)createImageFromBuffer:(CVImageBufferRef)buffer;
+ (CGImageRef)createImageFromBuffer:(CVImageBufferRef)buffer
                               left:(size_t)left
                                top:(size_t)top
                              width:(size_t)width
                             height:(size_t)height;

- (ZXCGImageLuminanceSource*)initWithZXImage:(ZXImage*)image
                                      left:(size_t)left
                                       top:(size_t)top
                                     width:(size_t)width
                                    height:(size_t)height;

- (ZXCGImageLuminanceSource*)initWithZXImage:(ZXImage*)image;

- (ZXCGImageLuminanceSource*)initWithCGImage:(CGImageRef)image
                                      left:(size_t)left
                                       top:(size_t)top
                                     width:(size_t)width
                                    height:(size_t)height;

- (ZXCGImageLuminanceSource*)initWithCGImage:(CGImageRef)image;

- (ZXCGImageLuminanceSource*)initWithBuffer:(CVPixelBufferRef)buffer
                                      left:(size_t)left
                                       top:(size_t)top
                                     width:(size_t)width
                                    height:(size_t)height;

- (ZXCGImageLuminanceSource*)initWithBuffer:(CVPixelBufferRef)buffer;

- (CGImageRef)image;

@end
