//
//  ArrayAndStringCategories.h
//  ZXing
//
//  Created by Christian Brunschen on 25/06/2008.
/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <UIKit/UIKit.h>
#import "ResultParser.h"

@interface NSString (DoCoMoFieldParsing)
- (NSString *)backslashUnescaped;
- (NSArray *)fieldsWithPrefix:(NSString *)prefix;
- (NSArray *)fieldsWithPrefix:(NSString *)prefix terminator:(NSString *)term;
- (NSString *)fieldWithPrefix:(NSString *)prefix;
- (NSString *)fieldWithPrefix:(NSString *)prefix terminator:(NSString *)term;
- (NSString *)stringWithTrimmedWhitespace;
@end

@interface NSArray (DoCoMoStringArray)

- (NSArray*)stringArrayWithTrimmedWhitespace;
+ (NSArray*)arrayWithStringIfNotNil:(NSString *)string;

@end


// This works around the linker bug described here:
// http://developer.apple.com/library/mac/#qa/qa1490/_index.html
void ForceArrayAndStringCategoriesToLoad(void);
