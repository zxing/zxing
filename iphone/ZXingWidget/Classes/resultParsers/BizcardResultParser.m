//
//  BizcardResultParser.m
//  ZXing
//
//  Ported to Objective-C by George Nachman on 7/14/2011.
/*
 * Copyright 2011 ZXing authors
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

#import "BizcardResultParser.h"
#import "BusinessCardParsedResult.h"
#import "CBarcodeFormat.h"
#import "DoCoMoResultParser.h"

@implementation BizcardResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (NSArray*)arrayWithFirstName:(NSString*)firstName
                       lastName:(NSString*)lastName {
  if (firstName && !lastName) {
      return [NSArray arrayWithObject:firstName];
  } else if (!firstName && lastName) {
      return [NSArray arrayWithObject:lastName];
  } else if (lastName && firstName) {
      return [NSArray arrayWithObject:[NSString stringWithFormat:@"%@ %@",
                                           firstName, lastName, nil]];
  } else {
    return nil;
  }
}

//
// Implements the "BIZCARD" address book entry format, though this has been
// largely reverse-engineered from examples observed in the wild -- still
// looking for a definitive reference.
//
// @author Sean Owen
//
+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    if (rawText == nil || ![rawText hasPrefix:@"BIZCARD:"]) {
      return nil;
    }
    NSString *firstName =
        [[rawText fieldWithPrefix:@"N:"] stringWithTrimmedWhitespace];
    NSString *lastName =
        [[rawText fieldWithPrefix:@"X:"] stringWithTrimmedWhitespace];
    NSString *title =
        [[rawText fieldWithPrefix:@"T:"] stringWithTrimmedWhitespace];
    NSString *org =
        [[rawText fieldWithPrefix:@"C:"] stringWithTrimmedWhitespace];
    NSArray *addresses =
        [[rawText fieldsWithPrefix:@"A:"] stringArrayWithTrimmedWhitespace];
    NSString *phoneNumber1 =
        [[rawText fieldWithPrefix:@"B:"] stringWithTrimmedWhitespace];
    NSString *phoneNumber2 =
        [[rawText fieldWithPrefix:@"M:"] stringWithTrimmedWhitespace];
    NSString *phoneNumber3 =
        [[rawText fieldWithPrefix:@"F:"] stringWithTrimmedWhitespace];
    NSString *email =
        [[rawText fieldWithPrefix:@"E:"] stringWithTrimmedWhitespace];

    BusinessCardParsedResult *result = [[BusinessCardParsedResult alloc] init];

    result.names = [BizcardResultParser arrayWithFirstName:firstName lastName:lastName];
    NSMutableArray *phoneNumbers = [NSMutableArray arrayWithCapacity:3];
    if (phoneNumber1) {
        [phoneNumbers addObject:phoneNumber1];
    }
    if (phoneNumber1) {
        [phoneNumbers addObject:phoneNumber2];
    }
    if (phoneNumber1) {
        [phoneNumbers addObject:phoneNumber3];
    }
    if ([phoneNumbers count]) {
        result.phoneNumbers = phoneNumbers;
    }
    result.emails = email ? [NSArray arrayWithObject:email] : nil;
    result.addresses = addresses;
    result.organization = org;
    result.jobTitle = title;

    return [result autorelease];
}

@end
