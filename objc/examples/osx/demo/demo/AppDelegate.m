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


#import "AppDelegate.h"
#import <ZXing/ZXResult.h>

@implementation AppDelegate

@synthesize mainWindow;
@synthesize mainView;
@synthesize binaryView;
@synthesize luminanceView;
@synthesize previewBox;
@synthesize binaryBox;
@synthesize luminanceBox;

@synthesize previewView;
@synthesize userdefaults;
@synthesize zxingEngine;
@synthesize captureLayer;
//@synthesize resultsLayer;
@synthesize captureDevice;
@synthesize mirrorVideoMode;
@synthesize resultsText;
@synthesize captureButton;

@synthesize sourceSelectPopupMenu;
@synthesize mirrorVideoCheckbox;
@synthesize soundsCheckbox;
@synthesize appLogoButton;
@synthesize currentImageBuffer;
@synthesize allVideoDevices;
@synthesize currentVideoSourceName;
@synthesize resultsSound;


// ----------------------------------------------------------------------------------------

- (void) awakeFromNib
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::awakeFromNib - ENTER");
#endif
	
  allVideoDevices		= [[NSMutableArray alloc] init];	
  [self setUserdefaults:[NSUserDefaults standardUserDefaults]];
}

// ----------------------------------------------------------------------------------------

- (void)applicationDidFinishLaunching:(NSNotification*) aNotification
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::applicationDidFinishLaunching - ENTER");
#endif
	
  BOOL forcePrefsReset = NO;
	
  [self setupPreferences:forcePrefsReset];
  [self setupSound];
	
  zxingEngine = [self createZXcapture];
	
  NSNotificationCenter* nc = [NSNotificationCenter defaultCenter];
  if(nil != nc)
  {
    [nc addObserver: self  
           selector: @selector(windowWillClose:)
               name: @"NSWindowWillCloseNotification" 
             object: nil];
  }
	
	
  // NSRect NSRectFromCGRect(CGRect cgrect);	
  // CGRect NSRectToCGRect(NSrect nsrect);
	
  if(nil != zxingEngine)
  {		
    CALayer* layertemp = nil; // this is used for debugging
    NSRect	 nsRect;
    CGRect	 cgRect;
			
    zxingEngine.binary = YES;
    zxingEngine.luminance = YES;
			
    // create a layer where the raw video will appear
    captureLayer = [zxingEngine layer];		
		
    if(nil != captureLayer)
    {
      // CALayer CGRect for capturePayer is going to be all ZEROES
      nsRect   = [[previewBox contentView] frame];
      cgRect	 = [self shrinkContentRect:nsRect];
			
      [captureLayer	setFrame:cgRect];
      [captureLayer	setBackgroundColor:kBACKGROUNDCOLOR];	
      [previewView	setLayer:captureLayer];
      [previewView	setWantsLayer:YES]; 
		
      layertemp = zxingEngine.luminance;
      if((nil != layertemp) && (nil != luminanceView))
      {
        nsRect  = [[luminanceBox contentView] frame];
        cgRect	= [self shrinkContentRect:nsRect];
        [layertemp	setFrame:cgRect];
        [luminanceView	setLayer:layertemp];
      }
				
      layertemp = zxingEngine.binary;
      if((nil != layertemp) & (nil != binaryView))
      {
        nsRect   = [[binaryBox contentView] frame];
        cgRect	= [self shrinkContentRect:nsRect];
        [layertemp	setFrame:cgRect];
        [binaryView setLayer:layertemp];
      }
				
      [self performVideoSourceScan];

      [captureButton			setTitle:kCANCELTITLE];
			
      if((nil == captureDevice) && (nil != allVideoDevices) && (0 < [allVideoDevices count]))
      {
        [self setCaptureDevice:(QTCaptureDevice*)[allVideoDevices objectAtIndex:0]];
      }
			
      
      zxingEngine.captureDevice = captureDevice;
      zxingEngine.delegate = self;
      zxingEngine.mirror = mirrorVideoMode;
      [zxingEngine start];
    }
  }
}
	
// -------------------------------------------------------------------------------------

- (void) applicationWillTerminate:(NSNotification *)notification
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::applicationWillTerminate - ENTER");
#endif
	
  // anything needed to be done on the way out? 
}
	
// -------------------------------------------------------------------------------------

