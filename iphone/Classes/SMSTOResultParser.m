//
//  SMSTOResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "SMSTOResultParser.h"
#import "SMSParsedResult.h"

#define PREFIX @"SMSTO:"

@implementation SMSTOResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int max = [s length];
    int restStart = prefixRange.location + prefixRange.length;
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
