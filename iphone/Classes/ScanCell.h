//
//  ScanCell.h
//  ZXing
//
//  Created by Christian Brunschen on 30/06/2008.
//  Copyright 2008 Google Inc. All rights reserved.
//

#import <UIKit/UIKit.h>

@class Scan;
@class ParsedResult;

@interface ScanCell : UITableViewCell {
  Scan *scan;
  ParsedResult *result;
  UIImageView *imageView;
  UILabel *textView;
  UILabel *dateView;
  UILabel *timeView;
}

@property (nonatomic, retain) Scan *scan;
@property (nonatomic, retain) UIImageView *imageView;
@property (nonatomic, retain) UILabel *textView;
@property (nonatomic, retain) UILabel *dateView;
@property (nonatomic, retain) UILabel *timeView;

@end
