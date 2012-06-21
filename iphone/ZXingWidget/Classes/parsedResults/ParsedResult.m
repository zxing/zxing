//
//  ParsedResult.m
//  ZXing
//
//  Created by Christian Brunschen on 22/05/2008.
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

#import "ParsedResult.h"

#import "TextParsedResult.h"
#import "TelParsedResult.h"
#import "EmailParsedResult.h"
#import "BusinessCardParsedResult.h"
#import "URIParsedResult.h"
#import "GeoParsedResult.h"

#import "UIKit/UIStringDrawing.h"
#import <math.h>

@implementation ParsedResult

static NSMutableDictionary *iconsByClass = nil;

- (NSString *)stringForDisplay {
  return @"{none}";
}

#define ICON_SIZE 40
#define ICON_INSIDE 36

+ (NSString *)typeName {
  return NSStringFromClass(self);
}

+ (UIImage *)icon {
  if (iconsByClass == nil) {
    iconsByClass = [[NSMutableDictionary alloc] initWithCapacity:16];
  }
  UIImage *icon = [iconsByClass objectForKey:NSStringFromClass([self class])];
  if (icon == nil) {
    UIGraphicsBeginImageContext(CGSizeMake(ICON_SIZE, ICON_SIZE));
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    
    [[UIColor lightGrayColor] set];
    UIRectFill(CGRectMake(0, 0, ICON_SIZE, ICON_SIZE));
    
    [[UIColor blackColor] set];
    NSString *s = NSStringFromClass([self class]);
    UIFont *font = [UIFont systemFontOfSize:16];
    CGSize stringSize = [s sizeWithFont:font];
    float xScale = fminf(1.0, ICON_INSIDE / stringSize.width);
    float yScale = fminf(1.0, ICON_INSIDE / stringSize.height);
    
    CGContextTranslateCTM(ctx, (ICON_SIZE / 2), (ICON_SIZE / 2));
    CGContextRotateCTM(ctx, -M_PI / 6.0);
    CGContextScaleCTM(ctx, xScale, yScale);
    CGContextTranslateCTM(ctx, 
                          -(stringSize.width)/2.0, 
                          -(stringSize.height)/2.0);
    
    [s drawAtPoint:CGPointMake(0, 0) withFont:font];
    
    // N.B.: I think this is overretained but it's static so doesn't matter and
    // I don't want to test right now. (smp)

    icon = [UIGraphicsGetImageFromCurrentImageContext() retain];
    [iconsByClass setObject:icon forKey:NSStringFromClass([self class])];
    UIGraphicsEndImageContext();
  }
  return icon;
}

- (UIImage *)icon {
  return [[self class] icon];
}

- (NSArray *)actions {
  if (!actions) {
    actions = [[NSMutableArray alloc] init];
    [self populateActions];
  }
  return actions;
}

- (void) populateActions {
}

- (void) dealloc {
  [actions release];
  [super dealloc];
}

@end
