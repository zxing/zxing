//
//  DecoderViewController.m
//  ZXing
//
//  Created by Christian Brunschen on 22/05/2008.
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

#import "DecoderViewController.h"
#import "Decoder.h"
#import "NSString+HTML.h"
#import "ResultParser.h"
#import "ParsedResult.h"
#import "ResultAction.h"

#import "Database.h"
#import "ArchiveController.h"
#import "MessageViewController.h"
#import "Scan.h"
#import "TwoDDecoderResult.h"

// Michael Jurewitz, Dec 16, 2009 6:32 PM writes:
// https://devforums.apple.com/message/149553
// Notice Regarding UIGetScreenImage()
// After carefully considering the issue, Apple is now allowing applications to
// use the function UIGetScreenImage() to programmatically capture the current
// screen contents.
// Note that a future release of iPhone OS may provide a public API equivalent
// of this functionality.  At such time, all applications using
// UIGetScreenImage() will be required to adopt the public API.
CGImageRef MyCGImageCopyScreenContents(void) {
   extern CGImageRef UIGetScreenImage(void);
   return UIGetScreenImage(); /* already retained */
}

@interface DecoderViewController()
- (void)takeScreenshot;
@end

@implementation DecoderViewController

@synthesize cameraBarItem;
@synthesize libraryBarItem;
@synthesize savedPhotosBarItem;
@synthesize archiveBarItem;
@synthesize actionBarItem;

@synthesize messageView;
@synthesize messageTextView;
@synthesize messageHelpButton;
@synthesize imageView;
@synthesize picker;
@synthesize toolbar;

@synthesize decoder;
@synthesize result;
@synthesize actions;

@synthesize resultPointViews;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
  if ((self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil])) {
    // Initialization code
    self.title =
        NSLocalizedString(@"DecoderViewController AppTitle", @"Barcode Scanner");

    Decoder *d = [[Decoder alloc] init];
    self.decoder = d;
    d.delegate = self;
    [d release];
    resultPointViews = [[NSMutableArray alloc] init];
  }
  return self;
}

- (void) messageReady:(id)sender {
  MessageViewController *messageController = sender;
  [[self navigationController] pushViewController:messageController animated:true];
  [messageController release];
}

- (void) messageFailed:(id)sender {
  MessageViewController *messageController = sender;
  NSLog(@"Failed to load message!");
  [messageController release];
}

- (void) showHints:(id)sender {
  NSLog(@"Showing Hints!");

  MessageViewController *hintsController =
      [[MessageViewController alloc] initWithMessageFilename:@"Hints"
                                                      target:self
                                                   onSuccess:@selector(messageReady:)
                                                   onFailure:@selector(messageFailed:)];
  hintsController.title =
      NSLocalizedString(@"DecoderViewController Hints MessageViewController title", @"Hints");
  [hintsController view];
}

- (void) showAbout:(id)sender {
  NSLog(@"Showing About!");

  MessageViewController *aboutController =
      [[MessageViewController alloc] initWithMessageFilename:@"About"
                                                      target:self
                                                   onSuccess:@selector(messageReady:)
                                                   onFailure:@selector(messageFailed:)];
  aboutController.title =
      NSLocalizedString(@"DecoderViewController About MessageViewController title", @"About");
  [aboutController view];
}


#define HELP_BUTTON_WIDTH (44.0)
#define HELP_BUTTON_HEIGHT (55.0)


#define FONT_NAME @"TimesNewRomanPSMT"
#define FONT_SIZE 16.0

- (void) reset {
  self.result = nil;
  [self clearImageView];
  [self updateToolbar];
  [self showMessage:NSLocalizedString(@"DecoderViewController take or choose picture",
      @"Please take or choose a picture containing a barcode") helpButton:YES];
}

