//
//  ISBNResultParserTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "ISBNResultParser.h"
#import "ISBNParsedResult.h"

@interface ISBNResultParserTests : SenTestCase
@end

@implementation ISBNResultParserTests

- (void)testWellFormedISBNResultParser {
  NSString *msg = @"9781234567890";
  ISBNParsedResult *result = (ISBNParsedResult *)
      [ISBNResultParser parsedResultForString:msg
                                                   format:BarcodeFormat_EAN_13];
  STAssertTrue([result.value isEqualToString:msg],
               @"Wrong value %@", result.value);
}

- (void)testMalformedNumDigitsISBNResultParser {
  NSString *msg = @"1234";
  ISBNParsedResult *result = (ISBNParsedResult *)
      [ISBNResultParser parsedResultForString:msg
                                                   format:BarcodeFormat_EAN_13];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedPrefixISBNResultParser {
  NSString *msg = @"9991234567890";
  ISBNParsedResult *result = (ISBNParsedResult *)
      [ISBNResultParser parsedResultForString:msg
                                                   format:BarcodeFormat_EAN_13];
  STAssertNil(result, @"Bogus message parsed");
}

- (void)testMalformedISBNResultParser {
  NSString *msg = @"I like traffic lights";
  ISBNParsedResult *result = (ISBNParsedResult *)
      [ISBNResultParser parsedResultForString:msg
                                                   format:BarcodeFormat_EAN_13];
  STAssertNil(result, @"Bogus message parsed");
}

@end
