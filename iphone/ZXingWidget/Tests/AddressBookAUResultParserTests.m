//
//  AddressBookAUTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "AddressBookAUResultParser.h"
#import "BusinessCardParsedResult.h"

@interface AddressBookAUResultParserTests : SenTestCase
@end
  
@implementation AddressBookAUResultParserTests

- (void)testWellFormedAddressBookAU {
  NSString *msg =
      @"NAME1:Name One\r"
      @"NAME2:Name Two\r"
      @"NAME3:Name Three\r"  // ignored
      @"TEL1:1111111\r"
      @"TEL2:2222222\r"
      @"TEL3:3333333\r"
      @"TEL4:4444444\r"  // ignored
      @"MAIL1:1@1.com\r"
      @"MAIL2:2@2.com\r"
      @"MAIL3:3@3.com\r"
      @"MAIL4:4@4.com\r"  // ignored
      @"MEMORY:This is a note\r"
      @"ADD:123 Fake St\r\n";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [AddressBookAUResultParser parsedResultForString:msg
                                                format:BarcodeFormat_QR_CODE];
  STAssertEquals(1U, result.names.count, @"Wrong number of names %d",
                 result.names.count);
  STAssertTrue([[result.names objectAtIndex:0] isEqualToString:@"Name One"],
               @"Wrong name one %@", [result.names objectAtIndex:0]);
  STAssertTrue([result.pronunciation isEqualToString:@"Name Two"],
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
  STAssertEquals(1U, result.addresses.count, @"Wrong number of addresses %d",
                 result.addresses.count);
  STAssertTrue(
      [[result.addresses objectAtIndex:0] isEqualToString:@"123 Fake St"],
               @"Wrong address %@", [result.addresses objectAtIndex:0]);
}

- (void)testMissingNewlineAddressBookAU {
  NSString *msg =
      @"NAME1:Name One\r"
      @"NAME2:Name Two\r"
      @"NAME3:Name Three\r"
      @"TEL1:1111111\r"
      @"TEL2:2222222\r"
      @"TEL3:3333333\r"
      @"TEL4:4444444\r"
      @"MAIL1:1@1.com\r"
      @"MAIL2:2@2.com\r"
      @"MAIL3:3@3.com\r"
      @"MAIL4:4@4.com\r"
      @"MEMORY:This is a note\r"
      @"ADD:123 Fake St\r";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [AddressBookAUResultParser parsedResultForString:msg
                                                format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched");
}

- (void)testMalformedAddressBookAU {
  NSString *msg =
      @"NAME1:Name One\r"
      @"NAME2:Name Two\r"
      @"NAME3:Name Three\r"  // ignored
      @"TEL1:1111111\r"
      @"TEL2:2222222\r"
      @"TEL3:3333333\r"
      @"TEL4:4444444\r"  // ignored
      @"MAIL1:1@1.com\r"
      @"MAIL2:2@2.com\r"
      @"MAIL3:3@3.com\r"
      @"MAIL4:4@4.com\r"  // ignored
      @"ADD:123 Fake St";
  BusinessCardParsedResult *result = (BusinessCardParsedResult *)
      [AddressBookAUResultParser parsedResultForString:msg
                                                format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched");
}

@end
