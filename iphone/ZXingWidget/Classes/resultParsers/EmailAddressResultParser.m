//
//  EmailAddressResultParser.m
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

#import "EmailAddressResultParser.h"
#import "EmailParsedResult.h"
#import "ArrayAndStringCategories.h"

//
// Represents a result that encodes an e-mail address, either as a plain address
// like "joe@example.org" or a mailto: URL like "mailto:joe@example.org".
// It can also take query parameters. For example:
// "mailto:joe@example.org?subject=hello&body=hello+world"
// "mailto:?to=joe%40example.org&subject=hello&body=hello+world"
//
// A query paramter of "to" will supercede the user/host portion of the URL if
// both are present.
//
// If the mailto: prefix is absent, this will recognize strings that look
// approximately like email addresses: they have an @ sign and no characters
// outlawed by RFC 2822.
//
// Originally by Sean Owen
//
@implementation EmailAddressResultParser

+ (void)load {
  [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)rawText
                                 format:(BarcodeFormat)format {
    if (rawText == nil) {
      return nil;
    }
    NSString *kMailto = @"mailto";
    NSURL *url = [NSURL URLWithString:rawText];
    if (url && [[[url scheme] lowercaseString] isEqualToString:kMailto]) {
        NSDictionary *nameValues = nil;
        if ([url query]) {
            nameValues = [ResultParser dictionaryForQueryString:[url query]];
        }

        // Set the "to" value with the query parameter or as much of the
        // user+host as we have.
        NSString *user = [url user];
        NSString *host = [url host];
        NSString *emailAddress = nil;
        EmailParsedResult *result =
            [[[EmailParsedResult alloc] init] autorelease];
        if ([nameValues objectForKey:@"to"]) {
            emailAddress = [nameValues objectForKey:@"to"];
        } else if (user && host) {
            emailAddress = [NSString stringWithFormat:@"%@@%@", user, host];
        } else if (user) {
            emailAddress = user;
        }
        if (!emailAddress) {
            return nil;
        }

        // Add optional fields if present.
        result.to = emailAddress;
        if ([nameValues objectForKey:@"subject"]) {
            result.subject = [nameValues objectForKey:@"subject"];
        }
        if ([nameValues objectForKey:@"body"]) {
            result.body = [nameValues objectForKey:@"body"];
        }
        return result;
    } else {
        // It doesn't start with mailto:, but maybe it looks like an email
        // address.
        if (![self isBasicallyValidEmailAddress:rawText]) {
          return nil;
        }
        EmailParsedResult *result =
            [[[EmailParsedResult alloc] init] autorelease];
        result.to = rawText;
        return result;
    }
}

// This implements only the most basic checking for an email address's validity:
// that it contains an '@' contains no characters disallowed by RFC 2822.
// This is an overly lenient definition of validity. We want to generally be
// lenient here since this class is only intended to encapsulate what's in a
// barcode, not "judge" it.
+ (BOOL)isBasicallyValidEmailAddress:(NSString *)address {
    NSRange atRange = [address rangeOfString:@"@"];
    if (atRange.location == NSNotFound) {
        return NO;
    }
    // Strip out the first at sign (exactly one is allowed).
    NSString *atlessEmail =
        [address stringByReplacingCharactersInRange:atRange
                                         withString:@""];
    // Set of non-alphanumeric characters allowed by rfc2822.
    NSString *allowedChars = @"!#$%&'*+-/=?^_`{}|~.";
    NSMutableCharacterSet *allowedSet =
        [NSCharacterSet characterSetWithCharactersInString:allowedChars];
    [allowedSet formUnionWithCharacterSet:
        [NSCharacterSet alphanumericCharacterSet]];
    NSCharacterSet *bogusSet = [allowedSet invertedSet];

    if ([atlessEmail rangeOfCharacterFromSet:bogusSet].location != NSNotFound) {
        return NO;
    }
    return YES;
}

@end

