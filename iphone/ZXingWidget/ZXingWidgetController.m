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
@synthesize result, actions, showCancel, delegate;

- (id)initWithDelegate:(id<ZXingDelegate>)scanDelegate {
	if (self = [super init]) {
		[self setDelegate:scanDelegate];
		showCancel = true;
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

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
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
	NSLog(@"crop rect %f, %f, %f, %f", cropRect.origin.x, cropRect.origin.y, cropRect.size.width, cropRect.size.height);
	[d decodeImage:scrn cropRect:cropRect];
}

// DecoderDelegate methods

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset{
	NSLog(@"DecoderViewController MessageWhileDecodingWithDimensions: Decoding image (%.0fx%.0f) ...", image.size.width, image.size.height);
}

- (void)decoder:(Decoder *)decoder
  decodingImage:(UIImage *)image
    usingSubset:(UIImage *)subset
       progress:(NSString *)message {
	NSLog(@"decoding image %@", message);
}

- (void)presentResultForString:(NSString *)resultString {
	NSLog(@"in presentResultForString()");
	self.result = [ResultParser parsedResultForString:resultString];
	AudioServicesPlaySystemSound(beepSound);
	//	self.actions = self.result.actions;
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
	NSLog(@"got points for display");
	[overlayView setPoints:resultPoints];
}

- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset withResult:(TwoDDecoderResult *)twoDResult {
	//	[self presentResultForString:twoDResult.text];
	NSLog(@"decoded image!!");
	[self presentResultPoints:[twoDResult points] forImage:image usingSubset:subset];
	if (delegate != nil) {
		[delegate scanResult:[twoDResult text]];
	}
	decoder.delegate = nil;
	[decoder release];
	
	// save the scan to the shared database
	//	[[Database sharedDatabase] addScanWithText:twoDResult.text];
	// need to call delegate....`
	//	[self performResultAction:self];
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