- (IBAction) windowWillClose:(NSNotification *)notification
{	
  static Boolean beenhere = FALSE;
	
  NSWindow* theWindow = (NSWindow*)[notification object];
	
  if(((mainWindow == theWindow) || (nil == notification)) && (beenhere == FALSE))
  {
#ifdef __DEBUG_LOGGING__
    NSLog(@"AppDelegate::windowWillClose - ENTER");
#endif
		
    beenhere = TRUE;
    [self exitTheApp];		// call this from here? 
  }
}

// -------------------------------------------------------------------------------------

- (void) dealloc
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::dealloc - ENTER");
#endif
	
  [allVideoDevices	removeAllObjects];
  [allVideoDevices	release];

  [[NSNotificationCenter defaultCenter] removeObserver:self];
  [super dealloc];
}

// ------------------------------------------------------------------------------------

- (void) exitTheApp
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::exitTheApp - ENTER");
#endif
	
  static Boolean beenhere = FALSE;
	
  if(FALSE == beenhere)
  {
    // shutdown all of the open windows, resources, streams, etc. 
		
    beenhere = TRUE;
    [NSApp terminate:nil];
  }
}

// ------------------------------------------------------------------------------------------
// Find all video input devices for DISPLAY IN POPUP MENU - This does not initialize anything

- (void) performVideoSourceScan		
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"ZXSourceSelect::performVideoSourceScan - ENTER");
#endif
	
  NSUInteger		dex				= 0;
  OSErr			err				= noErr;
  NSString*		theitemtitle	= nil;
  NSMenuItem*		themenuitem		= nil;
  NSArray*		theQTarray		= nil;
	
  if(nil != sourceSelectPopupMenu)
  {
    NSUInteger count;
    [sourceSelectPopupMenu	removeAllItems];		// wipe popup menu clean
		
    [sourceSelectPopupMenu  addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];	// always the top item
		
    [allVideoDevices		removeAllObjects];		// wipe array of video devices clean
		
    // acquire unmuxed devices
    theQTarray = [NSArray arrayWithArray:[QTCaptureDevice inputDevicesWithMediaType:QTMediaTypeVideo]];
    if((nil != theQTarray) && (0 < [theQTarray count]))
      [allVideoDevices addObjectsFromArray:theQTarray];
		
    // acquire muxed devices
    theQTarray = [NSArray arrayWithArray:[QTCaptureDevice inputDevicesWithMediaType:QTMediaTypeMuxed]];
    if((nil != theQTarray) && (0 < [theQTarray count]))
      [allVideoDevices addObjectsFromArray:theQTarray];
		
    // did anything show up?
    count = [allVideoDevices count];
    if(0 < count)
    {
      for(dex = 0; dex < count; dex++)
      {
        QTCaptureDevice* aVideoDevice = (QTCaptureDevice*)[allVideoDevices objectAtIndex:dex];
        if(nil != aVideoDevice)
        {
          theitemtitle  = [aVideoDevice localizedDisplayName];
          [sourceSelectPopupMenu addItemWithTitle:theitemtitle]; // NSPopUpButton
					
          themenuitem = [sourceSelectPopupMenu itemWithTitle:theitemtitle];
          [themenuitem setTag:(dex+kVIDEOSOURCEOFFSET)];
        }
      }
    }
    else	// reset the menu
    {
      err = fnfErr;
      [sourceSelectPopupMenu removeAllItems];
      [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];
      [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
      [self setCurrentVideoSourceName:kBLANKSTR];
      [userdefaults setObject:(id)kBLANKSTR forKey:kVIDEOSOURCETITLE];
    }
		
    if(noErr == err)
    {
      [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
      [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONDISCONNECT];	// Disconnect video Source
    }
  }
}

// ------------------------------------------------------------------------------------------
// called when user clicks in or selects an item in the popup menu

- (IBAction) selectedVideoSourceChange:(id)sender
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"ZXSourceSelect::selectedVideoSourceChange - ENTER");
#endif
	
  [self performSelectorOnMainThread:@selector(configureForVideoSource:) withObject:sender waitUntilDone:NO];
}
	
// ------------------------------------------------------------------------------------------

