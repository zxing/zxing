//
//  TextResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "TextResultParser.h"
#import "TextParsedResult.h"

@implementation TextResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  return [[[TextParsedResult alloc] initWithString:s] autorelease];
}


@end
