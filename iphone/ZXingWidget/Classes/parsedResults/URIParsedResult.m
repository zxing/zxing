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


@implementation URIParsedResult

@synthesize urlString;
@synthesize title;
@synthesize URL;

- (ResultAction *)createAction {
  return [OpenUrlAction actionWithURL:self.URL];
}

- (id)initWithURLString:(NSString *)s title:(NSString *)t URL:(NSURL *)url {
  if ((self = [super init]) != nil) {
    self.urlString = s;
    self.title = t;
    self.URL = url;
  }
  return self;
}

- (id)initWithURLString:(NSString *)s URL:(NSURL *)url {
  return [self initWithURLString:s title:nil URL:url];
}

- (id)initWithURLString:(NSString *)s title:(NSString *)t {
  return [self initWithURLString:s title:t URL:[NSURL URLWithString:s]];
}

- (id)initWithURLString:(NSString *)s {
  return [self initWithURLString:s title:nil URL:[NSURL URLWithString:s]];
}

- (NSString *)stringForDisplay {
  return self.title ? 
  [NSString stringWithFormat:@"%@ <%@>", self.title, self.urlString] :
  self.urlString;
}

+ (NSString *)typeName {
  return NSLocalizedString(@"URIParsedResult type name", @"URI");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"link2.png"];
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
  [title release];
  [super dealloc];
}

@end
