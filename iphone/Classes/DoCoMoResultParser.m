//
//  DoCoMoResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
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

#import "DoCoMoResultParser.h"


@implementation NSString (DoCoMoFieldParsing) 

- (NSString *)backslashUnescaped {
  NSRange backslashRange = [self rangeOfString:@"\\"];
  if (backslashRange.location == NSNotFound) {
    return self;
  }
  
  int max = [self length];
  int startLocation = 0;
  NSMutableString *result = [NSMutableString stringWithCapacity:[self length]];
  while (backslashRange.location != NSNotFound) {
    [result appendString:[self substringWithRange:NSMakeRange(startLocation, 
                                                              backslashRange.location - startLocation)]];
    [result appendFormat:@"%c", [self characterAtIndex:backslashRange.location + 1]];
    startLocation = backslashRange.location + 2;
    NSRange searchRange = NSMakeRange(startLocation, max - startLocation);
    backslashRange = [self rangeOfString:@"\\" options:0 range:searchRange];
  }
  if (startLocation < max) {
    [result appendString:[self substringWithRange:NSMakeRange(startLocation, max - startLocation)]];
  }
  return [NSString stringWithString:result];
}

- (NSArray *)fieldsWithPrefix:(NSString *)prefix {
  return [self fieldsWithPrefix:prefix terminator:@";"];
}

- (NSArray *)fieldsWithPrefix:(NSString *)prefix terminator:(NSString *)term {
  NSMutableArray *result = nil;
  
  int i = 0;
  int max = [self length];
  NSRange searchRange;
  NSRange foundRange;
  while (i < max) {
    searchRange = NSMakeRange(i, max - i);
    foundRange = [self rangeOfString:prefix options:0 range:searchRange];
    if(foundRange.location == NSNotFound) {
      break;
    }
    
    int start = i = foundRange.location + foundRange.length;
    bool done = false;
    while (!done) {
      searchRange = NSMakeRange(i, max - i);
      NSRange termRange = [self rangeOfString:term options:0 range:searchRange];
      if (termRange.location == NSNotFound) {
        i = max;
        done = true;
      } else if ([self characterAtIndex:termRange.location-1] == (unichar)'\\') {
        i++;
      } else {
        NSString *substring = [self substringWithRange:NSMakeRange(start, termRange.location - start)];
        NSString *unescaped = [substring backslashUnescaped];
        if (result == nil) {
          result = [NSMutableArray arrayWithObject:unescaped];
        } else {
          [result addObject:unescaped];
        }
        i = termRange.location + termRange.length;
        done = true;
      }
    }
  }
  
  return result;
}

- (NSString *)fieldWithPrefix:(NSString *)prefix {
  return [self fieldWithPrefix:prefix terminator:@";"];
}

- (NSString *)fieldWithPrefix:(NSString *)prefix terminator:(NSString *)term {
  NSArray *fields = [self fieldsWithPrefix:prefix terminator:term];
  if (fields.count == 0) {
    return nil;
  } else {
    return [fields lastObject];
  }
}

@end



@implementation DoCoMoResultParser

@end