// Implement loadView if you want to create a view hierarchy programmatically
- (void)loadView {
  [super loadView];

  CGRect messageViewFrame = imageView.frame;
  UIView *mView = [[UIView alloc] initWithFrame:messageViewFrame];
  mView.backgroundColor = [UIColor darkGrayColor];
  mView.alpha = 0.9;
  mView.autoresizingMask = UIViewAutoresizingFlexibleHeight |
  UIViewAutoresizingFlexibleWidth |
  UIViewAutoresizingFlexibleTopMargin;

  UITextView *mTextView = [[UITextView alloc] initWithFrame:messageViewFrame];
  mTextView.autoresizingMask = UIViewAutoresizingFlexibleHeight |
  UIViewAutoresizingFlexibleWidth;
  mTextView.editable = false;
  mTextView.scrollEnabled = true;
  mTextView.font = [UIFont fontWithName:FONT_NAME size:FONT_SIZE];
  mTextView.textColor = [UIColor whiteColor];
  mTextView.backgroundColor = [[UIColor lightGrayColor] colorWithAlphaComponent:0.0];
  mTextView.textAlignment = UITextAlignmentLeft;
  mTextView.alpha = 1.0;
  [mView addSubview:mTextView];

  UIButton *mHelpButton = [[UIButton buttonWithType:UIButtonTypeInfoLight] retain];
  mHelpButton.frame = CGRectMake(messageViewFrame.size.width - HELP_BUTTON_WIDTH, 0.0, HELP_BUTTON_WIDTH, HELP_BUTTON_HEIGHT);

  mHelpButton.backgroundColor = [UIColor clearColor];
  [mHelpButton setUserInteractionEnabled:YES];
  [mHelpButton addTarget:self action:@selector(showHints:) forControlEvents:UIControlEventTouchUpInside];

  self.messageHelpButton = mHelpButton;
  [mHelpButton release];

  self.messageTextView = mTextView;
  [mTextView release];

  self.messageView = mView;
  [mView release];

  [self.view addSubview:self.messageView];

  // add the 'About' button at the top-right of the navigation bar
  UIBarButtonItem *aboutButton =
  [[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"DecoderViewController about button title", @"About")
                                   style:UIBarButtonItemStyleBordered
                                  target:self
                                  action:@selector(showAbout:)];
  self.navigationItem.rightBarButtonItem = aboutButton;
  [aboutButton release];

  [self reset];
}

- (void) updateToolbar {
  self.cameraBarItem.enabled = [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera];
  self.savedPhotosBarItem.enabled = [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeSavedPhotosAlbum];
  self.libraryBarItem.enabled = [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypePhotoLibrary];
  self.archiveBarItem.enabled = true;
  self.actionBarItem.enabled = (self.result != nil) && ([self.result actions] != nil) && ([self.result actions].count > 0);
}


// If you need to do additional setup after loading the view, override viewDidLoad.
- (void)viewDidLoad {
  [super viewDidLoad];
}


- (void)clearImageView {
  imageView.image = nil;
  for (UIView *view in resultPointViews) {
    [view removeFromSuperview];
  }
  [resultPointViews removeAllObjects];
}

- (void)pickAndDecodeFromSource:(UIImagePickerControllerSourceType) sourceType {
  [self reset];

  // Create the Image Picker
  if ([UIImagePickerController isSourceTypeAvailable:sourceType]) {
    UIImagePickerController* aPicker =
        [[[UIImagePickerController alloc] init] autorelease];
    aPicker.sourceType = sourceType;
    aPicker.delegate = self;
    self.picker = aPicker;

    // [[NSUserDefaults standardUserDefaults] boolForKey:@"allowEditing"];
    BOOL isCamera = (sourceType == UIImagePickerControllerSourceTypeCamera);
    if ([picker respondsToSelector:@selector(setAllowsEditing:)]) {
      // not in 3.0
      [picker setAllowsEditing:!isCamera];
    }
    if (isCamera) {
      if ([picker respondsToSelector:@selector(setShowsCameraControls:)]) {
        [picker setShowsCameraControls:NO];
        UIButton *cancelButton =
          [UIButton buttonWithType:UIButtonTypeRoundedRect];
        NSString *cancelString =
          NSLocalizedString(@"DecoderViewController cancel button title", @"");
        CGFloat height = [UIFont systemFontSize];
        CGSize size =
          [cancelString sizeWithFont:[UIFont systemFontOfSize:height]];
        [cancelButton setTitle:cancelString forState:UIControlStateNormal];
        CGRect appFrame = [[UIScreen mainScreen] bounds];
        static const int kMargin = 10;
        static const int kInternalXMargin = 10;
        static const int kInternalYMargin = 10;
        CGRect frame = CGRectMake(kMargin,
          appFrame.size.height - (height + 2*kInternalYMargin + kMargin),
          2*kInternalXMargin + size.width,
          height + 2*kInternalYMargin);
        [cancelButton setFrame:frame];
        [cancelButton addTarget:self
                         action:@selector(cancel:)
               forControlEvents:UIControlEventTouchUpInside];
        picker.cameraOverlayView = cancelButton;
        // The camera takes quite a while to start up. Hence the 2 second delay.
        [self performSelector:@selector(takeScreenshot)
                   withObject:nil
                   afterDelay:2.0];
      }
    }

    // Picker is displayed asynchronously.
    [self presentModalViewController:picker animated:YES];
  } else {
    NSLog(@"Attempted to pick an image with illegal source type '%d'", sourceType);
  }
}

