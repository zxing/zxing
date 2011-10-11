// -*- Mode: ObjC; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
//
//  ScanViewController.m
//  ZXing
//
//  Created by Christian Brunschen on 24/06/2008.
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

#import "ScanViewController.h"
#import "ResultAction.h"


#define TEXT_VIEW_TAG 0x17
#define DATETIME_VIEW_TAG 0x18
#define BUTTON_LABEL_TAG 0x19
#define TITLE_HEIGHT 44
#define BODY_HEIGHT 88

@implementation ScanViewController

@synthesize result;
@synthesize scan;
@synthesize dateFormatter;

#define FONT_NAME @"TimesNewRomanPSMT"
#define FONT_SIZE 16

- (id)initWithResult:(ParsedResult *)r forScan:(Scan *)s {
	if ((self = [super initWithStyle:UITableViewStyleGrouped])) {
    self.result = r;
    self.scan = s;
    self.title = NSLocalizedString(@"ScanViewController title", @"Scan");
    dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateStyle:NSDateFormatterLongStyle];
    [dateFormatter setTimeStyle:NSDateFormatterLongStyle];
    bodyFont = [[UIFont fontWithName:FONT_NAME size:FONT_SIZE] retain];
	}
	return self;
}


- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
	return [[result actions] count] ? 2 : 1;
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
  switch (section) {
    case 0:
      return 3;
    case 1:
      return [[result actions] count];
    default:
      return 0;
  }
}

- (UITableViewCell *)cellWithIdentifier:(NSString *)identifier inTableView:(UITableView *)tableView {
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:identifier];
	if (cell == nil) {
                cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault  reuseIdentifier:identifier] autorelease];
	}
  return cell;
}

- (UITableViewCell *)titleCellInTableView:(UITableView *)tableView {
	static NSString *TitleIdentifier = @"ScanViewTitleIdentifier";
  return [self cellWithIdentifier:TitleIdentifier inTableView:tableView];
}

- (UITableViewCell *)datetimeCellInTableView:(UITableView *)tableView {
	static NSString *DatetimeIdentifier = @"ScanViewDatetimeIdentifier";
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:DatetimeIdentifier];
	if (cell == nil) {
		cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:DatetimeIdentifier] autorelease];
    cell.frame = CGRectMake(0, 0, 320, 34);
    UILabel *label = [cell textLabel];
    label.font = [UIFont systemFontOfSize:[UIFont systemFontSize] * 2.0 / 3.0];
    label.textColor = [UIColor grayColor];
    label.textAlignment = UITextAlignmentCenter;
	}
  return cell;
}

- (UITableViewCell *)bodyCellInTableView:(UITableView *)tableView {
	static NSString *BodyIdentifier = @"ScanViewBodyIdentifier";
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:BodyIdentifier];
	if (cell == nil) {
		cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:BodyIdentifier] autorelease];
    cell.frame = CGRectMake(0, 0, 320, BODY_HEIGHT);
    UITextView *textView = [[UITextView alloc] initWithFrame:CGRectInset(cell.contentView.bounds, 6, 6)];
    textView.font = bodyFont;
    [textView setTag:TEXT_VIEW_TAG];
    textView.editable = NO;
    textView.dataDetectorTypes = UIDataDetectorTypeAll;
    [textView setAutoresizingMask:(UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight)];
    [cell.contentView addSubview:textView];
    [textView release];
	}
  return cell;
}

- (UITableViewCell *)buttonCellInTableView:(UITableView *)tableView {
	static NSString *ButtonIdentifier = @"ScanViewButtonIdentifier";
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:ButtonIdentifier];
	if (cell == nil) {
		cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:ButtonIdentifier] autorelease];
    cell.frame = CGRectMake(0, 0, 320, 44);
    UILabel *label = [[UILabel alloc] initWithFrame:CGRectInset(cell.contentView.bounds, 6, 6)];
    label.font = [UIFont boldSystemFontOfSize:[UIFont systemFontSize]];
    [label setTag:BUTTON_LABEL_TAG];
    label.lineBreakMode = UILineBreakModeMiddleTruncation;
    label.textColor = [UIColor grayColor];
    label.textAlignment = UITextAlignmentCenter;
    [label setAutoresizingMask:(UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight)];
    [cell.contentView addSubview:label];
    [label release];
	}
  return cell;
}

#define TEXT_VIEW_HEIGHT 330.0

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  if (indexPath.section == 0) {
    if (indexPath.row == 0) {
      return TITLE_HEIGHT;
    } else if (indexPath.row == 1) {
      CGSize size = [[result stringForDisplay] sizeWithFont:bodyFont constrainedToSize:CGSizeMake(280.0, TEXT_VIEW_HEIGHT) lineBreakMode:UILineBreakModeWordWrap];
#ifdef DEBUG
      NSLog(@"text size = %f", size.height);
#endif
      return fminf(TEXT_VIEW_HEIGHT, fmaxf(44, size.height + 24));
    } else if (indexPath.row == 2) {
      return 24.0;
    }
  }
  return tableView.rowHeight;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  UITableViewCell *cell = nil;
  
  if (indexPath.section == 0) {
    if (indexPath.row == 0) {
      cell = [self titleCellInTableView:tableView];
      UIImageView *imageView = cell.imageView;
      imageView.image = [result icon];
      UILabel *textLabel = cell.textLabel;
      textLabel.text = [[result class] typeName];
    } else if (indexPath.row == 1) {
      cell = [self bodyCellInTableView:tableView];
      UITextView *textView = (UITextView *)[cell viewWithTag:TEXT_VIEW_TAG];
      textView.text = [result stringForDisplay];
    } else if (indexPath.row == 2) {
      cell = [self datetimeCellInTableView:tableView];
      UILabel *textLabel = cell.textLabel;
      textLabel.text = [dateFormatter stringFromDate:[scan stamp]];
    }
  } else if (indexPath.section == 1) {
    cell = [self buttonCellInTableView:tableView];
    ResultAction *action = [[result actions] objectAtIndex:indexPath.row];
    UILabel *label = (UILabel *)[cell viewWithTag:BUTTON_LABEL_TAG];
    label.text = [action title];
  }
	
	return cell;
}

- (void)performAction:(ResultAction *)action {
  [action performActionWithController:self shouldConfirm:NO];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  if (indexPath.section == 1) {
    ResultAction *action = [[result actions] objectAtIndex:indexPath.row];
    [self performSelector:@selector(performAction:) withObject:action afterDelay:0.0];
  }
}

/*
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
	
	if (editingStyle == UITableViewCellEditingStyleDelete) {
	}
	if (editingStyle == UITableViewCellEditingStyleInsert) {
	}
}
*/

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
	return NO;
}

/*
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}
*/

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
	return NO;
}

- (NSIndexPath *)tableView:(UITableView *)tableView willSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  if (indexPath.section != 1) {
    return nil;
  }
  return indexPath;
}


- (void)dealloc {
  [result release];
  [scan release];
  [bodyFont release];
  [dateFormatter release];
	[super dealloc];
}


- (void)viewDidLoad {
	[super viewDidLoad];
}


- (void)viewWillAppear:(BOOL)animated {
	[super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated {
	[super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
}

- (void)viewDidDisappear:(BOOL)animated {
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning];
}


@end

