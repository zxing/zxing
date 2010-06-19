/**
 * Copyright 2009 Jeff Verkoeyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "ZXingWidgetController.h"
#import "Decoder.h"
#import "NSString+HTML.h"
#import "ResultParser.h"
#import "ParsedResult.h"
#import "ResultAction.h"
#include <sys/types.h>
#include <sys/sysctl.h>

#define CAMERA_SCALAR 1.12412 // scalar = (480 / (2048 / 480))
#define FIRST_TAKE_DELAY 1.0
#define ONE_D_BAND_HEIGHT 10.0

CGImageRef UIGetScreenImage(void);

@interface ZXingWidgetController ()

@property BOOL showCancel;
@property BOOL oneDMode;

@property (nonatomic, retain) UIImagePickerController* imagePicker;

@end





@implementation ZXingWidgetController
@synthesize result, actions, delegate, soundToPlay;
@synthesize overlayView;
@synthesize oneDMode, showCancel;
@synthesize imagePicker;
@synthesize readers;


-(void)loadImagePicker {
  if (self.imagePicker)
  {
    [imagePicker release];
    imagePicker = nil;
  }
  UIImagePickerController* imController = [[UIImagePickerController alloc] init];
  self.imagePicker = imController;
  imagePicker.delegate = self;
  [imController release];
  imagePicker.wantsFullScreenLayout = YES;
  if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera])
    imagePicker.sourceType = UIImagePickerControllerSourceTypeCamera;
  float zoomFactor = CAMERA_SCALAR;
  if ([self fixedFocus]) {
    zoomFactor *= 2.0;
  }
  if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera])
    imagePicker.cameraViewTransform = CGAffineTransformScale(
                                                             imagePicker.cameraViewTransform, zoomFactor, zoomFactor);
  if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera])
  {
    imagePicker.showsCameraControls = NO;
    imagePicker.cameraOverlayView = overlayView;
    imagePicker.allowsEditing = NO;
  }
}

- (void)unloadImagePicker {
  if (self.imagePicker)
  {
    [imagePicker release];
    imagePicker = nil;
  }
}

- (id)initWithDelegate:(id<ZXingDelegate>)scanDelegate showCancel:(BOOL)shouldShowCancel OneDMode:(BOOL)shouldUseoOneDMode {
  if (self = [super init]) {
    [self setDelegate:scanDelegate];
    self.oneDMode = shouldUseoOneDMode;
    self.showCancel = shouldShowCancel;
    self.wantsFullScreenLayout = YES;
    beepSound = -1;
    OverlayView *theOverLayView = [[OverlayView alloc] initWithFrame:[UIScreen mainScreen].bounds 
                                                       cancelEnabled:showCancel 
                                                            oneDMode:oneDMode];
    [theOverLayView setDelegate:self];
    self.overlayView = theOverLayView;
    [theOverLayView release];
  }
  
  return self;
}

- (void)dealloc {
  if (beepSound != -1) {
    AudioServicesDisposeSystemSoundID(beepSound);
  }
  imagePicker.cameraOverlayView = nil;
  [imagePicker release];
  [overlayView release];
  [readers release];
  [super dealloc];
}

- (void)cancelled {
  [[UIApplication sharedApplication] setStatusBarHidden:NO];
  wasCancelled = YES;
  if (delegate != nil) {
    [delegate zxingControllerDidCancel:self];
  }
}

- (NSString *)getPlatform {
  size_t size;
  sysctlbyname("hw.machine", NULL, &size, NULL, 0);
  char *machine = malloc(size);
  sysctlbyname("hw.machine", machine, &size, NULL, 0);
  NSString *platform = [NSString stringWithCString:machine encoding:NSASCIIStringEncoding];
  free(machine);
  return platform;
}

- (BOOL)fixedFocus {
  NSString *platform = [self getPlatform];
  if ([platform isEqualToString:@"iPhone1,1"] ||
      [platform isEqualToString:@"iPhone1,2"]) return YES;
  return NO;
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  self.wantsFullScreenLayout = YES;
  //[[UIApplication sharedApplication] setStatusBarHidden:YES];
  if ([self soundToPlay] != nil) {
    OSStatus error = AudioServicesCreateSystemSoundID((CFURLRef)[self soundToPlay], &beepSound);
    if (error != kAudioServicesNoError) {
      NSLog(@"Problem loading nearSound.caf");
    }
  }
}

- (void)viewDidAppear:(BOOL)animated {
  NSLog(@"View did appear");
  [super viewDidAppear:animated];
  [[UIApplication sharedApplication] setStatusBarHidden:YES];
  //self.wantsFullScreenLayout = YES;
  [self loadImagePicker];
  self.view = imagePicker.view;
  
  [overlayView setPoints:nil];
  wasCancelled = NO;
  if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {

    [NSTimer scheduledTimerWithTimeInterval: FIRST_TAKE_DELAY
                                     target: self
                                   selector: @selector(takePicture:)
                                   userInfo: nil
                                    repeats: NO];
  }
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
  //self.wantsFullScreenLayout = NO;
  [UIApplication sharedApplication].statusBarHidden = NO;
  [self cancelled];
}


- (CGImageRef)CGImageRotated90:(CGImageRef)imgRef
{
  CGFloat angleInRadians = -90 * (M_PI / 180);
  CGFloat width = CGImageGetWidth(imgRef);
  CGFloat height = CGImageGetHeight(imgRef);
  
  CGRect imgRect = CGRectMake(0, 0, width, height);
  CGAffineTransform transform = CGAffineTransformMakeRotation(angleInRadians);
  CGRect rotatedRect = CGRectApplyAffineTransform(imgRect, transform);
  
  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  CGContextRef bmContext = CGBitmapContextCreate(NULL,
                                                 rotatedRect.size.width,
                                                 rotatedRect.size.height,
                                                 8,
                                                 0,
                                                 colorSpace,
                                                 kCGImageAlphaPremultipliedFirst);
  CGContextSetAllowsAntialiasing(bmContext, FALSE);
  CGContextSetInterpolationQuality(bmContext, kCGInterpolationNone);
  CGColorSpaceRelease(colorSpace);
  //      CGContextTranslateCTM(bmContext,
  //                                                +(rotatedRect.size.width/2),
  //                                                +(rotatedRect.size.height/2));
  CGContextScaleCTM(bmContext, rotatedRect.size.width/rotatedRect.size.height, 1.0);
  CGContextTranslateCTM(bmContext, 0.0, rotatedRect.size.height);
  CGContextRotateCTM(bmContext, angleInRadians);
  //      CGContextTranslateCTM(bmContext,
  //                                                -(rotatedRect.size.width/2),
  //                                                -(rotatedRect.size.height/2));
  CGContextDrawImage(bmContext, CGRectMake(0, 0,
                                           rotatedRect.size.width,
                                           rotatedRect.size.height),
                     imgRef);
  
  CGImageRef rotatedImage = CGBitmapContextCreateImage(bmContext);
  CFRelease(bmContext);
  [(id)rotatedImage autorelease];
  
  return rotatedImage;
}

- (CGImageRef)CGImageRotated180:(CGImageRef)imgRef
{
  CGFloat angleInRadians = M_PI;
  CGFloat width = CGImageGetWidth(imgRef);
  CGFloat height = CGImageGetHeight(imgRef);
  
  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  CGContextRef bmContext = CGBitmapContextCreate(NULL,
                                                 width,
                                                 height,
                                                 8,
                                                 0,
                                                 colorSpace,
                                                 kCGImageAlphaPremultipliedFirst);
  CGContextSetAllowsAntialiasing(bmContext, FALSE);
  CGContextSetInterpolationQuality(bmContext, kCGInterpolationNone);
  CGColorSpaceRelease(colorSpace);
  CGContextTranslateCTM(bmContext,
                        +(width/2),
                        +(height/2));
  CGContextRotateCTM(bmContext, angleInRadians);
  CGContextTranslateCTM(bmContext,
                        -(width/2),
                        -(height/2));
  CGContextDrawImage(bmContext, CGRectMake(0, 0, width, height), imgRef);
  
  CGImageRef rotatedImage = CGBitmapContextCreateImage(bmContext);
  CFRelease(bmContext);
  [(id)rotatedImage autorelease];
  
  return rotatedImage;
}

- (void)takePicture:(NSTimer*)theTimer {
  CGImageRef capture = UIGetScreenImage();
  CGRect cropRect = [overlayView cropRect];
  if (oneDMode) {
    // let's just give the decoder a vertical band right above the red line
    cropRect.origin.x = cropRect.origin.x + (cropRect.size.width / 2) - (ONE_D_BAND_HEIGHT + 1);
    cropRect.size.width = ONE_D_BAND_HEIGHT;
    // do a rotate
    CGImageRef croppedImg = CGImageCreateWithImageInRect(capture, cropRect);
    capture = [self CGImageRotated90:croppedImg];
    capture = [self CGImageRotated180:capture];
    //              UIImageWriteToSavedPhotosAlbum([UIImage imageWithCGImage:capture], nil, nil, nil);
    CGImageRelease(croppedImg);
    cropRect.origin.x = 0.0;
    cropRect.origin.y = 0.0;
    cropRect.size.width = CGImageGetWidth(capture);
    cropRect.size.height = CGImageGetHeight(capture);
  }
  CGImageRef newImage = CGImageCreateWithImageInRect(capture, cropRect);
  CGImageRelease(capture);
  //UIImage *scrn = [UIImage imageWithCGImage:newImage];
  UIImage *scrn = [[UIImage alloc] initWithCGImage:newImage];
  CGImageRelease(newImage);
  Decoder *d = [[Decoder alloc] init];
  d.readers = readers;
  d.delegate = self;
  cropRect.origin.x = 0.0;
  cropRect.origin.y = 0.0;
  [d decodeImage:scrn cropRect:cropRect];
  [scrn release];
}

// DecoderDelegate methods

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset{
#ifdef DEBUG
  NSLog(@"DecoderViewController MessageWhileDecodingWithDimensions: Decoding image (%.0fx%.0f) ...", image.size.width, image.size.height);
#endif
}

- (void)decoder:(Decoder *)decoder
  decodingImage:(UIImage *)image
    usingSubset:(UIImage *)subset
       progress:(NSString *)message {
}

- (void)presentResultForString:(NSString *)resultString {
  self.result = [ResultParser parsedResultForString:resultString];
  
  if (beepSound != -1) {
    AudioServicesPlaySystemSound(beepSound);
  }
#ifdef DEBUG
  NSLog(@"result string = %@", resultString);
  NSLog(@"result has %d actions", actions ? 0 : actions.count);
#endif
  //      [self updateToolbar];
}

- (void)presentResultPoints:(NSArray *)resultPoints
                   forImage:(UIImage *)image
                usingSubset:(UIImage *)subset {
  // simply add the points to the image view
  [overlayView setPoints:resultPoints];
}

- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset withResult:(TwoDDecoderResult *)twoDResult {
  [self presentResultForString:[twoDResult text]];
  [self presentResultPoints:[twoDResult points] forImage:image usingSubset:subset];
  // now, in a selector, call the delegate to give this overlay time to show the points
  [self performSelector:@selector(alertDelegate:) withObject:[[twoDResult text] copy] afterDelay:1.0];
  decoder.delegate = nil;
  [decoder release];
}

- (void)alertDelegate:(id)text {        
  [[UIApplication sharedApplication] setStatusBarHidden:NO];
  if (delegate != nil) {
    [delegate zxingController:self didScanResult:text];
  }
}

- (void)decoder:(Decoder *)decoder failedToDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset reason:(NSString *)reason {
  decoder.delegate = nil;
  [decoder release];
  [overlayView setPoints:nil];
  if (!wasCancelled) {
    [self takePicture:nil];
  }
  //[self updateToolbar];
}

@end
