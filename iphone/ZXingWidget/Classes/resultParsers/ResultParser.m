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
#import "TextResultParser.h"

@implementation ResultParser

static NSMutableSet *sResultParsers = nil;

+ (void)registerResultParserClass:(Class)resultParser {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  @synchronized(self) {
    if (!sResultParsers) {
      sResultParsers = [[NSMutableSet alloc] init];
    }
    [sResultParsers addObject:resultParser];
  }
  [pool drain];
}

+ (NSSet *)resultParsers {
  NSSet *resultParsers = nil;
  @synchronized(self) {
    resultParsers = [[sResultParsers copy] autorelease];
  }
  return resultParsers;
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)barcodeFormat {
#ifdef DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif
  for (Class c in [self resultParsers]) {
#ifdef DEBUG
    NSLog(@"trying %@", NSStringFromClass(c));
#endif
    ParsedResult *result = [c parsedResultForString:s format:barcodeFormat];
    if (result != nil) {
#ifdef DEBUG
      NSLog(@"parsed as %@ %@", NSStringFromClass([result class]), result);
#endif
      return result;
    }
  }

#ifdef DEBUG
  NSLog(@"No result parsers matched. Falling back to text.");
#endif
  return [TextResultParser parsedResultForString:s format:barcodeFormat];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  return [ResultParser parsedResultForString:s format:BarcodeFormat_None];
}

@end
