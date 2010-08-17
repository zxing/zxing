//
//  RootViewController.m
//  ScanTest
//
//  Created by David Kavanagh on 5/10/10.
//  Copyright __MyCompanyName__ 2010. All rights reserved.
//

#import "RootViewController.h"
#import "QRCodeReader.h"


@interface RootViewController()

@end


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
	
  ZXingWidgetController *widController = [[ZXingWidgetController alloc] initWithDelegate:self showCancel:YES OneDMode:NO];
  QRCodeReader* qrcodeReader = [[QRCodeReader alloc] init];
  NSSet *readers = [[NSSet alloc ] initWithObjects:qrcodeReader,nil];
  [qrcodeReader release];
  widController.readers = readers;
  [readers release];
  NSBundle *mainBundle = [NSBundle mainBundle];
  widController.soundToPlay =
      [NSURL fileURLWithPath:[mainBundle pathForResource:@"beep-beep" ofType:@"aiff"] isDirectory:NO];
  [self presentModalViewController:widController animated:YES];
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
}

- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller {
  [self dismissModalViewControllerAnimated:YES];
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

