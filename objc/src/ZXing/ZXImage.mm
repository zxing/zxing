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

#import <ZXing/ZXImage.h>
#if TARGET_OS_IPHONE || TARGET_IPHONE_SIMULATOR
#import <ImageIO/CGImageSource.h>
#endif

@implementation ZXImage

@synthesize cgimage;

- (ZXImage*)initWithURL:(NSURL const*)url {
  
  CGDataProviderRef provider = CGDataProviderCreateWithURL((CFURLRef)url);

  if (provider) {
    CGImageSourceRef source = CGImageSourceCreateWithDataProvider(provider, 0);

    if (source) {
      cgimage = CGImageSourceCreateImageAtIndex(source, 0, 0);

      CFRelease(source);
    }

    CGDataProviderRelease(provider);
  }

  if (cgimage) {
    return self;
  } else {
    return 0;
  }
}

- (size_t)width {
  return CGImageGetWidth(cgimage);
}

- (size_t)height {
  return CGImageGetHeight(cgimage);
}

- (void)dealloc {
  if (cgimage) {
    CGImageRelease(cgimage);
  }
  [super dealloc];
}

@end
