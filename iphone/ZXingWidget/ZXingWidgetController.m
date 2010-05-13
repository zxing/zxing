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

CGImageRef UIGetScreenImage();

@implementation ZXingWidgetController
@synthesize result, actions, showCancel, delegate, soundToPlay;

- (id)initWithDelegate:(id<ZXingDelegate>)scanDelegate {
	if (self = [super init]) {
		[self setDelegate:scanDelegate];
		showCancel = true;
		beepSound = -1;
		self.wantsFullScreenLayout = YES;
		self.sourceType = UIImagePickerControllerSourceTypeCamera;
		float zoomFactor = CAMERA_SCALAR;
		if ([self fixedFocus]) {
			zoomFactor *= 1.5;
		}
		self.cameraViewTransform = CGAffineTransformScale(
					self.cameraViewTransform, zoomFactor, zoomFactor);
		overlayView = [[OverlayView alloc] initWithCancelEnabled:showCancel frame:[UIScreen mainScreen].bounds];
		[overlayView setDelegate:self];
		self.sourceType = UIImagePickerControllerSourceTypeCamera;
		self.showsCameraControls = NO;
		self.cameraOverlayView = overlayView;
		self.allowsEditing = NO; // [[NSUserDefaults standardUserDefaults] boolForKey:@"allowEditing"];
	}
	
	return self;
}

- (void)dealloc {
	if (beepSound != -1) {
		AudioServicesDisposeSystemSoundID(beepSound);
	}
	[overlayView dealloc];
	[super dealloc];
}

- (void)cancelled {
	NSLog(@"cancelled called in ZXingWidgetController");
	wasCancelled = true;
	if (delegate != nil) {
		[delegate cancelled];
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
		[platform isEqualToString:@"iPhone1,2"]) return true;
	return false;
}

- (void)viewWillAppear:(BOOL)animated {
	[super viewWillAppear:animated];
	NSLog(@"should load sound");
	if ([self soundToPlay] != nil) {
		NSLog(@"will try to load sound");
		OSStatus error = AudioServicesCreateSystemSoundID((CFURLRef)[self soundToPlay], &beepSound);
		if (error != kAudioServicesNoError) {
			NSLog(@"Problem loading nearSound.caf");
		}
	}
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
	[overlayView setPoints:nil];
	wasCancelled = false;
	[NSTimer scheduledTimerWithTimeInterval: FIRST_TAKE_DELAY
									 target: self
								   selector: @selector(takePicture:)
								   userInfo: nil
									repeats: NO];
}

- (void)takePicture:(NSTimer*)theTimer {
	CGImageRef capture = UIGetScreenImage();
	UIImage *scrn = [UIImage imageWithCGImage:CGImageCreateWithImageInRect(capture, [overlayView cropRect])];
	Decoder *d = [[Decoder alloc] init];
	d.delegate = self;
	CGRect cropRect = overlayView.cropRect;
	cropRect.origin.x = 0.0;
	cropRect.origin.y = 0.0;
	[d decodeImage:scrn cropRect:cropRect];
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
		NSLog(@"about to play beep... trying...");
		AudioServicesPlaySystemSound(beepSound);
	}
#ifdef DEBUG
	NSLog(@"result string = %@", resultString);
	NSLog(@"result has %d actions", actions ? 0 : actions.count);
#endif
	//	[self updateToolbar];
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
	if (delegate != nil) {
		[delegate scanResult:text];
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
