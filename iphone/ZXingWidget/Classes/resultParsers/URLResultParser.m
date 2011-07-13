//
//  URIResultParser.m
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

#import "URLResultParser.h"
#import "URIParsedResult.h"
#import "CBarcodeFormat.h"

@implementation NSString (ZXingURLExtensions)

- (bool)looksLikeAURI {
  if ([self rangeOfCharacterFromSet:[NSCharacterSet whitespaceCharacterSet]].location != NSNotFound) {
    return false;
  }
  if ([self rangeOfString:@":"].location == NSNotFound) {
    return false;
  }
  return true;
}

- (NSString *)massagedURLString {
  NSRange colonRange = [self rangeOfString:@":"];
  if (colonRange.location == NSNotFound) {
    return [NSString stringWithFormat:@"http://%@", self];
  } else {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSString *part1 = [[self substringToIndex:colonRange.location] lowercaseString];
    NSString *part2 = [self substringFromIndex:colonRange.location];
    NSString *result = [[NSString alloc] initWithFormat:@"%@%@", part1,part2];
    [pool release];
    return [result autorelease];
  }
}

@end


#define PREFIX @"URL:"

@implementation URLResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)format {
  
  NSAutoreleasePool *myPool = [[NSAutoreleasePool alloc] init];
  ParsedResult *result = nil;
  
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int restStart = /*prefixRange.location + */ prefixRange.length;
    result = [[URIParsedResult alloc] initWithURLString:[[s substringFromIndex:restStart] massagedURLString]]; 
//    return [[[URIParsedResult alloc] initWithURLString:[[s substringFromIndex:restStart] massagedURLString]]
//            autorelease];
  } else if ([s looksLikeAURI]) {
    NSString *massaged = [s massagedURLString];
    NSURL *url = [[NSURL alloc] initWithString:massaged];
    if (url != nil) {
      result = [[URIParsedResult alloc] initWithURLString:massaged URL:url];
    }
    [url release];
  }
  [myPool release];
  return [result autorelease];
}


@end