- (IBAction) configureForVideoSource:(id) sender
{
  NSPopUpButton* videoselections = (id) sender;

  if(nil != videoselections)
  {
    NSInteger numberOfItems = [videoselections numberOfItems];
    if(1 < numberOfItems)
    {
      NSMenuItem* selectedItem = [videoselections selectedItem];
      if(nil != selectedItem)
      {
        NSInteger tag = [selectedItem tag];
				
        // See if it's a source or command that the user has selected
				
        if(kVIDEOSOURCEOFFSET > tag)	// in this case, the user wants to rescan or disable
        {
          NSString* selectedItemTitle = [selectedItem title];
          if(nil != selectedItemTitle)
          {
            BOOL isDisableVideoRequest = (NSOrderedSame == [selectedItemTitle compare:kIMAGESOURCESELECTIONDISCONNECT]);
            if(YES == isDisableVideoRequest)
            {
              [sourceSelectPopupMenu removeAllItems];
              [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONPOPUPTITLE];
              [sourceSelectPopupMenu addItemWithTitle:kIMAGESOURCESELECTIONRESCANTITLE];
            }
            else
            {
              BOOL isRescanRequest = (NSOrderedSame == [selectedItemTitle compare:kIMAGESOURCESELECTIONRESCANTITLE]);
              if(YES == isRescanRequest)
              {
                [self performVideoSourceScan];
              }
              else
              {
                ; // nothing... 
              }
            }
          }
        }
        else	// changing the selected video to a source that is (or was) known to exist
        {
          NSInteger devicecount = [allVideoDevices count];
          NSInteger tagindex = (tag - kVIDEOSOURCEOFFSET);
					
          if((0 <= tagindex) && (tagindex < devicecount))
          {
            QTCaptureDevice* aCaptureDevice = (QTCaptureDevice*)[allVideoDevices objectAtIndex:tagindex];
						
            if(nil != aCaptureDevice)
            {
              currentVideoSourceName = [aCaptureDevice localizedDisplayName];
              if(nil != currentVideoSourceName)
              {
                if(0 < [currentVideoSourceName length])
                {
                  [userdefaults setObject:kZXING_VIDEOSOURCENAME forKey:currentVideoSourceName];
                  [userdefaults synchronize];
									
                  [zxingEngine stop];
                  zxingEngine.captureDevice = aCaptureDevice;
                  zxingEngine.delegate = self;
                  zxingEngine.mirror = mirrorVideoMode;
                  [zxingEngine start];
                }
              }
              [self setCaptureDevice:aCaptureDevice]; // releases existing and retains the new device
            }
          }
          else
          {
#ifdef __DEBUG_LOGGING__
            NSLog(@"ERROR ZXSourceSelect::presentSourceSelectSheet - tag [%d] for [%@] outside bounds of allVideoDevices count [%d]",
              (int)tag, [selectedItem title], (int)devicecount);
#endif
          }
        }
      }
    }
  }
}
	
// ------------------------------------------------------------------------------------------

- (IBAction) captureButtonPressed:(id) sender
{
#pragma unused(sender)
	
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::captureButtonPressed - ENTER");
#endif
	
  if(!zxingEngine.running)
  {
  // Remove the RESULTS layer if it's there... 
  //if(nil != resultsLayer)
  //	{
  //	[resultsLayer removeFromSuperlayer];
  //	[resultsLayer release];
  //	resultsLayer = nil;
  //	}
			
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::captureButtonPressed - zxingEngine was not running");
#endif

		
  [captureButton			setTitle:kCANCELTITLE];
  [resultsText			setStringValue:kBLANKSTR];	// NSTextField
		
  zxingEngine.captureDevice = captureDevice ;
  zxingEngine.delegate = self;
  zxingEngine.mirror = mirrorVideoMode;

  [zxingEngine start];
}
  else	// isRunning 
  {			
  [zxingEngine stop];
  [captureButton			setTitle:kCAPTURETITLE];
}
}
	
// ------------------------------------------------------------------------------------------

- (IBAction) mirrorCheckboxPressed:(id) sender
{
#pragma unused(sender)
	
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::mirrorCheckboxPressed - ENTER");
#endif
	
  [self setMirrorVideoMode:[mirrorVideoCheckbox state]];
	
  [userdefaults setBool:[self mirrorVideoMode] forKey:KZXING_MIRROR_VIDEO];
}
	
// ------------------------------------------------------------------------------------------

- (IBAction) soundsCheckboxPressed:(id) sender
{
#pragma unused(sender)
	
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::soundsCheckboxPressed - ENTER");
#endif
		
  [userdefaults setBool:[soundsCheckbox state] forKey:KZXING_SOUND_ON_RESULT];
}

// ------------------------------------------------------------------------------------------

- (IBAction) zxingImagePressed:(id) sender
{
#pragma unused(sender)
	
  BOOL	success = NO;
  NSURL*	zxingweblink = [NSURL URLWithString:kZXINGWEBURL];
	
  success = [[NSWorkspace sharedWorkspace] openURL:zxingweblink];
	
#ifdef __DEBUG_LOGGING__
  if (YES == success)
    NSLog(@"AppDelegate::zxingImagePressed - ZXing website access successful.");
  else
    NSLog(@"AppDelegate::zxingImagePressed - ZXing website access failed.");		
#endif
}
	
