//
//  URIResultParser.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "URLResultParser.h"
#import "URIParsedResult.h"

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


#define PREFIX @"URL:"

@implementation URLResultParser

+ (ParsedResult *)parsedResultForString:(NSString *)s {
  NSRange prefixRange = [s rangeOfString:PREFIX options:NSCaseInsensitiveSearch];
  if (prefixRange.location == 0) {
    int restStart = prefixRange.location + prefixRange.length;
    return [[[URIParsedResult alloc] initWithURLString:[[s substringFromIndex:restStart] massagedURLString]]
            autorelease];
  }
  
  if ([s looksLikeAURI]) {
    NSString *massaged = [s massagedURLString];
    NSURL *url = [NSURL URLWithString:massaged];
    if (url != nil) {
      return [[[URIParsedResult alloc] initWithURLString:massaged URL:url] autorelease];
    }
  }
  
  return nil;
}


@end
