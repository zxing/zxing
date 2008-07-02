//
//  ScannedImageView.h
//  ZXing
//
//  Created by Christian Brunschen on 01/07/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import <UIKit/UIKit.h>


@interface ScannedImageView : UIView {
  UIImage *image;
  NSMutableArray *resultPoints;
}

@property (nonatomic, retain) UIImage *image;

- (void) addResultPoint:(CGPoint)p;

@end
