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

+ (ParsedResult *)parsedResultForString:(NSString *)s {
#ifdef DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif

  // Make the parser of last resort the last parser we try.
  NSMutableArray *resultParsers =
    [NSMutableArray arrayWithArray:[[self resultParsers] allObjects]];
  NSUInteger textIndex =
    [resultParsers indexOfObject:NSClassFromString(@"TextResultParser")];
  if (NSNotFound != textIndex) {
    // If it is present, make sure it is last.
    [resultParsers exchangeObjectAtIndex:textIndex
                       withObjectAtIndex:[resultParsers count] - 1];
  }

  for (Class c in resultParsers) {
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
