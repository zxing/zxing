//
//  ResultParser.m
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
#import "PlainEmailResultParser.h"

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
     [PlainEmailResultParser class],
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
#ifdef DEBUG
    NSLog(@"trying %@", NSStringFromClass(c));
#endif
    ParsedResult *result = [c parsedResultForString:s];
    if (result != nil) {
#ifdef DEBUG
      NSLog(@"parsed as %@ %@", NSStringFromClass([result class]), result);
#endif
      return result;
    }
  }
  return nil;
}

@end
