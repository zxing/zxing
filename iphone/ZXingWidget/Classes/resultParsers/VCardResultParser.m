//
//  VCardResultParser.m
//  ZXing
//
//  Ported to Objective-C by George Nachman on 7/19/2011.
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

#import "BusinessCardParsedResult.h"
#import "CBarcodeFormat.h"
#import "ResultParser.h"
#import "AddressBookAUResultParser.h"
#import "VCardResultParser.h"
#import "ArrayAndStringCategories.h"

@interface NSString (VCardResultParser)

// Extract a single VCard value from this with a given prefix.
- (NSString *)vcardValueForFieldWithPrefix:(NSString *)prefix;

// Extracts an array of VCard values from this string with a given prefix.
- (NSArray *)vcardValuesForFieldWithPrefix:(NSString *)prefix;

// Returns true if the string's value is a well-formed date.
- (BOOL)isVCardDate;

// Returns the starting index (or NSNotFound) of the given substring not
// before index |offset|.
- (NSUInteger)vcardIndexOf:(NSString*)substr startingAt:(int)offset;

// Assuming this string is in quoted printable in the character set |charset|,
// decode the QP encoding and convert to NSString's native character set.
- (NSString *)vcardStringFromQuotedPrintableWithCharset:(NSString *)charset;

// Strip out \r. Also strip out \n plus one character following it if possible.
- (NSString *)vcardStringWithoutContinuationCRLF;

// Split up key-value strings on = sign.
- (NSString *)vcardKeyComponent;
- (NSString *)vcardValueComponent;
@end

@interface NSArray (VCardResultParser)

// Reformat VCard names from a semicolon-delimited list to a human-readable
// name.
- (NSArray *)vcardArrayWithFormattedNames;
// Reformat VCard addresses from a semicolon-delimited list to a human-readable
// name.
- (NSArray *)vcardArrayWithFormattedAddresses;

@end

// Parses contact information formatted according to the VCard (2.1) format.
// This is not a complete implementation but should parse information as
// commonly encoded in 2D barcodes.
//
// Originally by Sean Owen. Adapted to Objective-C by George Nachman.
//
@implementation VCardResultParser

