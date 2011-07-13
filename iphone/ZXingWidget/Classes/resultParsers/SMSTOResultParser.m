//
//  SMSTOResultParser.m
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

#import "SMSTOResultParser.h"
#import "SMSParsedResult.h"
#import "CBarcodeFormat.h"

#define PREFIX @"SMSTO:"

@implementation SMSTOResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)format {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int max = [s length];
    int restStart = /*prefixRange.location + */ prefixRange.length;
    NSRange searchRange = NSMakeRange(restStart, max - restStart);
    NSRange colonRange = [s rangeOfString:@":" options:0 range:searchRange];
    if (colonRange.location != NSNotFound) {
      NSRange numberRange = NSMakeRange(restStart,
                                        colonRange.location - restStart);
      int bodyStart = colonRange.location + colonRange.length;
      NSRange bodyRange = NSMakeRange(bodyStart, max - bodyStart);
      return [[[SMSParsedResult alloc] initWithNumber:[s substringWithRange:numberRange]
                                                 body:[s substringWithRange:bodyRange]]
              autorelease];
    } else {
      return [[[SMSParsedResult alloc] initWithNumber:[s substringFromIndex:restStart] 
                                                 body:nil]
                autorelease];
    }
  }
  return nil;
}

@end