// ------------------------------------------------------------------------------------------
// The name of the video source saved into prefs is compared to the names in the popup menu
// if a match can be made, the menu is set so that item is the selected item and the actual
// captureDevice object reference is sent to the ZXCapture object
/*
  - (void) setupVideoSourceMenuForName:(NSString*) inDisplayName
  {
  NSString* displayName = inDisplayName;
	
  if((nil == displayName) && (nil != allVideoDevices))
  {
  NSString* savedName = [userdefaults objectForKey:kZXING_VIDEOSOURCENAME];
  if(nil == savedName)
  {
  NSInteger devicecount = [allVideoDevices count];
  if(0 < devicecount)
  {
  QTCaptureDevice* thedevice = [allVideoDevices objectAtIndex:0];
  if(nil != thedevice)
  {
  NSString* thelocalizedDisplayName = [thedevice localizedDisplayName];
  if(nil != thelocalizedDisplayName)
  {
  displayName = thelocalizedDisplayName;
  [userdefaults setObject:displayName forKey:kZXING_VIDEOSOURCENAME];
  [userdefaults synchronize];
  }
  }
  }
  }
  else
  {
  displayName = savedName;
  }
  }
	
  if(nil != displayName)
  {
  NSInteger	menuitemindex = [sourceSelectPopupMenu indexOfItemWithTitle:displayName]; 

  if(kNO_SUCH_MENU_ITEM != menuitemindex)
  {			
  if(nil != allVideoDevices)
  {
  // there is always a first item = 'Video Source Select' above the actual sources.
  NSInteger adjustedindex = (menuitemindex - 1);	
				
  if((0 <= adjustedindex) && (adjustedindex < [allVideoDevices count]))
  {
  QTCaptureDevice* aCaptureDevice = [allVideoDevices objectAtIndex:adjustedindex];
  if(nil != aCaptureDevice)
  {
  // double check
  NSString* captureDeviceDisplayName = [aCaptureDevice localizedDisplayName];
  if(NSOrderedSame == [displayName compare:captureDeviceDisplayName])
  {
  NSMenuItem*	themenuitem = [sourceSelectPopupMenu itemWithTitle:displayName];
  [sourceSelectPopupMenu selectItemWithTitle:displayName];
							
  [userdefaults setObject:displayName forKey:kZXING_VIDEOSOURCENAME];
  [userdefaults synchronize];
  [self setCurrentVideoSourceName:displayName];
  [self setCaptureDevice:aCaptureDevice];
  if(nil != themenuitem)
  {
  [themenuitem setTag:(menuitemindex+kVIDEOSOURCEOFFSET)];
  }
  }							
  else
  {
  #ifdef __DEBUG_LOGGING__
  NSLog(@"ERROR ZXSourceSelect::setupVideoSourceMenuForName - Can't match [%@] in allVideoDevices", 
  displayName);
  #endif
  }
  }
  }
  else
  {
  #ifdef __DEBUG_LOGGING__
  NSLog(@"ERROR ZXSourceSelect::setupVideoSourceMenuForName - 'adjustedindex' [%d] into allvideo Devices array", 
  (int)adjustedindex);
  #endif					
  }
  }
  }
  }
  }
*/
// ------------------------------------------------------------------------------------------

- (CGRect) shrinkContentRect:(NSRect) inRect
{
  CGRect result;
	
  result.origin.x		= (inRect.origin.x		+ kLEFTVIDEOEASE);
  result.origin.y		= (inRect.origin.y		+ kTOPVIDEOEASE);
  result.size.width	= (inRect.size.width	- kWIDTHVIDEOEASE);
  result.size.height	= (inRect.size.height	- kHEIGHTVIDEOEASE);
	
  return(result);
}

// ------------------------------------------------------------------------------------------

- (void) setupPreferences:(BOOL) forceReset
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::setupPerferences - ENTER - forceReset [%@]", ((YES == forceReset)?@"YES":@"NO"));
#endif
	
  // If there was nothing there, init - else leave previous settings in place
  NSString* zxinglibsupport = (NSString*)[userdefaults objectForKey:kZXING_LIBSUPPORT];
	
  if((nil == zxinglibsupport) || (YES == forceReset))
  {
    [userdefaults setObject:kZXING_LIBSUPPORT forKey:kZXING_LIBSUPPORT];
    [userdefaults setBool:NO				  forKey:KZXING_MIRROR_VIDEO];
    [userdefaults setBool:YES				  forKey:KZXING_SOUND_ON_RESULT];
		
    [userdefaults synchronize];
  }
		
  [self setMirrorVideoMode:(BOOL)[userdefaults boolForKey:KZXING_MIRROR_VIDEO]];
}

