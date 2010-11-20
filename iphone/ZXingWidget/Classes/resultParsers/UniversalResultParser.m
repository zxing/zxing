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
static NSMutableArray *sTheResultParsers = nil;
//@synthesize parsers;

//static NSMutableSet *sResultParsers = nil;
+(void) load {
  [self initWithDefaultParsers];
}

+ (void)addParserClass:(Class)klass {
  [sTheResultParsers addObject:klass];           
}

+ (void) initWithDefaultParsers {
  // NSMutableArray *set = [[NSMutableArray alloc] initWithCapacity:11];
  // self.parsers = set;
  // [set release];
  // 
  
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  @synchronized(self) {
    if (!sTheResultParsers) {
      sTheResultParsers = [[NSMutableArray alloc] init];
    }
  }
  [pool release];
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
}

+ (ParsedResult *)parsedResultForString:(NSString *)s {
#ifdef DEBUG
  NSLog(@"parsing result:\n<<<\n%@\n>>>\n", s);
#endif
  for (Class c in sTheResultParsers) {
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
  [super dealloc];
}
@end
