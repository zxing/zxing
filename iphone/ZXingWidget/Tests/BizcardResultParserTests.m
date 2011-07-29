//
//  BizcardTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "BizcardResultParser.h"
#import "BusinessCardParsedResult.h"

@interface BizcardResultParserTests : SenTestCase
@end

@implementation BizcardResultParserTests

- (void)testWellFormedBizcard {
  NSString *msg =
      @"BIZCARD:;"
      @"N:Firstname;"
      @"X:Lastname;"
      @"T:Title;"
      @"C:Org;"
      @"A:Addr 1;"
      @"A:Addr 2;"
      @"B:1111111;"
      @"M:2222222;"
      @"F:3333333;"
      @"E:1@1.com;";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
  [BizcardResultParser parsedResultForString:msg
                                      format:BarcodeFormat_QR_CODE];
  STAssertEquals(1U, result.names.count, @"Wrong number of names %d",
                 result.names.count);
  STAssertTrue(
      [[result.names objectAtIndex:0] isEqualToString:@"Firstname Lastname"],
      @"Name one is %@", [result.names objectAtIndex:0]);
  STAssertTrue([result.jobTitle isEqualToString:@"Title"],
               @"Title is %@", result.jobTitle);
  STAssertTrue([result.organization isEqualToString:@"Org"],
               @"Organization is %@", result.organization);
  STAssertEquals(2U, result.addresses.count, @"Wrong number of addresses %d",
                 result.addresses.count);
  STAssertTrue([[result.addresses objectAtIndex:0] isEqualToString:@"Addr 1"],
               @"Wrong address %@", [result.addresses objectAtIndex:0]);
  STAssertTrue([[result.addresses objectAtIndex:1] isEqualToString:@"Addr 2"],
               @"Wrong address %@", [result.addresses objectAtIndex:0]);
  STAssertEquals(3U, result.phoneNumbers.count,
                 @"Wrong number of phone numbers %d",
                 result.phoneNumbers.count);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:0] isEqualToString:@"1111111"],
      @"Phone number 1 is %@", [result.phoneNumbers objectAtIndex:0]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:1] isEqualToString:@"2222222"],
      @"Phone number 2 is %@", [result.phoneNumbers objectAtIndex:1]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:2] isEqualToString:@"3333333"],
      @"Phone number 3 is %@", [result.phoneNumbers objectAtIndex:2]);
  STAssertEquals(1U, result.emails.count,
                 @"Wrong number of emails %d", result.emails.count);
  STAssertTrue([[result.emails objectAtIndex:0] isEqualToString:@"1@1.com"],
               @"Email 1 is %@", [result.emails objectAtIndex:0]);
}

- (void)testExtraWhitespaceBizcard {
  NSString *msg =
      @"BIZCARD:;"
      @"N:Firstname ;"
      @"X:Lastname\r;"
      @"T:Title\n;"
      @"C:Org\r\n;"
      @"A: Addr 1;"
      @"A:\rAddr 2;"
      @"B:\n1111111;"
      @"M:\r\n2222222;"
      @"F:\t3333333;"
      @"E:1@1.com\t;";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
  [BizcardResultParser parsedResultForString:msg
                                      format:BarcodeFormat_QR_CODE];
  STAssertEquals(1U, result.names.count, @"Wrong number of names %d",
                 result.names.count);
  STAssertTrue(
      [[result.names objectAtIndex:0] isEqualToString:@"Firstname Lastname"],
      @"Name one is %@", [result.names objectAtIndex:0]);
  STAssertTrue([result.jobTitle isEqualToString:@"Title"],
               @"Title is %@", result.jobTitle);
  STAssertTrue([result.organization isEqualToString:@"Org"],
               @"Organization is %@", result.organization);
  STAssertEquals(2U, result.addresses.count, @"Wrong number of addresses %d",
                 result.addresses.count);
  STAssertTrue([[result.addresses objectAtIndex:0] isEqualToString:@"Addr 1"],
               @"Wrong address %@", [result.addresses objectAtIndex:0]);
  STAssertTrue([[result.addresses objectAtIndex:1] isEqualToString:@"Addr 2"],
               @"Wrong address %@", [result.addresses objectAtIndex:0]);
  STAssertEquals(3U, result.phoneNumbers.count,
                 @"Wrong number of phone numbers %d",
                 result.phoneNumbers.count);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:0] isEqualToString:@"1111111"],
      @"Phone number 1 is %@", [result.phoneNumbers objectAtIndex:0]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:1] isEqualToString:@"2222222"],
      @"Phone number 2 is %@", [result.phoneNumbers objectAtIndex:1]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:2] isEqualToString:@"3333333"],
      @"Phone number 3 is %@", [result.phoneNumbers objectAtIndex:2]);
  STAssertEquals(1U, result.emails.count,
                 @"Wrong number of emails %d", result.emails.count);
  STAssertTrue([[result.emails objectAtIndex:0] isEqualToString:@"1@1.com"],
               @"Email 1 is %@", [result.emails objectAtIndex:0]);
}

- (void)testMalformedBizcard {
  NSString *msg =
      @"N:Name One;"
      @"X:Name Two;"
      @"T:Title;"
      @"C:Org;"
      @"A:Addr 1;"
      @"A:Addr 2;"
      @"B:1111111;"
      @"M:2222222;"
      @"F:3333333;"
      @"E:1@1.com;";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [BizcardResultParser parsedResultForString:msg
                                                  format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched");
}

@end
