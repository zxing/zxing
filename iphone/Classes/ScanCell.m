//
//  ScanCell.m
//  ZXing
//
//  Created by Christian Brunschen on 30/06/2008.
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

#import "ScanCell.h"
#import "Scan.h"
#import "ParsedResult.h"
#import "ResultParser.h"

static NSDateFormatter *_makeDateFormatter(NSDateFormatterStyle dateStyle,
                                           NSDateFormatterStyle timeStyle) {
  NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
  [dateFormatter setDateStyle:dateStyle];
  [dateFormatter setTimeStyle:timeStyle];
  return dateFormatter;
}

static NSString *_dateString(NSDate *date) {
  static NSDateFormatter *dateFormatter = nil;
  if (!dateFormatter) {
    dateFormatter = 
  _makeDateFormatter(NSDateFormatterShortStyle, NSDateFormatterNoStyle);
  }
  return [dateFormatter stringFromDate:date];
}

static NSString *_timeString(NSDate *date) {
  static NSDateFormatter *timeFormatter = nil;
  if (!timeFormatter) {
    timeFormatter =
  
  _makeDateFormatter(NSDateFormatterNoStyle, NSDateFormatterShortStyle);
  }
  return [timeFormatter stringFromDate:date];
}

#define VIEW_PADDING        2.0
#define IMAGE_SIZE          40.0
#define EDITING_INSET       10.0
#define CONTENT_HEIGHT      (IMAGE_SIZE + 2.0 *  VIEW_PADDING)
#define DATE_TIME_WIDTH     50.0

@implementation ScanCell

@synthesize imageView;
@synthesize textView;
@synthesize dateView;
@synthesize timeView;


- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier {
  if ((self = [super initWithStyle:style reuseIdentifier:reuseIdentifier])) {
    imageView = [[UIImageView alloc] initWithFrame:CGRectZero];
    imageView.contentMode = UIViewContentModeCenter;
    [self.contentView addSubview:imageView];
    
    textView = [[UILabel alloc] initWithFrame:CGRectZero];
    textView.font = [UIFont boldSystemFontOfSize:[UIFont systemFontSize]];
    textView.textAlignment = UITextAlignmentLeft;
    textView.textColor = [UIColor blackColor];
    [self.contentView addSubview:textView];
    
    dateView = [[UILabel alloc] initWithFrame:CGRectZero];
    dateView.font = [UIFont systemFontOfSize:(2 * [UIFont systemFontSize]) / 3];
    dateView.textAlignment = UITextAlignmentRight;
    dateView.textColor = [UIColor grayColor];
    [self.contentView addSubview:dateView];

    timeView = [[UILabel alloc] initWithFrame:CGRectZero];
    timeView.font = [UIFont systemFontOfSize:(2 * [UIFont systemFontSize]) / 3];
    timeView.textAlignment = UITextAlignmentRight;
    timeView.textColor = [UIColor grayColor];
    [self.contentView addSubview:timeView];
  }
  return self;
}

- (CGRect) _imageViewFrame {
  CGRect frame = CGRectMake(VIEW_PADDING, VIEW_PADDING, IMAGE_SIZE, IMAGE_SIZE);
  if (self.editing) {
    frame.origin.x += EDITING_INSET;
  }
  return frame;
}

- (CGRect) _textViewFrame {
  CGRect frame = CGRectMake(2 * VIEW_PADDING + IMAGE_SIZE, VIEW_PADDING, self.contentView.bounds.size.width - IMAGE_SIZE - DATE_TIME_WIDTH - 3 * VIEW_PADDING, CONTENT_HEIGHT - 2 * VIEW_PADDING);
  if (self.editing) {
    frame.origin.x += EDITING_INSET;
    frame.size.width += DATE_TIME_WIDTH + VIEW_PADDING - EDITING_INSET;
  }
  return frame;
}

- (CGRect) _timeViewFrame {
  float x = CGRectGetMaxX(self.contentView.bounds) - DATE_TIME_WIDTH - VIEW_PADDING;
  CGRect frame = CGRectMake(x, VIEW_PADDING, DATE_TIME_WIDTH, (CONTENT_HEIGHT - 2 * VIEW_PADDING) / 2);
  return frame;
}

- (CGRect) _dateViewFrame {
  float x = CGRectGetMaxX(self.contentView.bounds) - DATE_TIME_WIDTH - VIEW_PADDING;
  CGRect frame = CGRectMake(x, (CONTENT_HEIGHT - 2 * VIEW_PADDING) / 2, DATE_TIME_WIDTH, (CONTENT_HEIGHT - 2 * VIEW_PADDING) / 2);
  return frame;
}


- (void)layoutSubviews {
  [super layoutSubviews];
  
  [imageView setFrame:[self _imageViewFrame]];
  [textView setFrame:[self _textViewFrame]];
  [dateView setFrame:[self _dateViewFrame]];
  [timeView setFrame:[self _timeViewFrame]];
  if (self.editing) {
    dateView.alpha = 0.0;
    timeView.alpha = 0.0;
  } else {
    dateView.alpha = 1.0;
    timeView.alpha = 1.0;
  }
}



- (void)setScan:(Scan *)newScan {
  if (newScan != scan) {
    [newScan retain];
    [scan release];
    scan = newScan;
    [result release];
    result = [[ResultParser parsedResultForString:[scan text]] retain];

    imageView.image = [result icon];
    textView.text = [result stringForDisplay];
    
    NSDate *date = [scan stamp];
    dateView.text = _dateString(date);
    timeView.text = _timeString(date);
  }
}

- (Scan *)scan {
  return scan;
}

- (void)dealloc {
  [imageView release];
  [textView release];
  [dateView release];
  [timeView release];
  [scan release];
  [result release];
  [super dealloc];
}


@end
