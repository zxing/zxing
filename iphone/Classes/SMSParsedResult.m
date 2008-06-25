//
//  SMSParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import "SMSParsedResult.h"
#import "SMSAction.h"

@implementation SMSParsedResult

@synthesize number;
@synthesize body;

- initWithNumber:(NSString *)n body:(NSString *)b {
  if ((self = [super init]) != nil) {
    self.number = n;
    self.body = b;
  }
  return self;
}

- (NSString *)stringForDisplay {
  if (self.body) {
    return [NSString stringWithFormat:@"%@\n%@", self.number, self.body];
  }
  return self.number;
}


+ (NSString *)typeName {
  return @"SMS";
}

- (UIImage *)icon {
  return [UIImage imageNamed:@"sms.png"];
}

- (void)populateActions { 
  [actions addObject:[SMSAction actionWithNumber:self.number body:self.body]];
}

- (void) dealloc {
  [number release];
  [body release];
  [super dealloc];
}


@end
