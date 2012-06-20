// -*- mode:objc; c-basic-offset:2; indent-tabs-mode:nil -*-
/*
 * Copyright 2011-2012 ZXing authors
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

#import <Cocoa/Cocoa.h>

#import "Defs.h"
#import <ZXing/ZXCapture.h>
#import <ZXing/ZXCaptureDelegate.h>
//#import <ZXingObjc/ZXOverlay.h>

// --------------------------------------------------------------------

@interface AppDelegate : NSObject <NSApplicationDelegate, ZXCaptureDelegate>
{
@private
	
  IBOutlet	NSWindow*				mainWindow;
  IBOutlet	NSView*					mainView;
  IBOutlet	NSView*					previewView;
  IBOutlet	NSView*					binaryView;
  IBOutlet	NSView*					luminanceView;
  IBOutlet	NSTextField*			resultsText;
  IBOutlet	NSPopUpButton*			sourceSelectPopupMenu;
  IBOutlet	NSButton*				mirrorVideoCheckbox;
  IBOutlet	NSButton*				soundsCheckbox;
  IBOutlet	NSButton*				appLogoButton;
  IBOutlet	NSButton*				captureButton;
  IBOutlet	NSBox*					previewBox;
  IBOutlet	NSBox*					binaryBox;
  IBOutlet	NSBox*					luminanceBox;

  ZXCapture*				zxingEngine;
  CALayer*				captureLayer;
  //ZXOverlay*				resultsLayer;
  QTCaptureDevice*                      captureDevice;
  NSUserDefaults*			userdefaults;
  NSMutableArray*			allVideoDevices;
  CVImageBufferRef                      currentImageBuffer;
  NSString*				currentVideoSourceName;
  BOOL					mirrorVideoMode;
  NSSound*				resultsSound;
}
	
// --------------------------------------------------------------------

@property (nonatomic, retain) IBOutlet	NSWindow*			mainWindow;
@property (nonatomic, retain) IBOutlet	NSTextField*		resultsText;

@property (nonatomic, retain) IBOutlet	NSView*				mainView;

@property (nonatomic, retain) IBOutlet	NSView*				binaryView;
@property (nonatomic, retain) IBOutlet	NSView*				luminanceView;
@property (nonatomic, retain) IBOutlet	NSView*				previewView;

@property (nonatomic, retain) IBOutlet	NSBox*				previewBox;
@property (nonatomic, retain) IBOutlet	NSBox*				binaryBox;
@property (nonatomic, retain) IBOutlet	NSBox*				luminanceBox;


@property (nonatomic, retain) IBOutlet	NSPopUpButton*		sourceSelectPopupMenu;
@property (nonatomic, retain) IBOutlet	NSButton*			mirrorVideoCheckbox;
@property (nonatomic, retain) IBOutlet	NSButton*			soundsCheckbox;
@property (nonatomic, retain) IBOutlet	NSButton*			appLogoButton;
@property (nonatomic, retain) IBOutlet	NSButton*			captureButton;

@property (nonatomic, retain)			NSUserDefaults*		userdefaults;
@property (nonatomic, retain)			ZXCapture*			zxingEngine;
//@property (nonatomic, retain)			ZXOverlay*			resultsLayer;

@property (nonatomic, retain)			CALayer*			captureLayer;
@property (nonatomic, retain)			QTCaptureDevice*	captureDevice;

@property (nonatomic, assign)			CVImageBufferRef	currentImageBuffer;
@property (nonatomic, retain)			NSArray*			allVideoDevices;
@property (nonatomic, retain)			NSString*			currentVideoSourceName;
@property (nonatomic, assign)			BOOL				mirrorVideoMode;
@property (nonatomic, retain)			NSSound*			resultsSound;

// --------------------------------------------------------------------

- (IBAction)	windowWillClose:(NSNotification *)notification;
- (IBAction)	selectedVideoSourceChange:(id) sender;
- (IBAction)	captureButtonPressed:(id) sender;
- (IBAction)	mirrorCheckboxPressed:(id) sender;
- (IBAction)	soundsCheckboxPressed:(id) sender;
- (IBAction)	zxingImagePressed:(id) sender;
- (IBAction)	configureForVideoSource:(id) sender;
- (void)		awakeFromNib;

- (void)		applicationDidFinishLaunching:(NSNotification *)notification;
- (void)		applicationWillTerminate:(NSNotification *)notification;
- (void)		setupPreferences:(BOOL) forceReset;
- (void)		setupSound;

- (ZXCapture*)	createZXcapture;

- (void)		performVideoSourceScan;

- (CGRect)		shrinkContentRect:(NSRect) inRect;
- (void)		manageOverlay:(ZXResult*) inResult;
- (void)		presentOverlayForPoints:(CGPoint)point0
                                            pt1:(CGPoint)point1 
                                            pt2:(CGPoint)point2;
									
//- (void)		setupVideoSourceMenuForName:(NSString*) displayName;

- (void)		exitTheApp;

// ---------------------------------------------------------------------
@end
