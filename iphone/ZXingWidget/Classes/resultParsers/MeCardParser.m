//
//  MeCardParser.m
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

#import "MeCardParser.h"
#import "BusinessCardParsedResult.h"
#import "CBarcodeFormat.h"

@interface MeCardParser (Private)

+ (BOOL)isWellFormedBirthday:(NSString *)birthday;

@end

@implementation MeCardParser (Private)

+ (BOOL)isWellFormedBirthday:(NSString *)birthday {
  if ([birthday length] != 8) {
    return NO;
  }
  NSCharacterSet* nonDigits =
      [[NSCharacterSet characterSetWithCharactersInString:@"0123456789"]
       invertedSet];
  NSRange range = [birthday rangeOfCharacterFromSet:nonDigits];
  return range.location == NSNotFound;
}

@end

@implementation MeCardParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s
                                 format:(BarcodeFormat)format {
  NSRange foundRange = [s rangeOfString:@"MECARD:"];
  if (foundRange.location == NSNotFound) {
    return nil;
  }
  
  NSString *name = [[s fieldWithPrefix:@"N:"] stringWithTrimmedWhitespace];
  if (name == nil) {
    return nil;
  }
  
  BusinessCardParsedResult *result = [[BusinessCardParsedResult alloc] init];
  result.names = [NSArray arrayWithObject:name];
  result.pronunciation = [[s fieldWithPrefix:@"SOUND:"] stringWithTrimmedWhitespace];
  result.phoneNumbers = [[s fieldsWithPrefix:@"TEL:"] stringArrayWithTrimmedWhitespace];
  result.emails = [[s fieldsWithPrefix:@"EMAIL:"] stringArrayWithTrimmedWhitespace];
  result.note = [s fieldWithPrefix:@"NOTE:"];
  result.addresses = [[s fieldsWithPrefix:@"ADR:"] stringArrayWithTrimmedWhitespace];
  result.birthday = [[s fieldWithPrefix:@"BDAY:"] stringWithTrimmedWhitespace];
  if (result.birthday != nil && ![MeCardParser isWellFormedBirthday:result.birthday]) {
      // No reason to throw out the whole card because the birthday is formatted wrong.
      result.birthday = nil;
  }

  // The following tags are not stricty part of MECARD spec, but as they are standard in
  // vcard, we honor them.
  result.url = [[s fieldWithPrefix:@"URL:"] stringWithTrimmedWhitespace];
  result.organization = [[s fieldWithPrefix:@"ORG:"] stringWithTrimmedWhitespace];
  result.jobTitle = [s fieldWithPrefix:@"TITLE:"];
  
  return [result autorelease];
}


@end
