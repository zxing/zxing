//
//  ProductParsedResult.4
//  ZXing
//
//  Created by George Nachman on 7/8/2011.
/*
 * Copyright 2011 ZXing authors
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
#import "ProductParsedResult.h"

@implementation ProductParsedResult

@synthesize productID;
@synthesize normalizedProductID;

- (id)initWithProductID:(NSString*)newProductID
    normalizedProductID:(NSString*)newNormalizedProductID {
    if ((self = [super init]) != nil) {
        self.productID = newProductID;
        self.normalizedProductID = newNormalizedProductID;
    }
    return self;
}

- (NSString *)stringForDisplay {
  return self.productID;
}

@end
