//
//  BookmarkDoCoMoTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/27/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "BookmarkDoCoMoResultParser.h"
#import "URIParsedResult.h"

@interface BookmarkDoCoMoTests : SenTestCase
@end

@implementation BookmarkDoCoMoTests

- (void)testWellFormedBookmarkDoCoMo {
  NSString *msg =
      @"MEBKM:;"
      @"URL:http://www.example.com/;"
      @"TITLE:The Title;";
  URIParsedResult *result = (URIParsedResult *)
      [BookmarkDoCoMoResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertTrue([result.urlString isEqualToString:@"http://www.example.com/"],
               @"Wrong URL string %@", result.urlString);
  STAssertTrue([[result.URL host] isEqualToString:@"www.example.com"],
               @"Wrong URL host %@", [result.URL host]);
  STAssertTrue([[result.URL path] isEqualToString:@"/"],
               @"Wrong URL path %@", [result.URL path]);
  STAssertTrue([[result.URL scheme] isEqualToString:@"http"],
               @"Wrong URL scheme %@", [result.URL scheme]);
  STAssertTrue([result.title isEqualToString:@"The Title"],
               @"Wrong title %@", result.title);
}

- (void)testMalformedBookmarkDoCoMo {
  NSString *msg =
    @"URL:http://www.example.com/;"
    @"TITLE:The Title";
  URIParsedResult *result = (URIParsedResult *)
      [BookmarkDoCoMoResultParser parsedResultForString:msg
                                                 format:BarcodeFormat_QR_CODE];
  STAssertNil(result, @"Bogus string matched.");
}


@end
