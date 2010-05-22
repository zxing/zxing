//
//  AddressBookDoCoMoParsedResult.m
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

@implementation BusinessCardParsedResult

@synthesize name;
@synthesize phoneNumbers;
@synthesize note;
@synthesize email;
@synthesize urlString;
@synthesize address;

- (NSString *)stringForDisplay {
  NSMutableString *result = [NSMutableString stringWithString:self.name];
  if (self.phoneNumbers) {
    for (NSString *number in self.phoneNumbers) {
      [result appendFormat:@"\n%@", number];
    }
  }
  
  if (self.email) {
    [result appendFormat:@"\n%@", self.email];
  }
  if (self.urlString) {
    [result appendFormat:@"\n%@", self.urlString];
  }
  if (self.note) {
    [result appendFormat:@"\n%@", self.note];
  }
  if (self.address) {
    [result appendFormat:@"\n%@", self.address];
  }
  return [NSString stringWithString:result];
}

- (void)populateActions {
  [actions addObject:[AddContactAction actionWithName:self.name
                                         phoneNumbers:self.phoneNumbers 
                                                email:self.email 
                                                  url:self.urlString 
                                              address:self.address 
                                                 note:self.note]];
}

- (void) dealloc {
  [name release];
  [phoneNumbers release];
  [email release];
  [urlString release];
  [address release];
  [note release];
  [super dealloc];
}

+ (NSString *)typeName {
  return NSLocalizedString(@"Contact Result Type Name", @"Contact");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"business-card.png"];
}

@end
