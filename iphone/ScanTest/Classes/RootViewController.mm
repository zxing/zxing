// -*- Mode: ObjC; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
/*
 * Copyright 2010-2012 ZXing authors
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

#import "RootViewController.h"
#import "MultiFormatReader.h"

@implementation RootViewController
@synthesize resultsView;
@synthesize resultsToDisplay;
#pragma mark -
#pragma mark View lifecycle

- (void)viewDidLoad {
  [super viewDidLoad];
  [self setTitle:@"ZXing"];
  [resultsView setText:resultsToDisplay];
}

- (IBAction)scanPressed:(id)sender {
	
  ZXingWidgetController *widController =
    [[ZXingWidgetController alloc] initWithDelegate:self showCancel:YES OneDMode:NO];

  NSMutableSet *readers = [[NSMutableSet alloc ] init];

  MultiFormatReader* reader = [[MultiFormatReader alloc] init];
  [readers addObject:reader];
  [reader release];
    
  widController.readers = readers;
  [readers release];
    
  NSBundle *mainBundle = [NSBundle mainBundle];
  widController.soundToPlay =
    [NSURL fileURLWithPath:[mainBundle pathForResource:@"beep-beep" ofType:@"aiff"] isDirectory:NO];

  [self presentModalViewController:widController animated:YES];
  // [self presentViewController:widController animated:YES completion:nil];

  [widController release];
}

#pragma mark -
#pragma mark ZXingDelegateMethods

- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result {
  self.resultsToDisplay = result;
  if (self.isViewLoaded) {
    [resultsView setText:resultsToDisplay];
    [resultsView setNeedsDisplay];
  }
  [self dismissModalViewControllerAnimated:NO];
  // [self dismissViewControllerAnimated:NO completion:nil];
}

- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller {
  [self dismissModalViewControllerAnimated:NO];
  // [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)viewDidUnload {
  self.resultsView = nil;
}

- (void)dealloc {
  [resultsView release];
  [resultsToDisplay release];
  [super dealloc];
}


@end

