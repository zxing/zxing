//
//  MeCardParserTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "BusinessCardParsedResult.h"
#import "MeCardParser.h"

@interface MeCardParserTests : SenTestCase
@end

@implementation MeCardParserTests

- (void)testWellFormedMeCard {
  NSString *msg =
      @"MECARD:;"
      @"N:Name One;"
      @"SOUND:pronunciation;"
      @"TEL:1111111;"
      @"TEL:2222222;"
      @"TEL:3333333;"
      @"EMAIL:1@1.com;"
      @"EMAIL:2@2.com;"
      @"EMAIL:3@3.com;"
      @"NOTE:This is a note;"
      @"ADR:123 Fake St;"
      @"ADR:234 Fake St;"
      @"BDAY:19980904;"
      @"URL:http://example.com/;"
      @"ORG:Organization;"
      @"TITLE:Title;";

  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [MeCardParser parsedResultForString:msg
                                                format:BarcodeFormat_QR_CODE];
  STAssertEquals(1U, result.names.count, @"Wrong number of names %d",
                 result.names.count);
  STAssertTrue([[result.names objectAtIndex:0] isEqualToString:@"Name One"],
               @"Wrong Name one %@", [result.names objectAtIndex:0]);
  STAssertTrue([result.pronunciation isEqualToString:@"pronunciation"],
               @"Wrong pronunciation %@", result.pronunciation);
  STAssertEquals(3U, result.phoneNumbers.count,
                 @"Wrong number of phone numbers %d",
                 result.phoneNumbers.count);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:0] isEqualToString:@"1111111"],
      @"Wrong phone number 1 %@", [result.phoneNumbers objectAtIndex:0]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:1] isEqualToString:@"2222222"],
      @"Wrong phone number 2 %@", [result.phoneNumbers objectAtIndex:1]);
  STAssertTrue(
      [[result.phoneNumbers objectAtIndex:2] isEqualToString:@"3333333"],
      @"Wrong phone number 3 %@", [result.phoneNumbers objectAtIndex:2]);
  STAssertEquals(3U, result.emails.count,
                 @"Wrong number of emails %d", result.emails.count);
  STAssertTrue([[result.emails objectAtIndex:0] isEqualToString:@"1@1.com"],
               @"Wrong email 1 %@", [result.emails objectAtIndex:0]);
  STAssertTrue([[result.emails objectAtIndex:1] isEqualToString:@"2@2.com"],
               @"Wrong email 2 %@", [result.emails objectAtIndex:1]);
  STAssertTrue([[result.emails objectAtIndex:2] isEqualToString:@"3@3.com"],
               @"Wrong email 3 %@", [result.emails objectAtIndex:2]);
  STAssertTrue([result.note isEqualToString:@"This is a note"],
               @"Wrong note %@", result.note);
  STAssertEquals(2U, result.addresses.count, @"Wrong number of addresses %d",
                 result.addresses.count);
  STAssertTrue(
      [[result.addresses objectAtIndex:0] isEqualToString:@"123 Fake St"],
      @"Wrong address %@", [result.addresses objectAtIndex:0]);
  STAssertTrue(
      [[result.addresses objectAtIndex:1] isEqualToString:@"234 Fake St"],
      @"Wrong address %@", [result.addresses objectAtIndex:1]);
  STAssertTrue([result.birthday isEqualToString:@"19980904"],
               @"Wrong birthday %@", result.birthday);
  STAssertTrue([result.url isEqualToString:@"http://example.com/"],
               @"Wrong url %@", result.url);
  STAssertTrue([result.organization isEqualToString:@"Organization"],
               @"Wrong organization %@", result.organization);
  STAssertTrue([result.jobTitle isEqualToString:@"Title"],
               @"Wrong job title %@", result.jobTitle);
}

- (void)testMalformedMeCard {
  NSString *msg =
      @"N:Name One;"
      @"SOUND:pronunciation;"
      @"TEL1:1111111;"
      @"TEL2:2222222;"
      @"TEL3:3333333;"
      @"EMAIL1:1@1.com;"
      @"EMAIL2:2@2.com;"
      @"EMAIL3:3@3.com;"
      @"NOTE:This is a note;"
      @"ADR1:123 Fake St;"
      @"ADR2:234 Fake St;"
      @"BDAY:19980904;"
      @"URL:http://example.com/;"
      @"ORG:Organization;"
      @"TITLE:Title;";

  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [MeCardParser parsedResultForString:msg
                                         format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched.");
}

@end
