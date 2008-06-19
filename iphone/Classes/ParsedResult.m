//
//  ParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 22/05/2008.
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

#import "ParsedResult.h"

#import "TextParsedResult.h"
#import "TelParsedResult.h"
#import "EmailDoCoMoParsedResult.h"
#import "AddressBookDoCoMoParsedResult.h"
#import "URIParsedResult.h"
#import "URLTOParsedResult.h"
#import "BookmarkDoCoMoParsedResult.h"
#import "GeoParsedResult.h"

@implementation ParsedResult

static NSArray *parsedResultTypes = nil;

+ (NSArray *)parsedResultTypes {
  if (parsedResultTypes == nil) {
    parsedResultTypes = 
    [[NSArray alloc] initWithObjects:
     [AddressBookDoCoMoParsedResult class],
     [EmailDoCoMoParsedResult class],
     [BookmarkDoCoMoParsedResult class],
     [URLTOParsedResult class],
     [TelParsedResult class],
     [GeoParsedResult class],
     [URIParsedResult class],
     [TextParsedResult class], 
     nil];
  }
  return parsedResultTypes;
}

+ parsedResultForString:(NSString *)s {
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
  for (Class c in [self parsedResultTypes]) {
    ParsedResult *result = [c parsedResultForString:s];
    if (result != nil) {
      return result;
    }
  }
  return nil;
}

+ (NSString *)typeName {
  return @"ABSTRACT";
}

- (NSString *)stringForDisplay {
  return @"{none}";
}

- (NSArray *)actions {
  if (!actions) {
    actions = [[NSMutableArray alloc] init];
    [self populateActions];
  }
  return actions;
}

- (void) populateActions {
}

- (void) dealloc {
  [actions release];
  [super dealloc];
}

@end
