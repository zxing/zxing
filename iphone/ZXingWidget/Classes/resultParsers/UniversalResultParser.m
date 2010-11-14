//
//  UniversalResultParser.m
//  ZXingWidget
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "UniversalResultParser.h"
#import "SMSTOResultParser.h"
#import "URLResultParser.h"
#import "TelResultParser.h"
#import "URLTOResultParser.h"
#import "SMSResultParser.h"
#import "PlainEmailResultParser.h"
#import "MeCardParser.h"
#import "EmailDoCoMoResultParser.h"
#import "BookmarkDoCoMoResultParser.h"
#import "GeoResultParser.h"
#import "TextResultParser.h"

@implementation UniversalResultParser
@synthesize parsers;

- (void)addParserClass:(Class)klass {
  [self.parsers addObject:klass];           
}

- (id) initWithDefaultParsers {
  NSMutableArray *set = [[NSMutableArray alloc] initWithCapacity:11];
  self.parsers = set;
  [set release];
  
  [self addParserClass:[SMSResultParser class]];
  [self addParserClass:[TelResultParser class]];
  [self addParserClass:[SMSTOResultParser class]];
  [self addParserClass:[MeCardParser class]];
  [self addParserClass:[URLResultParser class]];
  [self addParserClass:[URLTOResultParser class]];
  [self addParserClass:[PlainEmailResultParser class]];
  [self addParserClass:[EmailDoCoMoResultParser class]];
  [self addParserClass:[BookmarkDoCoMoResultParser class]];
  [self addParserClass:[GeoResultParser class]];
  [self addParserClass:[TextResultParser class]];
  return self;
}

- (ParsedResult *)resultForString:(NSString *)s {
#ifdef DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif
  for (Class c in parsers) {
#ifdef DEBUG
    NSLog(@"trying %@", NSStringFromClass(c));
#endif
    ParsedResult *result = [c parsedResultForString:s];
    if (result != nil) {
#ifdef DEBUG
      NSLog(@"parsed as %@ %@", NSStringFromClass([result class]), result);
#endif
      return result;
    }
  }
  return nil;
}


-(void)dealloc {
  [parsers release];
  [super dealloc];
}
@end
