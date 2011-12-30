//
//  SMSAction.h
//  ZXing
//
//  Created by Christian Brunschen on 16/06/2008.
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
#import "CallAction.h"

@interface SMSAction : CallAction {
  NSString *body;
}

@property (nonatomic, copy) NSString *body;

+ (id)actionWithNumber:(NSString *)number body:(NSString *)body;
+ (id)actionWithNumber:(NSString *)number;

@end
