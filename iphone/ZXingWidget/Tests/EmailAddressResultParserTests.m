//
//  EmailAddressResultParserTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "EmailAddressResultParserResultParser.h"
#import "URIParsedResult.h"

@interface EmailAddressResultParserTests : SenTestCase
@end

@implementation EmailAddressResultParserTests

- (void)testWellFormedEmailAddressResultParser {
  NSString *msg =
      @"mailto:user@example.com?subject=the+subject&body=the%20body";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertTrue([result.subject isEqualToString:@"the subject"],
               @"Wrong subject %@", result.subject);
  STAssertTrue([result.body isEqualToString:@"the body"],
               @"Wrong body %@", result.body);
}

- (void)testWellFormedEmailAddressWithToResultParser {
  NSString *msg =
      @"mailto:bogus@example.com?to=user@exampe.com&subject=the+subject&"
      @"body=the%20body";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertTrue([result.subject isEqualToString:@"the subject"],
               @"Wrong subject %@", result.subject);
  STAssertTrue([result.body isEqualToString:@"the body"],
               @"Wrong body %@", result.body);
}

- (void)testWellFormedEmailAddressNoQueryResultParser {
  NSString *msg =
      @"mailto:user@example.com";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertNil(result.subject, @"Wrong subject %@", result.subject);
  STAssertNil(result.body, @"Wrong body %@", result.body);
}

- (void)testWellFormedEmailAddressNoHostResultParser {
  NSString *msg =
      @"mailto:user";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user"],
               @"Wrong to %@", result.to);
  STAssertNil(result.subject, @"Wrong subject %@", result.subject);
  STAssertNil(result.body, @"Wrong body %@", result.body);
}

- (void)testSimpleEmailAddressResultParser {
  NSString *msg =
      @"user@example.com";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.to isEqualToString:@"user@example.com"],
               @"Wrong to %@", result.to);
  STAssertNil(result.subject,
              @"Wrong subject %@", result.subject);
  STAssertNil(result.body,
              @"Wrong body %@", result.body);
}

- (void)testMalformedEmailAddressResultParser {
  NSString *msg =
      @"I like traffic lights";
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedDoubleAtEmailAddressResultParser {
  NSString *msg =
      @"me@here@there"
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedBogusCharEmailAddressResultParser {
  NSString *msg =
      @"me(yeah)me@google.com"
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedJustMailtoEmailAddressResultParser {
  NSString *msg =
      @"mailto:"
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedEmptyEmailAddressResultParser {
  NSString *msg =
      @""
  EmailParsedResult *result = (EmailParsedResult *)
      [EmailAddressResultParserResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus message parsed");
}
@end
