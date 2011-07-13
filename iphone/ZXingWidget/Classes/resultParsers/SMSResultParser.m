//
//  SMSResultParser.m
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

#import "SMSResultParser.h"
#import "SMSParsedResult.h"
#import "CBarcodeFormat.h"

#define PREFIX @"sms:"

@implementation SMSResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)format {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int max = [s length];
    int restStart = /*prefixRange.location + */ prefixRange.length;
    
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
