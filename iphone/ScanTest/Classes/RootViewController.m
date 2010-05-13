//
//  RootViewController.m
//  ScanTest
//
//  Created by David Kavanagh on 5/10/10.
//  Copyright __MyCompanyName__ 2010. All rights reserved.
//

#import "RootViewController.h"


@implementation RootViewController
@synthesize resultsView;

#pragma mark -
#pragma mark View lifecycle

- (void)viewDidLoad {
    [super viewDidLoad];
	[self setTitle:@"ZXing"];
	scanController = [[ZXingWidgetController alloc] initWithDelegate:self];
	NSBundle *mainBundle = [NSBundle mainBundle];
	[scanController setSoundToPlay:[[NSURL fileURLWithPath:[mainBundle pathForResource:@"beep-beep" ofType:@"aiff"] isDirectory:NO] retain]];
}

- (IBAction)scanPressed:(id)sender {
	[self presentModalViewController:scanController animated:YES];
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
	[resultsView setText:result];
	[self dismissModalViewControllerAnimated:true];
}

- (void)cancelled {
	[self dismissModalViewControllerAnimated:true];
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
	[scanController dealloc];
    [super dealloc];
}


@end

