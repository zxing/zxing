//
//  SMTPResultParserTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "SMTPResultParser.h"
#import "EmailParsedResult.h"

@interface SMTPResultParserTests : SenTestCase
@end

@implementation SMTPResultParserTests

- (void)testWellFormedSMTPResultParser {
  NSString *msg =
      @"smtp:user@example.com:the subject:the body";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertTrue([result.subject isEqualToString:@"the subject"],
               @"Wrong subject %@", result.subject);
  STAssertTrue([result.body isEqualToString:@"the body"],
               @"Wrong body %@", result.body);
}

- (void)testWellFormedNoBodySMTPResultParser {
  NSString *msg =
      @"smtp:user@example.com:the subject";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertTrue([result.subject isEqualToString:@"the subject"],
               @"Wrong subject %@", result.subject);
  STAssertNil(result.body,
               @"Wrong body %@", result.body);
}

- (void)testWellFormedNoSubjectSMTPResultParser {
  NSString *msg = @"smtp:user@example.com";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertNil(result.subject,
               @"Wrong subject %@", result.subject);
  STAssertNil(result.body,
               @"Wrong body %@", result.body);
}

- (void)testMalformedHeaderOnlySMTPResultParser {
  NSString *msg =
      @"smtp:";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedEmptySMTPResultParser {
  NSString *msg =
      @"";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedSMTPResultParser {
  NSString *msg =
      @"I like traffic lights";
  EmailParsedResult *result = (EmailParsedResult *)
      [SMTPResultParser parsedResultForString:msg
                                       format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}
@end