- (IBAction)pickAndDecode:(id) sender {
  UIImagePickerControllerSourceType sourceType;
  int i = [sender tag];

  switch (i) {
    case 0: sourceType = UIImagePickerControllerSourceTypeCamera; break;
    case 1: sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum; break;
    case 2: sourceType = UIImagePickerControllerSourceTypePhotoLibrary; break;
    default: sourceType = UIImagePickerControllerSourceTypeSavedPhotosAlbum;
  }
  [self pickAndDecodeFromSource:sourceType];
}


- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  // Return YES for supported orientations
  return (interfaceOrientation == UIInterfaceOrientationPortrait);
}


- (void)didReceiveMemoryWarning {
  [super didReceiveMemoryWarning]; // Releases the view if it doesn't have a superview
  // Release anything that's not essential, such as cached data
}

- (void)dealloc {
  [decoder release];
  [self clearImageView];
  [imageView release];
  [actionBarItem release];
  [cameraBarItem release];
  [libraryBarItem release];
  [savedPhotosBarItem release];
  [archiveBarItem release];
  [toolbar release];
  [picker release];
  [actions release];
  [resultPointViews release];

  [super dealloc];
}

- (void)showMessage:(NSString *)message helpButton:(BOOL)showHelpButton {
#ifdef DEBUG
  NSLog(@"Showing message '%@' %@ help Button", message, showHelpButton ? @"with" : @"without");
#endif

  CGSize imageMaxSize = imageView.bounds.size;
  if (showHelpButton) {
    imageMaxSize.width -= messageHelpButton.frame.size.width;
  }
  CGSize size = [message sizeWithFont:messageTextView.font constrainedToSize:imageMaxSize lineBreakMode:UILineBreakModeWordWrap];
  float height = 20.0 + fmin(100.0, size.height);
  if (showHelpButton) {
    height = fmax(HELP_BUTTON_HEIGHT, height);
  }

  CGRect messageFrame = imageView.bounds;
  messageFrame.origin.y = CGRectGetMaxY(messageFrame) - height;
  messageFrame.size.height = height;
  [self.messageView setFrame:messageFrame];
  messageView.autoresizingMask = UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
  CGRect messageViewBounds = [messageView bounds];

  self.messageTextView.text = message;
  messageTextView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  if (showHelpButton) {
    CGRect textViewFrame;
    CGRect helpButtonFrame;

    CGRectDivide(messageViewBounds, &helpButtonFrame, &textViewFrame, HELP_BUTTON_WIDTH, CGRectMaxXEdge);
    [self.messageTextView setFrame:textViewFrame];

    [messageHelpButton setFrame:helpButtonFrame];
    messageHelpButton.alpha = 1.0;
    messageHelpButton.enabled = YES;
    messageHelpButton.autoresizingMask =
      UIViewAutoresizingFlexibleLeftMargin |
      UIViewAutoresizingFlexibleTopMargin;
    [messageView addSubview:messageHelpButton];
  } else {
    [messageHelpButton removeFromSuperview];
    messageHelpButton.alpha = 0.0;
    messageHelpButton.enabled = NO;

    [self.messageTextView setFrame:messageViewBounds];
  }
}

