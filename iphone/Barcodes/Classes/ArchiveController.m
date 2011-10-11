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
#import "UniversalResultParser.h"
#import "ParsedResult.h"
#import "ScanViewController.h"
#import "ScanCell.h"

@implementation ArchiveController

@synthesize scans;
@synthesize results;
@synthesize delegate;
@synthesize dateFormatter;


- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
	return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
	return [scans count];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  return 44.0;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
	static NSString *ScanIdentifier = @"ScanIdentifier";
	
	ScanCell *cell = (ScanCell *)[tableView dequeueReusableCellWithIdentifier:ScanIdentifier];
	if (cell == nil) {
		cell = [[[ScanCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:ScanIdentifier] autorelease];
	}
  
	// Configure the cell
  int idx = [self scanIndexForRow:indexPath.row];
  Scan *scan = [scans objectAtIndex:idx];
  [cell setScan:scan];
	return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  //[decoderViewController showScan:[scans objectAtIndex:[self scanIndexForRow:indexPath.row]]];
  int idx = [self scanIndexForRow:indexPath.row];
  Scan *scan = [scans objectAtIndex:idx];
  ParsedResult *result = [results objectAtIndex:idx];
  ScanViewController *scanViewController = [[ScanViewController alloc] initWithResult:result forScan:scan];
  [self.navigationController pushViewController:scanViewController animated:YES];
  [scanViewController release];
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
	if (editingStyle == UITableViewCellEditingStyleDelete) {
    int idx = [self scanIndexForRow:indexPath.row];
    Scan *scan = [self.scans objectAtIndex:idx];
    // delete the scan from the database ...
    [[Database sharedDatabase] deleteScan:scan];
    // ... delete the scan from our in-memory cache of the database ...
    [scans removeObjectAtIndex:idx];
    // ... delete the corresponding result from our in-memory cache  ...
    [results removeObjectAtIndex:idx];
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
  delegate = nil;
  [dateFormatter release];
	[super dealloc];
}


- (void)viewDidLoad {
	[super viewDidLoad];
  NSMutableArray *theScans = [[NSMutableArray alloc] init];
  self.scans = theScans;
  [theScans release];
  
  NSMutableArray *theResults = [[NSMutableArray alloc] init];
  self.results = theResults;
  [theResults release];
  
  NSDateFormatter *theDateFormatter = [[NSDateFormatter alloc] init];
  self.dateFormatter = theDateFormatter;
  [theDateFormatter release];
  
  self.title = NSLocalizedString(@"ScanArchiveTitle", @"Scan Archive");
  self.navigationItem.rightBarButtonItem = [self editButtonItem];
  //self.navigationItem.leftBarButtonItem = [[[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"Back",@"Back") style:UIBarButtonItemStyleDone target:self action:@selector(done:)] autorelease];
  
  self.scans = [NSMutableArray arrayWithArray:[[Database sharedDatabase] scans]];
  self.results = [NSMutableArray arrayWithCapacity:self.scans.count];
  for (Scan *scan in scans) {
    ParsedResult *res = [UniversalResultParser parsedResultForString:scan.text];
    [results addObject:res];
  }
}

-(void)done:(id)sender {
  [delegate modalViewControllerWantsToBeDismissed:self];
}


- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  self.scans = [NSMutableArray arrayWithArray:[[Database sharedDatabase] scans]];
  self.results = [NSMutableArray arrayWithCapacity:self.scans.count];
  for (Scan *scan in scans) {
    [results addObject:[UniversalResultParser parsedResultForString:scan.text]];
  }
  [self.tableView reloadData];
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

