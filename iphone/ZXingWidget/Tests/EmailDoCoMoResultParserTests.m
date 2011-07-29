//
//  EmailDoCoMoTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "EmailDoCoMoResultParser.h"
#import "EmailParsedResult.h"

@interface EmailDoCoMoResultParserTests : SenTestCase
@end

@implementation EmailDoCoMoResultParserTests

- (void)testWellFormedEmailDoCoMo {
  NSString *msg =
      @"MATMSG:;"
      @"TO:addressee@example.com;"
      @"SUB:subject;"
      @"BODY:The body\nThe end;";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailDoCoMoResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"addressee@example.com"],
               @"Wrong to: is %@", result.to);
  STAssertTrue([result.subject isEqualToString:@"subject"],
               @"Wrong subject %@", result.subject);
  STAssertTrue([result.body isEqualToString:@"The body\nThe end"],
               @"Wrong body %@", result.subject);
}

- (void)testMalformedEmailDoCoMo {
  NSString *msg =
      @"TO:addressee@example.com;"
      @"SUB:subject;"
      @"BODY:The body\nThe end;";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailDoCoMoResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched.");
}


@end
