//
//  ResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "ResultParser.h"

#import "MeCardParser.h"
#import "EmailDoCoMoResultParser.h"
#import "BookmarkDoCoMoResultParser.h"
#import "TelResultParser.h"
#import "GeoResultParser.h"
#import "URLTOResultParser.h"
#import "URLResultParser.h"
#import "TextResultParser.h"
#import "SMSResultParser.h"
#import "SMSTOResultParser.h"

@implementation ResultParser

static NSArray *resultParsers = nil;
+ (NSArray *)resultParsers {
  if (resultParsers == nil) {
    resultParsers = 
    [[NSArray alloc] initWithObjects:
     [MeCardParser class],
     [EmailDoCoMoResultParser class],
     [BookmarkDoCoMoResultParser class],
     [TelResultParser class],
     [GeoResultParser class],
     [SMSTOResultParser class],
     [SMSResultParser class],
     [URLTOResultParser class],
     [URLResultParser class],
     [TextResultParser class],
     nil];
  }
  return resultParsers;
}

+ (ParsedResult *)parsedResultForString:(NSString *)s {
#ifdef DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif
  for (Class c in [self resultParsers]) {
    ParsedResult *result = [c parsedResultForString:s];
    if (result != nil) {
      return result;
    }
  }
  return nil;
}

@end
