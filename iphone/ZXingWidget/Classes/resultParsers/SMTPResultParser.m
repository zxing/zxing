//
//  SMTPResultParser.m
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

#import "SMTPResultParser.h"
#import "EmailParsedResult.h"
#import "ArrayAndStringCategories.h"

@implementation SMTPResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

//
// Parses an "smtp:" URI result, whose format is not standardized but appears
// to be like one of these:
// smtp:to
// smtp:to:subject
// smtp:to:subject:body
//
// See http://code.google.com/p/zxing/issues/detail?id=536
//
// Originally by Sean Owen
//
+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    if (rawText == nil) {
      return nil;
    }
    NSString *kSmtp = @"smtp:";
    if ([rawText length] <= [kSmtp length] ||
        (![rawText hasPrefix:kSmtp] &&
         ![rawText hasPrefix:[kSmtp uppercaseString]])) {
        return nil;
    }

    NSArray *components = [rawText componentsSeparatedByString:@":"];
    const NSUInteger n = [components count];
    if (n < 2 || n > 4) {
        return nil;
    }

    EmailParsedResult *result = [[[EmailParsedResult alloc] init] autorelease];
    if (n >= 2) {
        result.to = [components objectAtIndex:1];
    }
    if (n >= 3) {
        result.subject = [components objectAtIndex:2];
    }
    if (n >= 4) {
        result.body = [components objectAtIndex:3];
    }
    return result;
}

@end