// DecoderDelegate methods

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset{
  [self clearImageView];
  [self.imageView setImage:subset];
  [self showMessage:[NSString stringWithFormat:NSLocalizedString(@"DecoderViewController MessageWhileDecodingWithDimensions", @"Decoding image (%.0fx%.0f) ..."), image.size.width, image.size.height]
     helpButton:NO];
}

- (void)decoder:(Decoder *)decoder
  decodingImage:(UIImage *)image
    usingSubset:(UIImage *)subset
       progress:(NSString *)message {
  [self clearImageView];
  [self.imageView setImage:subset];
  [self showMessage:message helpButton:NO];
}

- (void)presentResultForString:(NSString *)resultString {
  self.result = [ResultParser parsedResultForString:resultString];
  [self showMessage:[self.result stringForDisplay] helpButton:NO];
  self.actions = self.result.actions;
#ifdef DEBUG
  NSLog(@"result has %d actions", actions ? 0 : actions.count);
#endif
  [self updateToolbar];
}

- (void)presentResultPoints:(NSArray *)resultPoints
                   forImage:(UIImage *)image
                usingSubset:(UIImage *)subset {
  // simply add the points to the image view
  imageView.image = subset;
  for (NSValue *pointValue in resultPoints) {
    [imageView addResultPoint:[pointValue CGPointValue]];
  }
}

- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset withResult:(TwoDDecoderResult *)twoDResult {
  self.picker = nil;
  [self presentResultForString:twoDResult.text];

  [self presentResultPoints:twoDResult.points forImage:image usingSubset:subset];

  // save the scan to the shared database
  [[Database sharedDatabase] addScanWithText:twoDResult.text];

  [self performResultAction:self];
}

- (void)decoder:(Decoder *)decoder failedToDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset reason:(NSString *)reason {
  if (self.picker && UIImagePickerControllerSourceTypeCamera == self.picker.sourceType) {
    // If we are using the camera, and the user hasn't manually cancelled,
    // take another snapshot and try to decode it.
    [self takeScreenshot];
  } else {
    [self showMessage:reason helpButton:YES];
    [self updateToolbar];
  }
}


- (void)willAnimateFirstHalfOfRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
  [super willAnimateFirstHalfOfRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];

  if (imageView.image) {
    /*
    CGRect viewBounds = imageView.bounds;
    CGSize imageSize = imageView.image.size;
    float scale = fmin(viewBounds.size.width / imageSize.width,
                       viewBounds.size.height / imageSize.height);
    float xOffset = (viewBounds.size.width - scale * imageSize.width) / 2.0;
    float yOffset = (viewBounds.size.height - scale * imageSize.height) / 2.0;
     */

    for (UIView *view in resultPointViews) {
      view.alpha = 0.0;
    }
  }
}

- (void)willAnimateSecondHalfOfRotationFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation duration:(NSTimeInterval)duration {
  [super willAnimateSecondHalfOfRotationFromInterfaceOrientation:fromInterfaceOrientation duration:duration];

  if (imageView.image) {
    /*
    CGRect viewBounds = imageView.bounds;
    CGSize imageSize = imageView.image.size;
    float scale = fmin(viewBounds.size.width / imageSize.width,
                       viewBounds.size.height / imageSize.height);
    float xOffset = (viewBounds.size.width - scale * imageSize.width) / 2.0;
    float yOffset = (viewBounds.size.height - scale * imageSize.height) / 2.0;
     */

    for (UIView *view in resultPointViews) {
      view.alpha = 1.0;
    }
  }
}

- (void)cancel:(id)sender {
  self.picker = nil;
}

- (void)takeScreenshot {
  if (picker) {
    CGImageRef cgScreen = MyCGImageCopyScreenContents();
    if (cgScreen) {
      CGRect croppedFrame = CGRectMake(0, 0, CGImageGetWidth(cgScreen),
          CGImageGetHeight(cgScreen) - (10+toolbar.bounds.size.height));
      CGImageRef cgCropped = CGImageCreateWithImageInRect(cgScreen, croppedFrame);
      if (cgCropped) {
        UIImage *screenshot = [UIImage imageWithCGImage:cgCropped];
        CGImageRelease(cgCropped);
        [self.decoder decodeImage:screenshot];
      }
      CGImageRelease(cgScreen);
    }
  }
}

