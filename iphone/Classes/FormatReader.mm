//
//  FormatReader.mm
//
//  Created by Dave MacLachlan on 2010-05-03.
/*
 * Copyright 2010 ZXing authors
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

#import "FormatReader.h"

@implementation FormatReader

static NSMutableSet *sFormatReaders = nil;

+ (void)registerFormatReader:(FormatReader*)formatReader {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  @synchronized(self) {
    if (!sFormatReaders) {
      sFormatReaders = [[NSMutableSet alloc] init];
    }
    [sFormatReaders addObject:formatReader];
  }
  [pool drain];
}

+ (NSSet *)formatReaders {
  NSSet *formatReaders = nil;
  @synchronized(self) {
    formatReaders = [[sFormatReaders copy] autorelease];
  }
  return formatReaders;
}

- (id)initWithReader:(zxing::Reader *)reader {
  if ((self = [super init])) {
    reader_ = reader;
  }
  return self;
}

- (void)dealloc {
  delete reader_;
  [super dealloc];
}

- (zxing::Ref<zxing::Result>)decode:(zxing::Ref<zxing::BinaryBitmap>)grayImage {
  return reader_->decode(grayImage);
}

@end
