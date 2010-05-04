//
//  ShowMapAction.m
//  ZXing
//
//  Created by Christian Brunschen on 05/06/2008.
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

#import "ShowMapAction.h"


@implementation ShowMapAction

@synthesize location;

static NSURL * URLForLocation(NSString *location) {
  NSString *urlString = [NSString stringWithFormat:@"http://maps.google.com/maps?q=%@", 
                         [location stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
  return [NSURL URLWithString:urlString];
}

- (id)initWithLocation:(NSString *)l {
  if ((self = [super initWithURL:URLForLocation(l)]) != nil) {
    self.location = l;
  }
  return self;
}

+ (id)actionWithLocation:(NSString *)location {
  return [[[self alloc] initWithLocation:location] autorelease];
}

- (NSString *)title {
  return NSLocalizedString(@"ShowMapAction action title", @"Show on Map");
}

- (NSString *)alertTitle {
  return NSLocalizedString(@"ShowMapAction alert title", @"Show on Map");
}

- (NSString *)alertMessage {
  return [NSString stringWithFormat:NSLocalizedString(@"ShowMapAction alert message", @"Show location %@ on Map ?"), self.location];
}

- (NSString *)alertButtonTitle {
  return NSLocalizedString(@"ShowMapAction alert button title", @"Show");
}


- (void)dealloc {
  [location release];
  [super dealloc];
}

@end
