//
//  DoCoMoResultParser.h
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "ResultParser.h"

@interface NSString (DoCoMoFieldParsing) 
- (NSString *)backslashUnescaped;
- (NSArray *)fieldsWithPrefix:(NSString *)prefix;
- (NSArray *)fieldsWithPrefix:(NSString *)prefix terminator:(NSString *)term;
- (NSString *)fieldWithPrefix:(NSString *)prefix;
- (NSString *)fieldWithPrefix:(NSString *)prefix terminator:(NSString *)term;
@end


@interface DoCoMoResultParser : ResultParser {

}

@end
