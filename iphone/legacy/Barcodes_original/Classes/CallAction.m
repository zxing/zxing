//
//  CallAction.m
//  ZXing
//
//  Created by Christian Brunschen on 28/05/2008.
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

#import "CallAction.h"


@implementation CallAction

@synthesize number;

+ (NSURL *)urlForNumber:(NSString *)number {
  NSString *urlString = [NSString stringWithFormat:@"tel:%@", number];
  return [NSURL URLWithString:urlString];
}

- (id)initWithNumber:(NSString *)n {
  if ((self = [super initWithURL:[[self class] urlForNumber:n]]) != nil) {
    self.number = n;
  }
  return self;
}

+ (id)actionWithNumber:(NSString *)number {
  return [[[self alloc] initWithNumber:number] autorelease];
}

- (NSString *)title {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"CallAction action title", @"Call %@"), self.number];
}

- (NSString *)alertTitle {
  return NSLocalizedString(@"CallAction alert title", @"Call");
}

- (NSString *)alertMessage {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"CallAction alert message", @"Call %@?"), self.number];
}

- (NSString *)alertButtonTitle {
  return NSLocalizedString(@"CallAction alert button title", @"Call");
}


- (void) dealloc {
  [number release];
  [super dealloc];
}

@end
