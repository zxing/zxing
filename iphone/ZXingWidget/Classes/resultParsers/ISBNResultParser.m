//
//  ISBNResultParser.m
//  ZXing
//
//  Ported to Objective-C by George Nachman on 7/29/2011
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

#import "ISBNResultParser.h"
#import "ISBNParsedResult.h"
#import "ArrayAndStringCategories.h"

@implementation ISBNResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

// Parses strings of digits that represent an ISBN
// ISBN-13 For Dummies
// http://www.bisg.org/isbn-13/for.dummies.html
// Originally by jbreiden@google.com (Jeff Breidenbach)
+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    if (format != BarcodeFormat_EAN_13) {
      return nil;
    }
    if (rawText == nil) {
      return nil;
    }
    if ([rawText length] != 13) {
      return nil;
    }
    if (![rawText hasPrefix:@"978"] &&
        ![rawText hasPrefix:@"979"]) {
      return nil;
    }

    ISBNParsedResult *result = [[[ISBNParsedResult alloc] init] autorelease];
    result.value = rawText;
    return result;
}

@end