+ (void)load {
    [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    // Although we should insist on the raw text ending with "END:VCARD",
    // there's no reason to throw out everything else we parsed just because
    // this was omitted. In fact, Eclair is doing just that, and we can't parse
    // its contacts without this leniency.
    if (rawText == nil || ![rawText hasPrefix:@"BEGIN:VCARD"]) {
        return nil;
    }
    NSArray *names = [[rawText vcardValuesForFieldWithPrefix:@"FN"]
                         stringArrayWithTrimmedWhitespace];
    if ([names count] == 0) {
      // If no display names found, look for regular name fields and format them
      names = [[rawText vcardValuesForFieldWithPrefix:@"N"]
                  stringArrayWithTrimmedWhitespace];
    }
    names = [names vcardArrayWithFormattedNames];
    NSArray *phoneNumbers = [[rawText vcardValuesForFieldWithPrefix:@"TEL"] stringArrayWithTrimmedWhitespace];
    NSArray *emails = [[rawText vcardValuesForFieldWithPrefix:@"EMAIL"] stringArrayWithTrimmedWhitespace];
    NSString *note = [rawText vcardValueForFieldWithPrefix:@"NOTE"];
    NSArray *addresses = [[[rawText vcardValuesForFieldWithPrefix:@"ADR"] stringArrayWithTrimmedWhitespace]
                                  vcardArrayWithFormattedAddresses];
    NSString *org = [[rawText vcardValueForFieldWithPrefix:@"ORG"] stringWithTrimmedWhitespace];
    NSString *birthday = [[rawText vcardValueForFieldWithPrefix:@"BDAY"]stringWithTrimmedWhitespace];
    if (![birthday isVCardDate]) {
        birthday = nil;
    }
    NSString *title = [[rawText vcardValueForFieldWithPrefix:@"TITLE"]stringWithTrimmedWhitespace];
    NSString *url = [[rawText vcardValueForFieldWithPrefix:@"URL"] stringWithTrimmedWhitespace];

    BusinessCardParsedResult *result =
        [[[BusinessCardParsedResult alloc] init] autorelease];

    if ([names count]) {
        result.names = names;
    }
    if ([phoneNumbers count]) {
        result.phoneNumbers = phoneNumbers;
    }
    if ([emails count]) {
        result.emails = emails;
    }
    if ([note length]) {
        result.note = note;
    }
    if ([addresses count]) {
        result.addresses = addresses;
    }
    if ([org length]) {
        result.organization = org;
    }
    if (birthday) {
        result.birthday = birthday;
    }
    if ([title length]) {
        result.jobTitle = title;
    }
    if ([url length]) {
        result.url = url;
    }

    return result;
}

@end

@implementation NSString (VCardResultParser)

- (NSUInteger)vcardIndexOf:(NSString*)substr startingAt:(int)offset {
    NSRange temp = NSMakeRange(offset, [self length] - offset);
    NSRange r = [self rangeOfString:substr
                            options:0
                              range:temp];
    return r.location;
}

- (NSString *)vcardKeyComponent {
    NSUInteger equals = [self vcardIndexOf:@"=" startingAt:0];
    if (equals != NSNotFound) {
        return [self substringWithRange:NSMakeRange(0, equals)];
    } else {
        return nil;
    }
}

- (NSString *)vcardValueComponent {
    NSUInteger equals = [self vcardIndexOf:@"=" startingAt:0];
    if (equals != NSNotFound) {
        return [self substringWithRange:NSMakeRange(equals + 1,
                                                    [self length] - equals - 1)];
    } else {
        return nil;
    }
}

- (NSDictionary*)parsedFieldMetadata {
  NSArray *parts = [self componentsSeparatedByCharactersInSet:
                    [NSCharacterSet characterSetWithCharactersInString:@";:"]];
  NSMutableDictionary *result = [NSMutableDictionary dictionary];
  for (NSString *part in parts) {
    NSString *key = [part vcardKeyComponent];
    NSString *value = [part vcardValueComponent];
    if (key && value) {
      [result setObject:value forKey:key];
    }
  }

  return result;
}

- (NSArray *)vcardValuesForFieldWithPrefix:(NSString *)prefix {
    NSMutableArray *matches = [NSMutableArray array];
    NSUInteger i = 0;
    NSUInteger myLength = [self length];
    unichar c;
    while (i < myLength) {
        i = [self vcardIndexOf:prefix startingAt:i];
        if (i == NSNotFound) {
            break;
        }

        if (i > 0 && [self characterAtIndex:i - 1] != '\n') {
            // This didn't start a new token: we matched in the middle of
            // something.
            i++;
            continue;
        }
        i += [prefix length];  // Skip past the prefix.
        c = [self characterAtIndex:i];
        if (c != ':' && c != ';') {
            // What we found wasn't actually a prefix.
            continue;
        }

        const NSUInteger metadataStart = i;
        // Skip until we find a colon.
        while ([self characterAtIndex:i] != ':') {
            i++;
        }

        // Extract key-value metadata fields between the ; and the : after the
        // prefix.
        BOOL quotedPrintable = NO;
        NSString *quotedPrintableCharset = @"ASCII";
        if (i >= metadataStart + 1) {
          NSString *metaData = [self substringWithRange:
                                    NSMakeRange(metadataStart + 1,
                                                i - (metadataStart + 1))];
          NSDictionary *metaDataDict = [metaData parsedFieldMetadata];
          NSString *encoding = [metaDataDict objectForKey:@"ENCODING"];
          NSString *charset = [metaDataDict objectForKey:@"CHARSET"];
          if ([encoding isEqualToString:@"QUOTED-PRINTABLE"]) {
            quotedPrintable = YES;
            if (charset) {
              quotedPrintableCharset = charset;
            }
          }
        }

        i++;  // Skip the colon.

        const NSUInteger matchStart = i;  // Found the start of a match here.

        while ((i = [self vcardIndexOf:@"\n" startingAt:i]) != NSNotFound) {
            if (i + 1 < [self length] &&
                ([self characterAtIndex:i + 1] == ' ' ||
                 [self characterAtIndex:i + 1] == '\t')) {
                // If it's followed by a tab or space, ignore them.
                i += 2;
            } else if (quotedPrintable &&
                       i >= 2 &&
                       ([self characterAtIndex:i-1] == '=' ||
                        [self characterAtIndex:i-2] == '=')) {
                // Indicates this is a quoted-printable continuation so
                // ignore the newline.
                i++;
            } else {
                break;
            }
        }

        if (i == NSNotFound) {
            // No terminating character.
            break;
        } else if (i > matchStart) {
            // Found a legal line. Add it to the output. i must be greater than
            // 0 because matchStart is unsigned.
            if ([self characterAtIndex:i-1] == '\r') {
                i--;  // Back up over \r if present.
            }
            NSUInteger rangeLength;
            if (i >= matchStart) {
                rangeLength = i - matchStart;
            } else {
                rangeLength = 0;
            }
            NSString *element = [self substringWithRange:
                NSMakeRange(matchStart, rangeLength)];
            if (quotedPrintable) {
                element = [element vcardStringFromQuotedPrintableWithCharset:
                    quotedPrintableCharset];
            } else {
                element = [element vcardStringWithoutContinuationCRLF];
            }
            [matches addObject:element];
            i++;
        } else {
            // Zero-length line.
            i++;
        }
    }

    return matches;
}

- (NSString *)vcardStringWithoutContinuationCRLF {
    int length = [self length];
    NSMutableString *result = [NSMutableString stringWithCapacity:length];
    BOOL lastWasLF = NO;
    for (int i = 0; i < length; i++) {
        if (lastWasLF) {
            lastWasLF = NO;
            continue;
        }
        unichar c = [self characterAtIndex:i];
        lastWasLF = NO;
        switch (c) {
            case '\n':
                lastWasLF = YES;
                break;
            case '\r':
                break;
            default:
                [result appendString:[NSString stringWithCharacters:&c
                                                             length:1]];
                break;
        }
    }
    return result;
}

- (NSString *)vcardStringFromQuotedPrintableWithCharset:(NSString *)charset {
    int length = [self length];
    NSMutableData *temp = [NSMutableData dataWithCapacity:length];
    for (int i = 0; i < length; i++) {
        unichar c = [self characterAtIndex:i];
        switch (c) {
            case '\r':
            case '\n':
                break;
            case '=':
                if (i < length - 2) {
                    unichar nextChar = [self characterAtIndex:i+1];
                    if (nextChar == '\r' || nextChar == '\n') {
                        // Ignore, it's just a continuation symbol.
                    } else {
                        NSString *hexstr =
                            [self substringWithRange:NSMakeRange(i+1, 2)];
                        NSScanner *scanner =
                            [NSScanner scannerWithString:hexstr];
                        unsigned result;
                        if ([scanner scanHexInt:&result]) {
                            unsigned char parsedChar = result;
                            [temp appendBytes:&parsedChar length:1];
                        }

                        i += 2;
                    }
                }
                break;
            default:
                [temp appendBytes:&c length:1];
        }
    }

    NSStringEncoding encoding;
    encoding = CFStringConvertEncodingToNSStringEncoding(
        CFStringConvertIANACharSetNameToEncoding((CFStringRef) charset));
    return [[[NSString alloc] initWithData:temp
                                  encoding:encoding] autorelease];
}

- (NSString *)vcardValueForFieldWithPrefix:(NSString *)prefix {
    NSArray *values = [self vcardValuesForFieldWithPrefix:prefix];
    return [values count] ? [values objectAtIndex:0] : @"";
}

- (BOOL)rangeIsDigits:(NSRange)range {
    for (NSUInteger i = range.location;
         i < range.location + range.length;
         i++) {
        unichar c = [self characterAtIndex:i];
        if (c < '0' || c > '9') {
            return NO;
        }
    }
    return YES;
}

- (BOOL)isVCardDate {
    // Not really sure this is true but matches practice
    if ([self length] == 8 && [self rangeIsDigits:NSMakeRange(0, 8)]) {
        // Matches YYYYMMDD
        return YES;
    } else if ([self length] == 10 &&
               [self characterAtIndex:4] == '-' &&
               [self characterAtIndex:7] == '-' &&
               [self rangeIsDigits:NSMakeRange(0, 4)] &&
               [self rangeIsDigits:NSMakeRange(5, 2)] &&
               [self rangeIsDigits:NSMakeRange(8, 2)]) {
        // Matches YYYY-MM-DD
        return YES;
    } else {
        return NO;
    }
}

@end

@implementation NSArray (VCardResultParser)

- (NSArray *)vcardArrayWithFormattedAddresses {
    NSMutableArray* result = [NSMutableArray array];
    for (NSString* address in self) {
        [result addObject:
            [[address stringByReplacingOccurrencesOfString:@";"
                                                withString:@" "]
                stringWithTrimmedWhitespace]];
    }
    return result;
}

// Formats name fields of the form "Public;John;Q.;Reverend;III" into a form
// like "Reverend John Q. Public III".
- (NSArray *)vcardArrayWithFormattedNames {
    NSMutableArray *result = [NSMutableArray array];
    for (NSString *name in self) {
        NSArray *components = [name componentsSeparatedByString:@";"];
        int newOrder[] = { 3, 1, 2, 0, 4 };
        int numReorderItems = sizeof(newOrder) / sizeof(int);
        NSMutableString *formattedName =
            [NSMutableString stringWithCapacity:[name length]];
        int n = [components count];
        for (int i = 0; i < numReorderItems; i++) {
            int j = newOrder[i];
            if (n > j) {
                [formattedName appendString:@" "];
                [formattedName appendString:[components objectAtIndex:j]];
            }
        }
        [result addObject:[formattedName stringWithTrimmedWhitespace]];
    }
    return result;
}


@end
