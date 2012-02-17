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

#include <UIKit/UIKit.h>
#include <AudioToolbox/AudioToolbox.h>
#import <AVFoundation/AVFoundation.h>
#include "Decoder.h"
#include "parsedResults/ParsedResult.h"
#include "OverlayView.h"

@protocol ZXingDelegate;

#if !TARGET_IPHONE_SIMULATOR
#define HAS_AVFF 1
#endif

@interface ZXingWidgetController : UIViewController<DecoderDelegate,
                                                    CancelDelegate,
                                                    UINavigationControllerDelegate
#if HAS_AVFF
                                                    , AVCaptureVideoDataOutputSampleBufferDelegate
#endif
                                                    > {
  NSSet *readers;
  ParsedResult *result;
  OverlayView *overlayView;
  SystemSoundID beepSound;
  BOOL showCancel;
  NSURL *soundToPlay;
  id<ZXingDelegate> delegate;
  BOOL wasCancelled;
  BOOL oneDMode;
#if HAS_AVFF
  AVCaptureSession *captureSession;
  AVCaptureVideoPreviewLayer *prevLayer;
#endif
  BOOL decoding;
  BOOL isStatusBarHidden;
}

#if HAS_AVFF
@property (nonatomic, retain) AVCaptureSession *captureSession;
@property (nonatomic, retain) AVCaptureVideoPreviewLayer *prevLayer;
#endif
@property (nonatomic, retain ) NSSet *readers;
@property (nonatomic, assign) id<ZXingDelegate> delegate;
@property (nonatomic, retain) NSURL *soundToPlay;
@property (nonatomic, retain) ParsedResult *result;
@property (nonatomic, retain) OverlayView *overlayView;

- (id)initWithDelegate:(id<ZXingDelegate>)delegate showCancel:(BOOL)shouldShowCancel OneDMode:(BOOL)shouldUseoOneDMode;
- (id)initWithDelegate:(id<ZXingDelegate>)scanDelegate showCancel:(BOOL)shouldShowCancel OneDMode:(BOOL)shouldUseoOneDMode showLicense:(BOOL)shouldShowLicense;

- (BOOL)fixedFocus;
- (void)setTorch:(BOOL)status;
- (BOOL)torchIsOn;

@end

@protocol ZXingDelegate
- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result;
- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller;
@end
