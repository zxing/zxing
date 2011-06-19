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

#import <ZXing/ZXReader.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/ReaderException.h>
#import <ZXing/ZXResult.h>
#import <ZXing/ZXBinaryBitmap.h>
#import <ZXing/ZXDecodeHints.h>
#import <ZXing/ZXReaderException.h>
#import <ZXing/ZXIllegalArgumentException.h>

@implementation ZXReader

- (ZXReader*)initWithReader:(zxing::Reader*)_reader {
  reader = _reader;
  return self;
}

- (ZXResult*)decode:(ZXBinaryBitmap*)bitmap hints:(ZXDecodeHints*)hints {
  try {
    zxing::Ref<zxing::Result> result (reader->decode(
                                        zxing::Ref<zxing::BinaryBitmap>([bitmap native]),
                                        *[hints native]));
    return [[[ZXResult alloc] initWithNative:result] autorelease];
  } catch (zxing::ReaderException const& re) {
    NSString* s = [NSString stringWithCString:re.what() encoding:NSUTF8StringEncoding];
    NSException* e = [[[ZXReaderException alloc] initWithName:@"ZXReaderException"
                                                      reason:s
                                                     userInfo:nil] autorelease];
    @throw e;
  } catch (zxing::IllegalArgumentException const& iae) {
    NSString* s = [NSString stringWithCString:iae.what() encoding:NSUTF8StringEncoding];
    NSException* e = [[[ZXIllegalArgumentException alloc] 
                        initWithName:@"ZXIllegalArgumntException"
                              reason:s
                            userInfo:nil] autorelease];
    @throw e;
  }
  /* not reachable */
  return nil;
}

@end
