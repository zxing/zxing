//
//  TextParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 23/05/2008.
/*
 * Copyright 2008 ZXing authors
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

#import "TextParsedResult.h"
#import "EmailAction.h"


@implementation TextParsedResult

@synthesize text;

- (id)initWithString:(NSString *)s {
  if ((self  = [super init]) != nil) {
    self.text = s;
  }
  return self;
}

+ (NSString *)typeName {
  return NSLocalizedString(@"TextParsedResult type name", @"Text");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"text.png"];
}

- (NSString *)stringForDisplay {
  return self.text;
}

- (void) populateActions {
  //[actions addObject:[EmailAction actionWithRecipient:@"recipient@domain" subject:@"QR Code Contents" body:text]];
}

- (void)dealloc {
  [text release];
  [super dealloc];
}

@end
