//
//  SMSParsedResult.h
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "ParsedResult.h"


@interface SMSParsedResult : ParsedResult {
  NSString *number;
  NSString *body;
}

@property (nonatomic, copy) NSString *number;
@property (nonatomic, copy) NSString *body;

- initWithNumber:(NSString *)n body:(NSString *)b;

@end
