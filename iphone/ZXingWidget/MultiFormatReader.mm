//
//  MultiFormatReader.mm
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
#import <zxing/MultiFormatReader.h>

@interface MultiFormatReader : FormatReader
@end

@implementation MultiFormatReader

+ (void)load {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  [FormatReader registerFormatReader:[[[self alloc] init] autorelease]];
  [pool drain];
}

- (id)init {
  zxing::MultiFormatReader *reader = new zxing::MultiFormatReader();
  return [super initWithReader:reader];
}

@end
