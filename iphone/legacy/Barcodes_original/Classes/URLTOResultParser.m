//
//  URLTOResultParser.m
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

#import "URLTOResultParser.h"
#import "URIParsedResult.h"

#define PREFIX @"URLTO:"

@implementation URLTOResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int max = [s length];
    int titleStart = prefixRange.location + prefixRange.length;
    NSRange searchRange = NSMakeRange(titleStart, max - titleStart);
    NSRange colonRange = [s rangeOfString:@":" options:0 range:searchRange];
    if (colonRange.location != NSNotFound) {
      NSRange titleRange = NSMakeRange(titleStart,
                                       colonRange.location - titleStart);
      int linkStart = colonRange.location + colonRange.length;
      NSRange linkRange = NSMakeRange(linkStart, max - linkStart);
      return [[[URIParsedResult alloc] initWithURLString:[s substringWithRange:linkRange]
                                                   title:[s substringWithRange:titleRange]]
              autorelease];
    }
  }
  return nil;
}


@end
