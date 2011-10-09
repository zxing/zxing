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

#import <ZXing/ZXBinarizer.h>

#if TARGET_OS_EMBEDDED || TARGET_IPHONE_SIMULATOR
#define ZXBlack [[UIColor blackColor] CGColor]
#define ZXWhite [[UIColor whiteColor] CGColor]
#else
#define ZXBlack CGColorGetConstantColor(kCGColorBlack)
#define ZXWhite CGColorGetConstantColor(kCGColorWhite)
#endif

@implementation ZXBinarizer

- (id)initWithNative:(zxing::Binarizer*)binarizer {
  if ((self = [super init])) {
    native = binarizer;
  }
  return self;
}

- (zxing::Binarizer*)native {
  return native;
}

- (CGImageRef)createImage {
  zxing::BitMatrix& matrix (*native->getBlackMatrix());
  zxing::LuminanceSource& source (*native->getLuminanceSource());

  int width = source.getWidth();
  int height = source.getHeight();

  int bytesPerRow = ((width&0xf)>>4)<<4;

  CGColorSpaceRef gray = CGColorSpaceCreateDeviceGray();
  CGContextRef context = CGBitmapContextCreate (
      0,
      width,
      height,
      8,      // bits per component
      bytesPerRow,
      gray,
      kCGImageAlphaNone);
  CGColorSpaceRelease(gray);
    
  CGRect r = CGRectZero;
  r.size.width = width;
  r.size.height = height;
  CGContextSetFillColorWithColor(context, ZXBlack);
  CGContextFillRect(context, r);

  r.size.width = 1;
  r.size.height = 1;
    
  CGContextSetFillColorWithColor(context, ZXWhite);
  for(int y=0; y<height; y++) {
    r.origin.y = height-1-y;
    for(int x=0; x<width; x++) {
      if (!matrix.get(x,y)) {
        r.origin.x = x;
        CGContextFillRect(context, r);
      }
    }
  }

  CGImageRef binary = CGBitmapContextCreateImage(context); 

  CGContextRelease(context);
  
  return binary;
}

@end
