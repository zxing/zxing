//
//  RootViewController.m
//  ScanTest
//
//  Created by David Kavanagh on 5/10/10.
//  Copyright __MyCompanyName__ 2010. All rights reserved.
//

#import "RootViewController.h"
@interface RootViewController()
@property (nonatomic,retain) ZXingWidgetController *scanController;

@end


@implementation RootViewController
@synthesize resultsView;
@synthesize resultsToDisplay;
@synthesize scanController;
#pragma mark -
#pragma mark View lifecycle

- (void)viewDidLoad {
    [super viewDidLoad];
	[self setTitle:@"ZXing"];
  
  ZXingWidgetController *widController = [[ZXingWidgetController alloc] initWithDelegate:self showCancel:YES OneDMode:NO];
	self.scanController = widController;
  [widController release];
	NSBundle *mainBundle = [NSBundle mainBundle];
	[scanController setSoundToPlay:[[NSURL fileURLWithPath:[mainBundle pathForResource:@"beep-beep" ofType:@"aiff"] isDirectory:NO] retain]];
   
}

- (IBAction)scanPressed:(id)sender {
  //UIImagePickerController *picker = [[UIImagePickerController alloc] init];
	[self presentModalViewController:scanController
                          animated:NO];
//	[self.navigationController pushViewController:scanController animated:true];
}

/*
- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
}
*/
/*
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
}
*/
/*
- (void)viewWillDisappear:(BOOL)animated {
	[super viewWillDisappear:animated];
}
*/
/*
- (void)viewDidDisappear:(BOOL)animated {
	[super viewDidDisappear:animated];
}
*/

/*
 // Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
	// Return YES for supported orientations.
	return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
 */

#pragma mark Memory management

- (void)scanResult:(NSString *)result {
	//[self.resultsView setText:result];
	[self dismissModalViewControllerAnimated:YES];
  self.resultsToDisplay = result;
}

- (void)viewWillAppear:(BOOL)animated {
  if (resultsToDisplay)
  {
    [resultsView setText:resultsToDisplay];
    [resultsView setNeedsDisplay];
  }
}

- (void)cancelled {
	[self dismissModalViewControllerAnimated:YES];
}

#pragma mark -
#pragma mark Memory management

- (void)didReceiveMemoryWarning {
    // Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
    
    // Relinquish ownership any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
    // Relinquish ownership of anything that can be recreated in viewDidLoad or on demand.
    // For example: self.myOutlet = nil;
}


- (void)dealloc {
  [resultsView release];
	[scanController release];
  [resultsToDisplay release];
    [super dealloc];
}


@end

