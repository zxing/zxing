//
//  GeoParsedResult.m
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

#import "GeoParsedResult.h"
#import "ShowMapAction.h"

@implementation GeoParsedResult

@synthesize location;

- (id)initWithLocation:(NSString *)l {
  if ((self = [super init]) != nil) {
    self.location = l;
  }
  return self;
}


+ (NSString *)typeName {
  return NSLocalizedString(@"GeoParsedResult type name", @"Geolocation");
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"map-pin.png"];
}


- (NSString *)stringForDisplay {
  return [NSString stringWithFormat:NSLocalizedString(@"GeoParsedResult display", @"Geo: %@"), self.location];
}

- (void)populateActions {
  [actions addObject:[ShowMapAction actionWithLocation:self.location]];
}

- (void) dealloc {
  [location release];
  [super dealloc];
}

@end
