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

#import "EmailDoCoMoParsedResult.h"
#import "EmailAction.h"

bool LooksLikeAnEmailAddress(NSString *s) {
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

@implementation EmailDoCoMoParsedResult

@synthesize to;
@synthesize subject;
@synthesize body;

+ parsedResultForString:(NSString *)s {
  NSRange foundRange = [s rangeOfString:@"MATMSG:"];
  if (foundRange.location == NSNotFound) {
    return nil;
  }
  
  NSString *to = [s fieldWithPrefix:@"TO:"];
  if (to == nil) {
    return nil;
  }
  
  EmailDoCoMoParsedResult *result = [[self alloc] init];
  result.to = to;
  result.subject = [s fieldWithPrefix:@"SUB:"];
  result.body = [s fieldWithPrefix:@"BODY:"];
  
  return [result autorelease];
}

- (NSString *)stringForDisplay {
  NSMutableString *result = [NSMutableString string];
  [result appendFormat:@"To: %@", self.to];
  if (self.subject) {
    [result appendFormat:@"\nSubject: %@", self.subject];
  }
  if (self.body) {
    [result appendFormat:@"\n\n%@", self.body];
  }
  return [NSString stringWithString:result];
}

+ (NSString *)typeName {
  return @"Email";
}

- (NSArray *)actions {
  return [NSArray arrayWithObject:[EmailAction actionWithRecipient:self.to
                                                           subject:self.subject
                                                              body:self.body]];
}

@end
