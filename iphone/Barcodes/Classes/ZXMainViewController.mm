//
//  ZXMainViewController.m
//  Barcodes
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "ZXMainViewController.h"
#import <QRCodeReader.h>
#import <UniversalResultParser.h>
#import <ParsedResult.h>
#import <ResultAction.h>
#import "ArchiveController.h"
#import "Database.h"

@implementation ZXMainViewController
@synthesize actions;
@synthesize result;
@synthesize resultView;
@synthesize lastActionButton;

// The designated initializer.  Override if you create the controller programmatically and want to perform customization that is not appropriate for viewDidLoad.
/*
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {

    }
    return self;
}
*/

      
// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad {
  [super viewDidLoad];
  NSString *rawLatestResult = [[NSUserDefaults standardUserDefaults] objectForKey:@"lastScan"];
  if (!rawLatestResult) rawLatestResult = NSLocalizedString(@"Latest result will appear here once you have scanned a barcode at least once",@"Latest result will appear here once you have scanned a barcode at least once");
  [self setResultViewWithText:rawLatestResult];
  
}


/*
// Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations.
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
*/

- (IBAction)scan:(id)sender {
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

- (void) messageReady:(id)sender {
  MessageViewController *messageController = sender;
  [self presentModalViewController:messageController animated:YES];
  [messageController release];
}

- (void) messageFailed:(id)sender {
  MessageViewController *messageController = sender;
  NSLog(@"Failed to load message!");
  [messageController release];
}

- (IBAction)info:(id)sender {
  MessageViewController *aboutController =
  [[MessageViewController alloc] initWithMessageFilename:@"About"];
  aboutController.delegate = self;
  [self presentModalViewController:aboutController animated:YES];
  [aboutController release];
}

- (IBAction)showArchive:(id)sender {
  ArchiveController *archiveController = [[ArchiveController alloc] init];
  archiveController.delegate = self;
  UINavigationController *navController = [[UINavigationController alloc] initWithRootViewController:archiveController];
  [self presentModalViewController:navController animated:YES];
  [navController release];
  [archiveController release];
}

- (IBAction)lastResultAction:(id)sender {
  [self performResultAction];
}


- (void)performAction:(ResultAction *)action {
  [action performActionWithController:self shouldConfirm:NO];
}

- (void)modalViewControllerWantsToBeDismissed:(UIViewController *)controller {
  [self dismissModalViewControllerAnimated:YES];
}

- (void)didReceiveMemoryWarning {
  // Releases the view if it doesn't have a superview.
  [super didReceiveMemoryWarning];
  // Release any cached data, images, etc. that aren't in use.
}

- (void)viewDidUnload {
  [super viewDidUnload];
  // Release any retained subviews of the main view.
  // e.g. self.myOutlet = nil;
}


- (void)dealloc {
  [resultView release];
  [lastActionButton release];
  actions = nil;
  result = nil;
  [super dealloc];
}

- (void)setResultViewWithText:(NSString*)theResult {
  ParsedResult *parsedResult = [[UniversalResultParser parsedResultForString:theResult] retain];
  NSString *displayString = [parsedResult stringForDisplay];
  self.resultView.text = displayString;
  self.result = [parsedResult retain];
  self.actions = [[parsedResult actions] retain];
  NSString *buttonTitle;
  if ([self.actions count] == 1) {
    ResultAction *theAction = [self.actions objectAtIndex:0];
    buttonTitle = [theAction title];
    lastActionButton.userInteractionEnabled = YES;
  } else if ([self.actions count] == 0) {
    lastActionButton.userInteractionEnabled = NO;
    buttonTitle = NSLocalizedString(@"No Actions",@"No Actions");
  } else {
    
    lastActionButton.userInteractionEnabled = YES;
    buttonTitle = NSLocalizedString(@"Actions ...",@"Actions ...");
  }
  [lastActionButton setTitle:buttonTitle forState: UIControlStateNormal];
}

#pragma mark -
#pragma mark ZXingDelegateMethods
- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)resultString {
  [self dismissModalViewControllerAnimated:YES];
  //ParsedResult *theResult = [UniversalResultParser parsedResultForString:resultString];
  //self.result = [theResult retain];
  //self.actions = [self.result.actions retain];
  [self setResultViewWithText:resultString];
#ifdef DEBUG  
  NSLog(@"result has %d actions", actions ? 0 : actions.count);
#endif
  [[Database sharedDatabase] addScanWithText:resultString];
  [[NSUserDefaults standardUserDefaults] setObject:resultString forKey:@"lastScan"];
  [self performResultAction];
}

- (void)confirmAndPerformAction:(ResultAction *)action {
  [action performActionWithController:self shouldConfirm:YES];
}

- (void)performResultAction {
  if (self.result == nil) {
    NSLog(@"no result to perform an action on!");
    return;
  }
  
  if (self.actions == nil || self.actions.count == 0) {
    NSLog(@"result has no actions to perform!");
    return;
  }
  
  if (self.actions.count == 1) {
    ResultAction *action = [self.actions lastObject];
#ifdef DEBUG
    NSLog(@"Result has the single action, (%@)  '%@', performing it",
          NSStringFromClass([action class]), [action title]);
#endif
    [self performSelector:@selector(confirmAndPerformAction:)
               withObject:action
               afterDelay:0.0];
  } else {
#ifdef DEBUG
    NSLog(@"Result has multiple actions, popping up an action sheet");
#endif
    UIActionSheet *actionSheet = [[UIActionSheet alloc] initWithFrame:self.view.bounds];
    
    for (ResultAction *action in self.actions) {
      [actionSheet addButtonWithTitle:[action title]];
    }
    
    int cancelIndex = [actionSheet addButtonWithTitle:NSLocalizedString(@"DecoderViewController cancel button title", @"Cancel")];
    actionSheet.cancelButtonIndex = cancelIndex;
    
    actionSheet.delegate = self;
    
    [actionSheet showInView:self.view];
  }
}

- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller {
  [self dismissModalViewControllerAnimated:YES];
}


@end
