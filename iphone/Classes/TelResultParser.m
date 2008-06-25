//
//  TelResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "TelResultParser.h"
#import "TelParsedResult.h"

#define PREFIX @"tel:"

@implementation TelResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange telRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (telRange.location == 0) {
    int restStart = telRange.location + telRange.length;
    return [[[TelParsedResult alloc] initWithNumber:[s substringFromIndex:restStart]]
            autorelease];
  }
  return nil;
}


@end
