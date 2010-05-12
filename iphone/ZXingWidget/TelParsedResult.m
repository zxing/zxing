//
//  TelParsedResult.m
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

#import "TelParsedResult.h"
#import "CallAction.h"

@implementation TelParsedResult

@synthesize number;

- (id)initWithNumber:(NSString *)n {
  if ((self = [super init]) != nil) {
    self.number = n;
  }
  return self;
}

- (NSString *)stringForDisplay {
  return self.number;
}


+ (NSString *)typeName {
  return NSLocalizedString(@"TelParsedResult type name", @"Tel");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"phone.png"];
}

- (void)populateActions { 
  [actions addObject:[CallAction actionWithNumber:self.number]];
}

- (void) dealloc {
  [number release];
  [super dealloc];
}

@end
