//
//  BookmarkDoCoMoResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "BookmarkDoCoMoResultParser.h"
#import "URIParsedResult.h"

@implementation BookmarkDoCoMoResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange foundRange = [s rangeOfString:@"MEBKM:"];
  if (foundRange.location == NSNotFound) {
    return nil;
  }
  
  NSString *urlString = [s fieldWithPrefix:@"URL:"];
  if (urlString == nil) {
    return nil;
  }
  
  NSString *title = [s fieldWithPrefix:@"TITLE:"];
  
  return [[[URIParsedResult alloc] initWithURLString:urlString 
                                               title:title] autorelease];
}


@end
