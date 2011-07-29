//
//  AddressBookAUResultParser.m
//  ZXing
//
//  Ported to Objective-C by George Nachman on 7/7/2011.
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

#import "AddressBookAUResultParser.h"
#import "BusinessCardParsedResult.h"
#import "CBarcodeFormat.h"
#import "ArrayAndStringCategories.h"

@interface NSString (AddressBookAUResultParser)

- (NSArray *)fieldsWithPrefix:(NSString *)prefix
                   maxResults:(int)maxResults
                         trim:(BOOL)trim;

@end

@implementation NSString (AddressBookAUResultParser)

- (NSArray *)fieldsWithPrefix:(NSString *)prefix
                   maxResults:(int)maxResults
                         trim:(BOOL)trim {
  NSMutableArray *values = nil;
  for (int i = 1; i <= maxResults; i++) {
    NSString *prefixWithNum = [NSString stringWithFormat:@"%@%d:", prefix, i];
    NSString *value = [self fieldWithPrefix:prefixWithNum
                                 terminator:@"\r"];
    if (value == nil) {
      break;
    }
    if (trim) {
      value = [value stringWithTrimmedWhitespace];
    }
    if (values == nil) {
      values = [NSMutableArray arrayWithCapacity:maxResults];
    }
    [values addObject:value];
  }
  return values;
}

@end


@implementation AddressBookAUResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

//
// Implements KDDI AU's address book format. See
// http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html.
// (Thanks to Yuzo for translating!)
// Credit to Sean Owen as the original author of this class in Java.
//
+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    // Force objects in ArrayAndStringCategories.m to be loaded from static
    // library to work around a linker bug.
    ForceArrayAndStringCategoriesToLoad();

    // MEMORY is mandatory; seems like a decent indicator, as does
    // end-of-record separator CR/LF
    if (rawText == nil ||
        [rawText rangeOfString:@"MEMORY"].location == NSNotFound ||
        [rawText rangeOfString:@"\r\n"].location == NSNotFound) {
      return nil;
    }

    // NAME1 and NAME2 have specific uses, namely written name and
    // pronunciation, respectively. Therefore we treat them specially instead
    // of as an array of names.
    NSString *name = [[rawText fieldWithPrefix:@"NAME1:"
                                    terminator:@"\r"] stringWithTrimmedWhitespace];
    NSString *pronunciation =
        [[rawText fieldWithPrefix:@"NAME2:"
                       terminator:@"\r"] stringWithTrimmedWhitespace];

    NSArray *phoneNumbers =
        [rawText fieldsWithPrefix:@"TEL"
                       maxResults:3
                             trim:YES];
    NSArray *emails =
        [rawText fieldsWithPrefix:@"MAIL"
                       maxResults:3
                             trim:YES];
    NSString *note =
        [rawText fieldWithPrefix:@"MEMORY:"
                      terminator:@"\r"];
       
    NSString *address =
        [[rawText fieldWithPrefix:@"ADD:"
                       terminator:@"\r"] stringWithTrimmedWhitespace];
    NSArray *addresses = address ? [NSArray arrayWithObject:address] : nil;

    BusinessCardParsedResult *result = [[BusinessCardParsedResult alloc] init];

    result.names = [NSArray arrayWithStringIfNotNil:name];
    result.pronunciation = pronunciation;
    result.phoneNumbers = phoneNumbers;
    result.emails = emails;
    result.note = note;
    result.addresses = addresses;

    return [result autorelease];
}

@end
