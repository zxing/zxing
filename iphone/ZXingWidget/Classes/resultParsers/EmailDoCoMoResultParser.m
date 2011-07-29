//
//  EmailDoCoMoResultParser.m
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

#import "EmailDoCoMoResultParser.h"
#import "EmailParsedResult.h"
#import "CBarcodeFormat.h"
#import "ArrayAndStringCategories.h"

@implementation EmailDoCoMoResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)format {
  NSRange foundRange = [s rangeOfString:@"MATMSG:"];
  if (foundRange.location == NSNotFound) {
    return nil;
  }

  NSString *to = [s fieldWithPrefix:@"TO:"];
  if (to == nil) {
    return nil;
  }

  EmailParsedResult *result = [[EmailParsedResult alloc] init];
  result.to = to;
  result.subject = [s fieldWithPrefix:@"SUB:"];
  result.body = [s fieldWithPrefix:@"BODY:"];

  return [result autorelease];
}

@end
