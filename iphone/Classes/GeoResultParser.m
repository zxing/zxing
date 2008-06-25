//
//  GeoResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "GeoResultParser.h"
#import "GeoParsedResult.h"

#define PREFIX @"geo:"

@implementation GeoResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int restStart = prefixRange.location + prefixRange.length;
    return [[[GeoParsedResult alloc] initWithLocation:[s substringFromIndex:restStart]]
            autorelease];
  }
  return nil;
}

@end
