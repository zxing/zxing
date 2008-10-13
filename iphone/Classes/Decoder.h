//
//  Decoder.h
//  ZXing
//
//  Created by Christian Brunschen on 31/03/2008.
//
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
#import "DecoderDelegate.h"

@interface Decoder : NSObject {
	UIImage *image;
	CGRect cropRect;
  UIImage *subsetImage;
  unsigned char *subsetData;
  size_t subsetWidth;
  size_t subsetHeight;
  size_t subsetBytesPerRow;
	id<DecoderDelegate> delegate;
}

@property(nonatomic, retain) UIImage *image;
@property(nonatomic, assign) CGRect cropRect;
@property(nonatomic, retain) UIImage *subsetImage;
@property(nonatomic, assign) unsigned char *subsetData;
@property(assign) size_t subsetWidth;
@property(assign) size_t subsetHeight;
@property(assign) size_t subsetBytesPerRow;
@property(nonatomic, assign) id<DecoderDelegate> delegate;

- (void) decodeImage:(UIImage *)image;
- (void) decodeImage:(UIImage *)image cropRect:(CGRect)cropRect;

@end