// UIImagePickerControllerDelegate methods

- (void)imagePickerController:(UIImagePickerController *)aPicker
didFinishPickingMediaWithInfo:(NSDictionary *)info {
  UIImage *imageToDecode =
    [info objectForKey:UIImagePickerControllerEditedImage];
  if (!imageToDecode) {
    imageToDecode = [info objectForKey:UIImagePickerControllerOriginalImage];
  }
  CGSize size = [imageToDecode size];
  CGRect cropRect = CGRectMake(0.0, 0.0, size.width, size.height);
  
#ifdef DEBUG
  NSLog(@"picked image size = (%f, %f)", size.width, size.height);
#endif
  NSString *systemVersion = [[UIDevice currentDevice] systemVersion];

  NSValue *cropRectValue = [info objectForKey:UIImagePickerControllerCropRect];
  if (cropRectValue) {
    UIImage *originalImage = [info objectForKey:UIImagePickerControllerOriginalImage];
    if (originalImage) {
#ifdef DEBUG
      NSLog(@"original image size = (%f, %f)", originalImage.size.width, originalImage.size.height);
#endif
       cropRect = [cropRectValue CGRectValue];
#ifdef DEBUG
      NSLog(@"crop rect = (%f, %f) x (%f, %f)", CGRectGetMinX(cropRect), CGRectGetMinY(cropRect), CGRectGetWidth(cropRect), CGRectGetHeight(cropRect));
#endif
      if (([picker sourceType] == UIImagePickerControllerSourceTypeSavedPhotosAlbum) &&
          [@"2.1" isEqualToString:systemVersion]) {
        // adjust crop rect to work around bug in iPhone OS 2.1 when selecting from the photo roll
        cropRect.origin.x *= 2.5;
        cropRect.origin.y *= 2.5;
        cropRect.size.width *= 2.5;
        cropRect.size.height *= 2.5;
#ifdef DEBUG
        NSLog(@"2.1-adjusted crop rect = (%f, %f) x (%f, %f)", CGRectGetMinX(cropRect), CGRectGetMinY(cropRect), CGRectGetWidth(cropRect), CGRectGetHeight(cropRect));
#endif
      }

      imageToDecode = originalImage;
    }
  }

  [imageToDecode retain];
  self.picker = nil;
  [self.decoder decodeImage:imageToDecode cropRect:cropRect];
  [imageToDecode release];
}


- (void)imagePickerControllerDidCancel:(UIImagePickerController *)aPicker {
  self.picker = nil;
}

- (void)setPicker:(UIImagePickerController *)aPicker {
  if (picker != aPicker) {
    [picker dismissModalViewControllerAnimated:YES];
    picker = [aPicker retain];
    [self updateToolbar];
  }
}

- (void)navigationController:(UINavigationController *)navigationController
       didShowViewController:(UIViewController *)viewController
                    animated:(BOOL)animated {
  // no-op
}

- (void)navigationController:(UINavigationController *)navigationController
      willShowViewController:(UIViewController *)viewController
                    animated:(BOOL)animated {
  // no-op
}

- (void)performAction:(ResultAction *)action {
  [action performActionWithController:self shouldConfirm:NO];
}

- (void)confirmAndPerformAction:(ResultAction *)action {
  [action performActionWithController:self shouldConfirm:YES];
}


- (IBAction)performResultAction:(id)sender {
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

    [actionSheet showFromToolbar:self.toolbar];
  }
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
  if (buttonIndex < self.actions.count) {
    int actionIndex = buttonIndex;
    ResultAction *action = [self.actions objectAtIndex:actionIndex];
    [self performSelector:@selector(performAction:)
                 withObject:action
                 afterDelay:0.0];
  }
}

- (IBAction)showArchive:(id)sender {
  ArchiveController *archiveController = [[ArchiveController alloc] initWithDecoderViewController:self];
  [[self navigationController] pushViewController:archiveController animated:true];
  [archiveController release];
}

- (void)showScan:(Scan *)scan {
  [self clearImageView];
  [self presentResultForString:scan.text];
  [[self navigationController] popToViewController:self animated:YES];
}

@end
