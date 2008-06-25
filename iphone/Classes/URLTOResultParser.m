//
//  URLTOResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "URLTOResultParser.h"
#import "URIParsedResult.h"

#define PREFIX @"URLTO:"

@implementation URLTOResultParser

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
