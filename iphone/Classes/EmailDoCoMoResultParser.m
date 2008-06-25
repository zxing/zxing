//
//  EmailDoCoMoResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "EmailDoCoMoResultParser.h"
#import "EmailParsedResult.h"

bool LooksLikeAnEmailAddress(NSString *s) {
  if ([s rangeOfString:@"@"].location == NSNotFound) {
    return false;
  }
  if ([s rangeOfString:@"."].location == NSNotFound) {
    return false;
  }
  if ([s rangeOfCharacterFromSet:[NSCharacterSet whitespaceCharacterSet]].location != NSNotFound) {
    return false;
  }
  return true;
}


@implementation EmailDoCoMoResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
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
