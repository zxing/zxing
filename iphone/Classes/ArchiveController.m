//
//  ArchiveController.m
//  UIShowcase
//
//  Created by Christian Brunschen on 29/05/2008.
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

#import "ArchiveController.h"
#import "Database.h"
#import "Scan.h"
#import "ParsedResult.h"
#import "DecoderViewController.h"
#import "ScanViewController.h"

#define IMAGE_VIEW_TAG 0x17
#define DATE_VIEW_TAG 0x18
#define TEXT_VIEW_TAG 0x19

#define VIEW_PADDING 2
#define IMAGE_VIEW_SIDE 40
#define CONTENT_HEIGHT IMAGE_VIEW_SIDE
#define DATE_VIEW_WIDTH 50

@implementation ArchiveController

@synthesize scans;
@synthesize results;
@synthesize decoderViewController;
@synthesize dateFormatter;

- initWithDecoderViewController:(DecoderViewController *)dc {
	if (self = [super initWithStyle:UITableViewStylePlain]) {
    decoderViewController = [dc retain];
    scans = [[NSMutableArray alloc] init];
    results = [[NSMutableArray alloc] init];
    dateFormatter = [[NSDateFormatter alloc] init];
	}
	return self;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
	return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
	return [scans count];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  return IMAGE_VIEW_SIDE + 2 * VIEW_PADDING;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
	static NSString *ScanIdentifier = @"ScanIdentifier";
	
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:ScanIdentifier];
	if (cell == nil) {
		cell = [[[UITableViewCell alloc] initWithFrame:CGRectZero reuseIdentifier:ScanIdentifier] autorelease];
    
    // clean out all existing subviews
    NSArray *subviews = [[NSArray alloc] initWithArray:cell.contentView.subviews];
    for (UIView *subview in subviews) {
      [subview removeFromSuperview];
    }
    [subviews release];

    float cellWidth = cell.contentView.bounds.size.width;

    // add the views
    UIImageView *imageView = [[UIImageView alloc] initWithFrame:CGRectMake(VIEW_PADDING, VIEW_PADDING, IMAGE_VIEW_SIDE, CONTENT_HEIGHT)];
    [imageView setTag:IMAGE_VIEW_TAG];
    [imageView setAutoresizingMask:UIViewAutoresizingFlexibleRightMargin];
    [cell.contentView addSubview:imageView];
    [imageView release];
    
    UILabel *textView = [[UILabel alloc] initWithFrame:CGRectMake(2*VIEW_PADDING + IMAGE_VIEW_SIDE, VIEW_PADDING, cellWidth - 4*VIEW_PADDING - IMAGE_VIEW_SIDE - DATE_VIEW_WIDTH, CONTENT_HEIGHT)];
    [textView setTag:TEXT_VIEW_TAG];
    [textView setAutoresizingMask:UIViewAutoresizingFlexibleWidth];
    [cell.contentView addSubview:textView];
    [textView release];
    
    UITextView *dateView = [[UITextView alloc] initWithFrame:CGRectMake(cellWidth - VIEW_PADDING - DATE_VIEW_WIDTH, VIEW_PADDING, DATE_VIEW_WIDTH, CONTENT_HEIGHT)];
    [dateView setTag:DATE_VIEW_TAG];
    [dateView setAutoresizingMask:UIViewAutoresizingFlexibleLeftMargin];
    dateView.font = [UIFont systemFontOfSize:9.0];
    dateView.textColor = [UIColor grayColor];
    dateView.textAlignment = UITextAlignmentRight;
    dateView.editable = NO;
    [cell.contentView addSubview:dateView];
    [dateView release];
	}
  
  UIImageView *imageView = (UIImageView *)[cell.contentView viewWithTag:IMAGE_VIEW_TAG];
  UILabel *textView = (UILabel *)[cell.contentView viewWithTag:TEXT_VIEW_TAG];
  UITextView *dateView = (UITextView *)[cell.contentView viewWithTag:DATE_VIEW_TAG];
	// Configure the cell
  int index = [self scanIndexForRow:indexPath.row];
  Scan *scan = [scans objectAtIndex:index];
  ParsedResult *result = [results objectAtIndex:index];
  imageView.image = nil;
  NSDate *stamp = [scan stamp];
  NSTimeInterval interval = -[stamp timeIntervalSinceNow];
  if (interval < 24 * 3600) { // last 24 hours
    [dateFormatter setDateStyle:NSDateFormatterNoStyle];
    [dateFormatter setTimeStyle:NSDateFormatterShortStyle];
  } else if (interval < 30 * 24 * 3600) { // last 30 days
    [dateFormatter setDateStyle:NSDateFormatterShortStyle];
    [dateFormatter setTimeStyle:NSDateFormatterShortStyle];
  } else {
    [dateFormatter setDateStyle:NSDateFormatterShortStyle];
    [dateFormatter setTimeStyle:NSDateFormatterNoStyle];
  }
  dateView.text = [dateFormatter stringFromDate:[scan stamp]];
  [dateView sizeToFit];
  textView.text = [result stringForDisplay];
  imageView.image = [result icon];
	return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  //[decoderViewController showScan:[scans objectAtIndex:[self scanIndexForRow:indexPath.row]]];
  int index = [self scanIndexForRow:indexPath.row];
  Scan *scan = [scans objectAtIndex:index];
  ParsedResult *result = [results objectAtIndex:index];
  ScanViewController *scanViewController = [[ScanViewController alloc] initWithResult:result forScan:scan];
  [self.navigationController pushViewController:scanViewController animated:YES];
  [scanViewController release];
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
	if (editingStyle == UITableViewCellEditingStyleDelete) {
    int index = [self scanIndexForRow:indexPath.row];
    Scan *scan = [self.scans objectAtIndex:index];
    // delete the scan from the database ...
    [[Database sharedDatabase] deleteScan:scan];
    // ... delete the scan from our in-memory cache of the database ...
    [scans removeObjectAtIndex:index];
    // ... delete the corresponding result from our in-memory cache  ...
    [results removeObjectAtIndex:index];
    // ... and remove the row from the table view.
    [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationFade];
    // [tableView reloadData];
	} else if (editingStyle == UITableViewCellEditingStyleInsert) {
    // no insertions!
	}
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
	return YES;
}

- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
	return NO;
}


- (void)dealloc {
  [scans release];
  [results release];
  [decoderViewController release];
  [dateFormatter release];
	[super dealloc];
}


- (void)viewDidLoad {
	[super viewDidLoad];
  self.title = NSLocalizedString(@"Scan Archive", "scan archive title");
  self.navigationItem.rightBarButtonItem = [self editButtonItem];
}


- (void)viewWillAppear:(BOOL)animated {
	[super viewWillAppear:animated];
  self.scans = [NSMutableArray arrayWithArray:[[Database sharedDatabase] scans]];
  self.results = [NSMutableArray arrayWithCapacity:self.scans.count];
  for (Scan *scan in scans) {
    [results addObject:[ParsedResult parsedResultForString:scan.text]];
  }
}

- (void)viewDidAppear:(BOOL)animated {
	[super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
  self.scans = nil;
  self.results = nil;
}

- (void)viewDidDisappear:(BOOL)animated {
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning];
}

- (NSInteger)scanIndexForRow:(NSInteger)row {
  return scans.count - 1 - row;
}

@end

