//
//  SMSParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
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

#import "SMSParsedResult.h"
#import "SMSAction.h"

@implementation SMSParsedResult

@synthesize number;
@synthesize body;

- (id)initWithNumber:(NSString *)n body:(NSString *)b {
  if ((self = [super init]) != nil) {
    self.number = n;
    self.body = b;
  }
  return self;
}

- (NSString *)stringForDisplay {
  if (self.body) {
    return [NSString stringWithFormat:@"%@\n%@", self.number, self.body];
  }
  return self.number;
}


+ (NSString *)typeName {
  return NSLocalizedString(@"SMSParsedResult type name", @"SMS");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"sms.png"];
}

- (void)populateActions { 
  [actions addObject:[SMSAction actionWithNumber:self.number body:self.body]];
}

- (void) dealloc {
  [number release];
  [body release];
  [super dealloc];
}


@end
