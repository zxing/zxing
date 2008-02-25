/*
 * Copyright 2008 Google Inc.
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

#import <CoreGraphics/CGGeometry.h>
#import <CoreGraphics/CGImage.h>
#import <Foundation/Foundation.h>
#import <UIKit/UIPushButton.h>
#import <UIKit/UIWindow.h>
#import <PhotoLibrary/CameraController.h>
#import "ZXingApplication.h"

@implementation ZXingApplication

- (void) applicationDidFinishLaunching: (id) param1 {
    mainWindow = [[UIWindow alloc] initWithContentRect: [UIHardware
        fullScreenApplicationContentRect]];

    [mainWindow orderFront: self];
    [mainWindow _setHidden: NO];

    // Setup camera stuff
    cameraController = [[CameraController alloc] init];
    [cameraController startPreview];
    [[CameraController sharedInstance] setDelegate: self];
	
    struct CGRect fullscreen_rect =
        [UIHardware fullScreenApplicationContentRect];
    fullscreen_rect.origin.x = 0.0f;
    fullscreen_rect.origin.y = 0.0f;
    fullscreen_rect.size.width = 320.0f;
    fullscreen_rect.size.height = 480.0f;

    // Setup previewView
    previewView = [[UIView alloc] initWithFrame: fullscreen_rect];
    [previewView addSubview: [cameraController previewView]];

    UIPushButton *decode_button =
        [[UIPushButton alloc] initWithFrame:
                                CGRectMake(140.0f, 440.0f, 50.0f, 20.0f)];
    [decode_button setTitle: @"Decode"];
    [decode_button setEnabled: YES];
    [decode_button addTarget: self action: @selector(decodeImage:)
                   forEvents: 255];

    [previewView addSubview: decode_button];

    // Setup decodeView
    decodeView = [[UIView alloc] initWithFrame: fullscreen_rect];
    decodeImageView = [[UIImageView alloc] initWithFrame:
                                             CGRectMake(0.0f, 0.0f,
                                                        320.0f, 480.0f)];
    [decodeImageView setRotationBy: 90];
    [decodeView addSubview: decodeImageView];
	
    [mainWindow setContentView: previewView];
}

- (void) decodeImage: (id) param1 {
  [cameraController capturePhoto];

  [mainWindow setContentView: decodeView];
}


- (void) cameraControllerReadyStateChanged: (id) param1 {
}

- (void) cameraController: (id) param1
              tookPicture: (id) param2
              withPreview: (id) preview
                 jpegData: (id) param3
          imageProperties: (id) param4 {
  [cameraController stopPreview];

  CGImageRef orig_image = [preview imageRef];
  int orig_height = CGImageGetHeight(orig_image);
  int orig_width = CGImageGetWidth(orig_image);

  [decodeImageView setImage: preview];
  // Height and width swapped since the image comes in rotated 90 degrees.
  [decodeImageView setFrame: CGRectMake(0.0f, 0.0f, orig_height, orig_width)];
}

@end
