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

@implementation ArchiveController

@synthesize scans;
@synthesize decoderViewController;

- initWithDecoderViewController:(DecoderViewController *)dc {
	if (self = [super initWithStyle:UITableViewStylePlain]) {
    self.decoderViewController = dc;
    self.scans = [NSMutableArray array];
	}
	return self;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
	return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
	return [scans count];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
	static NSString *ScanIdentifier = @"ScanIdentifier";
	
	UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:ScanIdentifier];
	if (cell == nil) {
		cell = [[[UITableViewCell alloc] initWithFrame:CGRectZero reuseIdentifier:ScanIdentifier] autorelease];
    cell.font = [cell.font fontWithSize:10.0];
    cell.lineBreakMode = UILineBreakModeCharacterWrap;
	}
	// Configure the cell
  Scan *scan = [scans objectAtIndex:[self scanIndexForRow:indexPath.row]];
  ParsedResult *result = [ParsedResult parsedResultForString:scan.text];
  cell.text = [result stringForDisplay];
	return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  [decoderViewController showScan:[scans objectAtIndex:[self scanIndexForRow:indexPath.row]]];
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
	if (editingStyle == UITableViewCellEditingStyleDelete) {
    int index = [self scanIndexForRow:indexPath.row];
    Scan *scan = [self.scans objectAtIndex:index];
    [[Database sharedDatabase] deleteScan:scan];
    [self.scans removeObjectAtIndex:index];
    [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationFade];
    [tableView reloadData];
	} else if (editingStyle == UITableViewCellEditingStyleInsert) {
    // no insertions!
	}
}

/*
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
	return YES;
}
*/
/*
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
}
*/
/*
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
	return YES;
}
*/


- (void)dealloc {
  [scans release];
  [decoderViewController release];
	[super dealloc];
}


- (void)viewDidLoad {
	[super viewDidLoad];
  self.title = @"Scan Archive";
  self.navigationItem.rightBarButtonItem = [self editButtonItem];
}


- (void)viewWillAppear:(BOOL)animated {
	[super viewWillAppear:animated];
  self.scans = [NSMutableArray arrayWithArray:[[Database sharedDatabase] scans]];
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

- (NSInteger)scanIndexForRow:(NSInteger)row {
  return scans.count - 1 - row;
}

@end

