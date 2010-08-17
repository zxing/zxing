//
//  FormatReader.h
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

#import <Foundation/Foundation.h>
#import <zxing/common/Counted.h>
#import <zxing/Result.h>
#import <zxing/BinaryBitmap.h>
#import <zxing/Reader.h>
#import <zxing/ResultPointCallback.h>

@interface FormatReader : NSObject {
  zxing::Reader *reader_;
}

+ (void)registerFormatReader:(FormatReader *)formatReader;
//+ (NSSet *)formatReaders;

- (id)initWithReader:(zxing::Reader *)reader;
- (zxing::Ref<zxing::Result>)decode:(zxing::Ref<zxing::BinaryBitmap>)grayImage;
- (zxing::Ref<zxing::Result>)decode:(zxing::Ref<zxing::BinaryBitmap>)grayImage andCallback:(zxing::Ref<zxing::ResultPointCallback>)callback;

@end
