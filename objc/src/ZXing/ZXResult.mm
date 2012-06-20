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

#import <ZXing/ZXResult.h>
#include <zxing/result.h>

@interface ZXResultInternal : NSObject {
  zxing::Ref<zxing::Result> native;
}
- (id)init:(void*)native;
- (zxing::Ref<zxing::Result> const&) native;
@end
@implementation ZXResultInternal
- (id)init:(void*)native_ {
  if ((self = [super init])) {
    native = (zxing::Result*)native_;
  }
  return self;
}
- (zxing::Ref<zxing::Result> const&)native {
  return native;
}
@end

@implementation ZXResult

- (ZXResult*)initWithNative:(void*)native {
  if ((self = [super init])) {
    state_ = [[ZXResultInternal alloc] init:native];
  }
  return self;
}

- (void)dealloc {
  [state_ release];
  [super dealloc];
}

- (NSString*)text {
  return [NSString 
           stringWithCString:[state_ native]->getText()->getText().c_str()
                    encoding:NSUTF8StringEncoding];
}

@end
