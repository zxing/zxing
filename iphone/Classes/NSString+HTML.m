//
//  NSString+HTML.m
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

#import "NSString+HTML.h"


@implementation NSString (HTMLExtensions)

static NSDictionary *htmlEscapes = nil;
static NSDictionary *htmlUnescapes = nil;

+ (NSDictionary *)htmlEscapes {
  if (!htmlEscapes) {
    htmlEscapes = [[NSDictionary alloc] initWithObjectsAndKeys:
                   @"&amp;", @"&",
                   @"&lt;", @"<",
                   @"&gt;", @">",
                   nil
                   ];
  }
  return htmlEscapes;
}

+ (NSDictionary *)htmlUnescapes {
  if (!htmlUnescapes) {
    htmlUnescapes = [[NSDictionary alloc] initWithObjectsAndKeys:
                     @"&", @"&amp;",
                     @"<", @"&lt;", 
                      @">", @"&gt;",
                     nil
                     ];
  }
  return htmlEscapes;
}

static NSString *replaceAll(NSString *s, NSDictionary *replacements) {
  for (NSString *key in replacements) {
    NSString *replacement = [replacements objectForKey:key];
    s = [s stringByReplacingOccurrencesOfString:key withString:replacement];
  }
  return s;
}

- (NSString *)htmlEscapedString {
  return replaceAll(self, [[self class] htmlEscapes]);
}

- (NSString *)htmlUnescapedString {
  return replaceAll(self, [[self class] htmlUnescapes]);
}

@end
