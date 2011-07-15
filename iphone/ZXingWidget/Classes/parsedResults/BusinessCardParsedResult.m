//
//  BusinesCardParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 29/05/2008.
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
#import "AddContactAction.h"

@interface BusinessCardParsedResult (Private)

- (void)append:(id)obj to:(NSMutableString *)dest;
+ (NSString *)normalizeName:(NSString*)name;

@end

@implementation BusinessCardParsedResult (Private)

// Append an object's string representation to dest preceeded by a newline. If
// it is an array, append each item sequentially.
- (void)append:(id)obj to:(NSMutableString *)dest {
  if (obj == nil) {
    return;
  }
  if ([obj isKindOfClass:[NSArray class]]) {
    for (id sub in obj) {
      [self append:sub to:dest];
    }
  } else if ([obj isKindOfClass:[NSString class]]) {
    [dest appendFormat:@"\n%@", obj];
  }
}

// Convert lastname,firstname to firstname lastname.
+ (NSString *)normalizeName:(NSString*)name {
    int comma = [name rangeOfString:@","].location;
    if (comma != NSNotFound) {
        // Format may be last,first; switch it around
        NSString* firstName = [name
            substringWithRange:NSMakeRange(comma + 1,
                                           [name length] - comma - 1)];
        NSString* lastName = [name
            substringWithRange:NSMakeRange(0, comma)];
        return [NSString stringWithFormat:@"%@ %@", firstName, lastName];
    }
    return name;
}
@end

@implementation BusinessCardParsedResult

@synthesize names;
@synthesize pronunciation;
@synthesize phoneNumbers;
@synthesize emails;
@synthesize note;
@synthesize addresses;
@synthesize organization;
@synthesize birthday;
@synthesize jobTitle;
@synthesize url;

- (NSString *)stringForDisplay {
    NSMutableString* result = [NSMutableString stringWithCapacity:1024];
    for (NSString *name in names) {
        [self append:[BusinessCardParsedResult normalizeName:name] to:result];
    }
    [self append:pronunciation to:result];
    [self append:jobTitle to:result];
    [self append:organization to:result];
    [self append:phoneNumbers to:result];
    [self append:emails to:result];
    [self append:url to:result];
    [self append:birthday to:result];
    [self append:note to:result];
    [self append:addresses to:result];

    return result;
}

- (void)populateActions {
    [actions addObject:[AddContactAction actionWithName:[self.names objectAtIndex:0]
                                           phoneNumbers:self.phoneNumbers
                                                  email:[self.emails objectAtIndex:0]
                                                    url:self.url
                                                address:[self.addresses objectAtIndex:0]
                                                   note:self.note
                                           organization:self.organization
                                               jobTitle:self.jobTitle]];
}

- (void)dealloc {
    [names release];
    [pronunciation release];
    [phoneNumbers release];
    [emails release];
    [note release];
    [addresses release];
    [organization release];
    [birthday release];
    [jobTitle release];
    [url release];

    [super dealloc];
}

+ (NSString *)typeName {
    return NSLocalizedString(@"Contact Result Type Name", @"Contact");
}

- (UIImage *)icon {
    return [UIImage imageNamed:@"business-card.png"];
}

@end
