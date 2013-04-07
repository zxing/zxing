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

@interface ResultParser(Private)

+ (NSString *)urlDecode:(NSString *)str;

@end

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
#if ZXING_DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif
  for (Class c in [self resultParsers]) {
#if ZXING_DEBUG
    NSLog(@"trying %@", NSStringFromClass(c));
#endif
    ParsedResult *result = [c parsedResultForString:s format:barcodeFormat];
    if (result != nil) {
#if ZXING_DEBUG
      NSLog(@"parsed as %@ %@", NSStringFromClass([result class]), result);
#endif
      return result;
    }
  }

#if ZXING_DEBUG
  NSLog(@"No result parsers matched. Falling back to text.");
#endif
  return [TextResultParser parsedResultForString:s format:barcodeFormat];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  return [ResultParser parsedResultForString:s format:BarcodeFormat_NONE];
}

+ (NSDictionary*)dictionaryForQueryString:(NSString *)queryString {
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    NSArray *keyValuePairs = [queryString componentsSeparatedByString:@"&"];
    for (NSString *kvp in keyValuePairs) {
        NSRange equals = [kvp rangeOfString:@"="];
        if (equals.location != NSNotFound) {
            NSString *key =
                [kvp substringWithRange:NSMakeRange(0, equals.location)];
            NSUInteger i = equals.location + 1;
            NSString *value =
                [kvp substringWithRange:NSMakeRange(i, [kvp length] - i)];
            [result setObject:[self urlDecode:value]
                       forKey:[self urlDecode:key]];
        }
    }
    return result;
}

@end

@implementation ResultParser(Private)

+ (NSString *)urlDecode:(NSString *)str {
    // Obj-C's url decoder does everything except + to space conversion.
    NSString *result = [str stringByReplacingOccurrencesOfString:@"+"
                                                      withString:@" "];
    return [result stringByReplacingPercentEscapesUsingEncoding:
        NSUTF8StringEncoding];
}

@end
