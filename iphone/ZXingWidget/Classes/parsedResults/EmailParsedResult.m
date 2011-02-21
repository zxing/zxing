//
//  EmailDoCoMoParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 28/05/2008.
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

#import "EmailParsedResult.h"
#import "EmailAction.h"


@implementation EmailParsedResult

@synthesize to;
@synthesize subject;
@synthesize body;

+ (bool) looksLikeAnEmailAddress:(NSString *)s {
  if ([s rangeOfString:@"@"].location == NSNotFound) {
    return false;
  }
  if ([s rangeOfString:@"."].location == NSNotFound) {
    return false;
  }
  if ([s rangeOfCharacterFromSet:[NSCharacterSet whitespaceCharacterSet]].location != NSNotFound) {
    return false;
  }
  return true;
}


- (NSString *)stringForDisplay {
  NSMutableArray *parts = [[NSMutableArray alloc] initWithCapacity:10];
  [parts addObject:[NSString stringWithFormat:NSLocalizedString(@"EmailParsedResult Display: Recipient", @"To: %@"), self.to]];
  if (self.subject) {
    [parts addObject:[NSString stringWithFormat:NSLocalizedString(@"EmailParsedResult Display: Subject", @"Subject: %@"), self.subject]];
  }
  if (self.body) {
    [parts addObject:@""];
    [parts addObject:[NSString stringWithFormat:NSLocalizedString(@"EmailParsedResult Display: Body", @"%@"), self.body]];
  }
  NSString* string = [NSString stringWithString:[parts componentsJoinedByString:@"\n"]];
  [parts release];
  return string;
}

+ (NSString *)typeName {
    return NSLocalizedString(@"EmailParsedResult type name", @"Email");
}

- (NSArray *)actions {
  return [NSArray arrayWithObject:[EmailAction actionWithRecipient:self.to
                                                           subject:self.subject
                                                              body:self.body]];
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"email.png"];
}


@end
