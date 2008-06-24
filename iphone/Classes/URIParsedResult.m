//
//  URIParsedResult.m
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

#import "URIParsedResult.h"
#import "OpenUrlAction.h"
#import "EmailAction.h"
#import "SMSAction.h"

@implementation NSString (ZXingURLExtensions)

- (bool)looksLikeAURI {
  if ([self rangeOfCharacterFromSet:[NSCharacterSet whitespaceCharacterSet]].location != NSNotFound) {
    return false;
  }
  if ([self rangeOfString:@":"].location == NSNotFound) {
    return false;
  }
  return true;
}

- (NSString *)massagedURLString {
  NSRange colonRange = [self rangeOfString:@":"];
  if (colonRange.location == NSNotFound) {
    return [NSString stringWithFormat:@"http://%@", self];
  } else {
    return [NSString stringWithFormat:@"%@%@",
            [[self substringToIndex:colonRange.location] lowercaseString],
            [self substringFromIndex:colonRange.location]
            ];
  }
}

@end

@implementation URIParsedResult

#define PREFIX @"URL:"

@synthesize urlString;
@synthesize URL;

- (ResultAction *)createAction {
  NSString *scheme = [self.URL scheme];
  if (scheme) {
    if ([@"mailto" isEqualToString:scheme]) {
      return [EmailAction actionWithRecipient:[urlString substringFromIndex:7] 
                                      subject:nil 
                                         body:nil];
    } else if ([@"sms" isEqualToString:scheme]) {
      return [SMSAction actionWithNumber:[urlString substringFromIndex:4]];
    }
  }
  return [OpenUrlAction actionWithURL:self.URL];
}

- initWithURLString:(NSString *)s URL:(NSURL *)url {
  if ((self = [super init]) != nil) {
    self.urlString = s;
    self.URL = url;
  }
  return self;
}

- initWithURLString:(NSString *)s {
  return [self initWithURLString:s URL:[NSURL URLWithString:s]];
}

+ parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int restStart = prefixRange.location + prefixRange.length;
    return [[[self alloc] initWithURLString:[[s substringFromIndex:restStart] massagedURLString]]
            autorelease];
  }
  
  if ([s looksLikeAURI]) {
    NSString *massaged = [s massagedURLString];
    NSURL *url = [NSURL URLWithString:massaged];
    if (url != nil) {
      return [[[self alloc] initWithURLString:massaged URL:url] autorelease];
    }
  }
  
  return nil;
}

- (NSString *)stringForDisplay {
  return self.urlString;
}


+ (NSString *)typeName {
  return @"URI";
}


- (void)populateActions { 
#ifdef DEBUG
  NSLog(@"creating action to open URL '%@'", self.urlString);
#endif
  
  [actions addObject:[self createAction]];
}

- (void)dealloc {
  [URL release];
  [urlString release];
  [super dealloc];
}

@end
