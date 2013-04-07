// -*- Mode: ObjC; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
//
//  ZXMainViewController.m
//  Barcodes
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "ZXMainViewController.h"
#import <MultiFormatReader.h>
#import <UniversalResultParser.h>
#import <ParsedResult.h>
#import <ResultAction.h>
#import "ArchiveController.h"
#import "Database.h"
#import "ScanViewController.h"
#import "BarcodesAppDelegate.h"

@implementation ZXMainViewController
@synthesize actions;
@synthesize result;

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
  self.navigationItem.title = @"Barcodes";

  [((BarcodesAppDelegate*)[[UIApplication sharedApplication] delegate]) registerView:self];
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
  MultiFormatReader* qrcodeReader = [[MultiFormatReader alloc] init];
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

- (void)messageReady:(id)sender {
  MessageViewController *messageController = sender;
  //[self presentModalViewController:messageController animated:YES];
  //TODO: change this
  [self.navigationController pushViewController:messageController animated:YES];
  [messageController release];
}

- (void)messageFailed:(id)sender {
  MessageViewController *messageController = sender;
  NSLog(@"Failed to load message!");
  [messageController release];
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
  [actions release];
  [result release];
  [super dealloc];
}

#pragma mark -
#pragma mark ZXingDelegateMethods
- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)resultString {
  [self dismissModalViewControllerAnimated:YES];
#if ZXING_DEBUG
  NSLog(@"result has %d actions", actions ? 0 : actions.count);
#endif
  Scan * scan = [[Database sharedDatabase] addScanWithText:resultString];
  [[NSUserDefaults standardUserDefaults] setObject:resultString forKey:@"lastScan"];
  NSString *returnUrl = [[NSUserDefaults standardUserDefaults] stringForKey:@"returnURL"];

  [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"returnURL"];
  [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"scanFormats"];
  [[NSUserDefaults standardUserDefaults] synchronize];

  if (returnUrl != nil) {
    resultString = (NSString*)
        CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault,
                                                (CFStringRef)resultString,
                                                NULL,
                                                (CFStringRef)@"!*'();:@&=+$,/?%#[]",
                                                kCFStringEncodingUTF8);

    NSURL *ourURL =
        [NSURL URLWithString:[returnUrl stringByReplacingOccurrencesOfString:@"{CODE}" withString:resultString]];

    CFRelease(resultString);

    // NSLog(@"%@", ourURL);

    [[UIApplication sharedApplication] openURL:ourURL];
    return;
  }
  
  ParsedResult *parsedResult = [UniversalResultParser parsedResultForString:resultString];
  self.result = [parsedResult retain];
  self.actions = [self.result.actions retain];
  ScanViewController *scanViewController = [[ScanViewController alloc] initWithResult:parsedResult forScan:scan];
  [self.navigationController pushViewController:scanViewController animated:NO];
  [scanViewController release];
  [self performResultAction];
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
  NSString* returnUrl = [[NSUserDefaults standardUserDefaults] stringForKey:@"returnURL"];

  [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"returnURL"];
  [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"scanFormats"];
  [[NSUserDefaults standardUserDefaults] synchronize];

  // NSLog(@"%@ %d", returnUrl, buttonIndex);
  if (returnUrl != nil && buttonIndex != 0) {
    NSURL *ourURL =
        [NSURL URLWithString:[returnUrl stringByReplacingOccurrencesOfString:@"{CODE}" withString:@""]];
    // NSLog(@"%@ %@", ourURL, returnUrl);
    [[UIApplication sharedApplication] openURL:ourURL];
  }
}

- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller {
  [self dismissModalViewControllerAnimated:YES];
  NSString *returnUrl = [[NSUserDefaults standardUserDefaults] stringForKey:@"returnURL"];
  if (returnUrl != nil) {
    UIAlertView* alert = [[UIAlertView alloc]
                           initWithTitle:@"Return to website?"
                                 message:nil
                                delegate:self
                           cancelButtonTitle:@"Cancel"
                           otherButtonTitles:@"Return", nil];
    [alert show];
    [alert release];
  }
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
#if ZXING_DEBUG
    NSLog(@"result has no actions to perform!");
#endif
    return;
  }
  
  if (self.actions.count == 1) {
    ResultAction *action = [self.actions lastObject];
#if ZXING_DEBUG
    NSLog(@"Result has the single action, (%@)  '%@', performing it",
          NSStringFromClass([action class]), [action title]);
#endif
    [self performSelector:@selector(confirmAndPerformAction:)
               withObject:action
               afterDelay:0.0];
  } else {
#if ZXING_DEBUG
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




@end
