//
//  SMSResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "SMSResultParser.h"
#import "SMSParsedResult.h"

#define PREFIX @"sms:"

@implementation SMSResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int max = [s length];
    int restStart = prefixRange.location + prefixRange.length;
    
    // initial presuption: everything after the prefix is the number, and there is no body
    NSRange numberRange = NSMakeRange(restStart, max - restStart);
    NSRange bodyRange = NSMakeRange(NSNotFound, 0);

    // is there a query string?
    NSRange queryRange = [s rangeOfString:@"?" options:0 range:numberRange];
    if (queryRange.location != NSNotFound) {
      // truncate the number range at the beginning of the query string
      numberRange.length = queryRange.location - numberRange.location;
      
      int paramsStart = queryRange.location + queryRange.length;
      NSRange paramsRange = NSMakeRange(paramsStart, max - paramsStart);
      NSRange bodyPrefixRange = [s rangeOfString:@"body=" options:0 range:paramsRange];
      if (bodyPrefixRange.location != NSNotFound) {
        int bodyStart = bodyPrefixRange.location + bodyPrefixRange.length;
        bodyRange = NSMakeRange(bodyStart, max - bodyStart);
        NSRange ampRange = [s rangeOfString:@"&" options:0 range:bodyRange];
        if (ampRange.location != NSNotFound) {
          // we found a '&', so we truncate the body range there
          bodyRange.length = ampRange.location - bodyRange.location;
        }
      }
    } 
    
    // if there's a semicolon in the number, truncate the number there
    NSRange semicolonRange = [s rangeOfString:@";" options:0 range:numberRange];
    if (semicolonRange.location != NSNotFound) {
      numberRange.length = semicolonRange.location - numberRange.location;
    }
    
    NSString *number = [s substringWithRange:numberRange];
    NSString *body = bodyRange.location != NSNotFound ? [s substringWithRange:bodyRange] : nil;
    return [[[SMSParsedResult alloc] initWithNumber:number body:body]
          autorelease];
  }
  return nil;
}


@end
