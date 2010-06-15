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
#include "Decoder.h"
#include "ParsedResult.h"
#include "OverlayView.h"

@protocol ZXingDelegate;

//@interface ZXingWidgetController : UIImagePickerController <DecoderDelegate, CancelDelegate> {
@interface ZXingWidgetController : UIViewController<DecoderDelegate, CancelDelegate, UIImagePickerControllerDelegate,UINavigationControllerDelegate> {
  NSSet *readers;
  ParsedResult *result;
  UIImagePickerController *imagePicker;
  NSArray *actions;       
  OverlayView *overlayView;
  SystemSoundID beepSound;
  BOOL showCancel;
  NSURL *soundToPlay;
  id<ZXingDelegate> delegate;
  BOOL wasCancelled;
  BOOL oneDMode;
}

@property (nonatomic, retain ) NSSet *readers;
@property (nonatomic, assign) id<ZXingDelegate> delegate;
@property (nonatomic, assign) NSURL *soundToPlay;
@property (nonatomic, retain) ParsedResult *result;
@property (nonatomic, retain) NSArray *actions;
@property (nonatomic, retain) OverlayView *overlayView;

- (id)initWithDelegate:(id<ZXingDelegate>)delegate showCancel:(BOOL)shouldShowCancel OneDMode:(BOOL)shouldUseoOneDMode;

- (BOOL)fixedFocus;
- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker;
@end

@protocol ZXingDelegate
- (void)scanResult:(NSString *)result;
- (void)cancelled;
@end