- (void) setMirrorVideoMode:(BOOL)mirror {
  mirrorVideoMode = mirror;
  zxingEngine.mirror = mirror;
}


// -----------------------------------------------------------------------------------------

- (ZXCapture*) createZXcapture
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::createZXcapture - ENTER");
#endif
	
  ZXCapture* thecaptureobject = [[ZXCapture alloc] init];
	
  return(thecaptureobject);
}

// -----------------------------------------------------------------------------------------

- (void) setupSound
{
  BOOL playsounds = (BOOL)[userdefaults boolForKey:KZXING_SOUND_ON_RESULT];
	
  [soundsCheckbox setState:playsounds];
	
  NSSound* thesound = [NSSound soundNamed:kCAPTURESUCCESSSOUNDFILENAME];
  if(nil != thesound)
  {
    [self setResultsSound:thesound];
  }
}

// ==========================================================================================
//
//		ZXCaptureDelegate functions:

- (void)captureResult:(ZXCapture*)zxCapture 
               result:(ZXResult*) inResult
{	
#ifdef __DEBUG_LOGGING_CAPTURE__
  NSLog(@"AppDelegate::captureResult - ENTER");
#endif
	
  if(nil != inResult)
  {
    NSString* resultText = [inResult text];
		
    if(nil != resultText)
    {
      [resultsText setStringValue:resultText];
      [self manageOverlay:inResult];
						
      [zxingEngine stop];							// stop and wait for user to want to "Capture" again
			
      [captureButton			setTitle:kCAPTURETITLE];		
			
#ifdef __DEBUG_LOGGING__
      NSLog(@"AppDelegate::captureResult - inResult text[%@]", resultText);
#endif
			
      BOOL playSound = [soundsCheckbox state];
      if(YES == playSound)
      {
        if(nil != resultsSound)
        {
          [resultsSound play];
        }
      }
    }
  }
}
	
// ------------------------------------------------------------------------------------------

- (void) presentOverlayForPoints:(CGPoint)point0
                             pt1:(CGPoint)point1 
                             pt2:(CGPoint)point2 
{
#ifdef __DEBUG_LOGGING__
  NSLog(@"AppDelegate::presentOverlayForPoints - ENTER pt0X[%d] pt0Y[%d] pt1X[%d] pt1Y[%d] pt2X[%d] pt2Y[%d]",
    (int)point0.x, (int)point0.y, (int)point1.x, (int)point1.y, (int)point2.x, (int)point2.y);
#endif
	
	
}
	
// ------------------------------------------------------------------------------------------

- (void) manageOverlay:(ZXResult*) inResult
{
#ifdef __DEBUG_LOGGING_CAPTURE__
  NSLog(@"AppDelegate::manageOverlay - ENTER");
#endif
  /*
    #ifdef __SHOW_OVERLAY_LAYER__
    if(nil != resultsLayer)
    {
    [resultsLayer removeFromSuperlayer];
    [resultsLayer release];
    resultsLayer = nil;
    }
	
    resultsLayer = [[ZXOverlay alloc] init];
	
    // NSRect NSRectFromCGRect(CGRect cgrect);	<- handy reference
    // CGRect NSRectToCGRect(NSrect nsrect);
	
    NSRect resultsRect   = [[previewBox contentView] frame];  // [self shrinkContentRect:
    [resultsLayer setFrame:NSRectToCGRect(resultsRect)];
    [resultsLayer plotPointsOnLayer:inResult]; 
    [captureLayer addSublayer:resultsLayer];	
    #endif
  */
	
}

// ------------------------------------------------------------------------------------------
// This interface doesn't do anything with this...

- (void)captureSize:(ZXCapture*) inCapture
              width:(NSNumber*)  inWidth
             height:(NSNumber*)  inHeight
{
#pragma unused (inCapture, inWidth, inHeight) 
	
#ifdef __DEBUG_LOGGING__
  if((0 != [inWidth intValue]) && (0 != [inHeight intValue]))
  {
    NSLog(@"AppDelegate::captureSize - ENTER - inWidth [%d] inHeight [%d]", 
          [inWidth intValue], [inHeight intValue]);
  }
#endif
}


//		finish: ZXCaptureDelegate functions:
// ==========================================================================================

@end
