//
//  MeCardParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "MeCardParser.h"
#import "BusinessCardParsedResult.h"

@implementation MeCardParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange foundRange = [s rangeOfString:@"MECARD:"];
  if (foundRange.location == NSNotFound) {
    return nil;
  }
  
  NSString *name = [s fieldWithPrefix:@"N:"];
  if (name == nil) {
    return nil;
  }
  
  BusinessCardParsedResult *result = [[BusinessCardParsedResult alloc] init];
  result.name = name;
  result.phoneNumbers = [s fieldsWithPrefix:@"TEL:"];
  result.email = [s fieldWithPrefix:@"EMAIL:"];
  result.note = [s fieldWithPrefix:@"NOTE:"];
  result.urlString = [s fieldWithPrefix:@"URL:"];
  result.address = [s fieldWithPrefix:@"ADR:"];
  
  return [result autorelease];
}


@end
