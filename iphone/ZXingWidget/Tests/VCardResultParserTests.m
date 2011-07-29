//
//  VCardResultParserTests.m
//  ZXingWidget
//
//  Created by George Nachman on 7/26/11.
//  Copyright 2011 ZXing Authors. All rights reserved.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "VCardResultParser.h"
#import "BusinessCardParsedResult.h"

@interface VCardResultParserTests : SenTestCase
@end

@implementation VCardResultParserTests

- (void)testVanillaVCard {
  NSString* msg =
      @"BEGIN:VCARD\n"
      @"N:Kennedy;Steve\n"
      @"TEL:+44 (0)7775 755503\n"
      @"ADR;HOME:;;Flat 2, 43 Howitt Road, Belsize Park;London;;NW34LU;UK\n"
      @"ORG:NetTek Ltd;\n"
      @"TITLE:Consultant\n"
      @"EMAIL:steve@nettek.co.uk\n"
      @"URL:www.nettek.co.uk\n"
      @"EMAIL;IM:MSN:steve@gbnet.net\n"
      @"NOTE:Testing 1 2 3\n"
      @"BDAY:19611105\n"
      @"END:VCARD";
  BusinessCardParsedResult* b = (BusinessCardParsedResult *)
      [VCardResultParser parsedResultForString:msg
                                        format:0];

  STAssertEquals(1U, b.names.count, @"Should have exactly one name");
  STAssertTrue([[b.names objectAtIndex:0] isEqualToString:@"Steve Kennedy"],
               @"Wrong name %@", b.names);
  STAssertEquals(1U, b.phoneNumbers.count,
                 @"Should have exactly one phone number");
  STAssertTrue([[b.phoneNumbers objectAtIndex:0] isEqualToString:
                    @"+44 (0)7775 755503"],
               @"Wrong phone number %@", [b.phoneNumbers objectAtIndex:0]);
  STAssertEquals(1U, b.addresses.count, @"Should have exactly one address");
  STAssertTrue([[b.addresses objectAtIndex:0] isEqualToString:
                    @"Flat 2, 43 Howitt Road, Belsize Park London  NW34LU UK"],
               @"Wrong address %@", [b.addresses objectAtIndex:0]);
  STAssertTrue([b.organization isEqualToString:@"NetTek Ltd;"],
               @"Wrong organization %@", b.organization);
  STAssertTrue([b.jobTitle isEqualToString:@"Consultant"],
               @"Wrong job title %@", b.jobTitle);
  STAssertEquals(2U, b.emails.count,
                 @"Wrong number of emails %d", b.emails.count);
  STAssertTrue([[b.emails objectAtIndex:0] isEqualToString:
                    @"steve@nettek.co.uk"],
               @"Wrong first email %@", [b.emails objectAtIndex:0]);
  STAssertTrue([b.note isEqualToString:@"Testing 1 2 3"],
               @"Wrong note %@", b.note);
  STAssertTrue([b.url isEqualToString:@"www.nettek.co.uk"],
               @"Wrong url %@", b.url);
  STAssertTrue([[b.emails objectAtIndex:1] isEqualToString:
                  @"MSN:steve@gbnet.net"],
               @"Wrong second email %@", [b.emails objectAtIndex:1]);
  STAssertTrue([b.birthday isEqualToString:@"19611105"],
               @"Wrong birthday %@", b.birthday);
}

- (void)testBrokenVCard {
  NSString *msg = @"Blah blah blah";
  BusinessCardParsedResult* b = (BusinessCardParsedResult *)
      [VCardResultParser parsedResultForString:msg
                                        format:0];

  STAssertTrue(b == nil, @"Bogus string parsed");
}

- (void)testQuotedPrintableVCard {
  NSString *msg =
      @"BEGIN:VCARD\n"
      @"FN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Kennedy;=C5=9Bteve\n"
      @"TEL:+44 (0)7775 755503\n"
      @"ADR;HOME:;;Flat 2, 43 Howitt Road, Belsize Park;London;;NW34LU;UK\n"
      @"ORG:NetTek Ltd;\n"
      @"TITLE:Consultant\n"
      @"EMAIL:steve@nettek.co.uk\n"
      @"URL:www.nettek.co.uk\n"
      @"EMAIL;IM:MSN:steve@gbnet.net\n"
      @"NOTE:Testing 1 2 3\n"
      @"BDAY:19611105\n"
      @"END:VCARD";
  BusinessCardParsedResult* b = (BusinessCardParsedResult *)
      [VCardResultParser parsedResultForString:msg
                                        format:0];
  STAssertEquals(1U, b.names.count,
                 @"Wrong number of names %d", b.names.count);
  STAssertTrue([[b.names objectAtIndex:0] isEqualToString:@"śteve Kennedy"],
               @"Wrong name %@", [b.names objectAtIndex:0]);
}

- (void)testExcessNewlineVCard {
  NSString *msg =
      @"BEGIN:VCARD\n"
      @"FN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Kennedy;=C5=9Bteve\n"
      @"TEL:+44 (0)7775 755503\n"
      @"ADR;HOME:;;Flat 2, 43 Howitt Road, Belsize Park;London;;NW34LU;UK\n"
      @"ORG:NetTek Ltd;\t\n"  // note tab at end
      @"TITLE:Consultant\n"
      @"EMAIL:steve@nettek.co.uk\n"
      @"URL:www.nettek.co.uk \n"  // note trailing space
      @"EMAIL;IM:MSN:steve@gbnet.net\n"
      @"NOTE:Testing 1 2 3\r\n"  // note dos newline
      @"BDAY:19611105\n";
  BusinessCardParsedResult* b = (BusinessCardParsedResult *)
      [VCardResultParser parsedResultForString:msg
                                      format:0];
  STAssertTrue([b.birthday isEqualToString:@"19611105"],
               @"Wrong birthday %@", b.birthday);
  STAssertTrue([b.jobTitle isEqualToString:@"Consultant"],
               @"Wrong job title %@", b.jobTitle);
  STAssertTrue([b.url isEqualToString:@"www.nettek.co.uk"],
               @"Wrong url %@", b.url);
  STAssertTrue([b.note isEqualToString:@"Testing 1 2 3"],
               @"Wrong note %@", b.note);
}

- (void)testStrayKeywordVCard {
  NSString *msg =
      @"BEGIN:VCARD\n"
      @"FN;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:Kennedy;=C5=9Bteve\n"
      @"TEL:+44 (0)7775 755503\n"
      @"ADR;HOME:;;Flat ORG TITLE 2, 43 Howitt Road, Belsize Park;London;;"
          @"NW34LU;UK\n"
      @"ORG:NetTek Ltd;\t\n"  // note tab at end
      @"TITLE:Consultant\n"
      @"EMAIL:steve@nettek.co.uk\n"
      @"URL:www.nettek.co.uk \n"  // note trailing space
      @"EMAIL;IM:MSN:steve@gbnet.net\n"
      @"NOTE:Testing 1 2 3\r\n"  // note dos newline
      @"BDAY:19611105\n";
  BusinessCardParsedResult* b = (BusinessCardParsedResult *)
      [VCardResultParser parsedResultForString:msg
                                        format:0];
  STAssertTrue([b.jobTitle isEqualToString:@"Consultant"],
               @"Wrong job title %@", b.jobTitle);
  STAssertTrue([b.organization isEqualToString:@"NetTek Ltd;"],
               @"Wrong organization %@", b.organization);
}

@end
